package jp.aquafactory.apprenticecodex.item.shield;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.item.continuouscast.ContinuousCastDurationSimulation;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.utility.SpellCooldownHelper;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public final class BulwarkGreatshieldRuntime {
    private static final int CONTINUOUS_CAST_INTERVAL_TICKS = 10;
    private static final String COOLDOWN_CAST_ERROR_KEY = "ui.irons_spellbooks.cast_error_cooldown";
    private static final Map<UUID, UseState> USE_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_DURABILITY_CONSUMPTION_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_MANA_RECOVERY_TICKS = new ConcurrentHashMap<>();

    private BulwarkGreatshieldRuntime() {
    }

    public static void beginUse(ServerPlayer player) {
        USE_STATES.put(player.getUUID(), new UseState(false, null));
    }

    public static void tryStartContinuousCast(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        var state = USE_STATES.computeIfAbsent(player.getUUID(), ignored -> new UseState(false, null));
        if (state.attempted()) {
            return;
        }
        USE_STATES.put(player.getUUID(), new UseState(true, null));

        var spellData = BulwarkGreatshield.resolveCastSpell(player, stack);
        if (spellData == null || spellData == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                || spellData.getSpell().getCastType() != CastType.CONTINUOUS) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        var spell = spellData.getSpell();
        if (magicData == null || magicData.isCasting()) {
            return;
        }
        var level = spell.getLevelFor(spellData.getLevel(), player);
        var castSource = BulwarkGreatshield.resolveCastSource(player, stack);
        if (!canStartCast(player, spell, level, castSource, magicData)) {
            return;
        }
        var slot = hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        var castDuration = ContinuousCastDurationSimulation.normalizeCastDuration(spell.getCastTime(level));
        var activeCast = new ContinuousCast(
                spell,
                level,
                castSource,
                slot,
                castDuration,
                player.level().getGameTime()
        );

        // 通常の attemptInitiateCast は盾使用を解除して詠唱モーションへ移るため、必要な魔法状態だけ構築する。
        magicData.initiateCast(spell, level, castDuration, castSource, slot);
        magicData.setPlayerCastingItem(stack);
        syncMagicDataSimulation(magicData, activeCast, stack, 0L);
        spell.onServerPreCast(player.level(), level, player, magicData);
        USE_STATES.put(player.getUUID(), new UseState(true, activeCast));
        if (!castPulse(player, activeCast, magicData)) {
            stopActiveCast(player, activeCast, magicData, true, true);
            USE_STATES.put(player.getUUID(), new UseState(true, null));
        }
    }

    public static void tickContinuousCast(ServerPlayer player, ItemStack stack) {
        var state = USE_STATES.get(player.getUUID());
        if (state == null || state.activeCast() == null) {
            return;
        }

        var activeCast = state.activeCast();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !magicData.getSyncedData().isCasting()
                || !activeCast.spell().getSpellId().equals(magicData.getCastingSpellId())) {
            USE_STATES.put(player.getUUID(), new UseState(true, null));
            return;
        }

        var elapsedTicks = Math.max(0L, player.level().getGameTime() - activeCast.startedAt());
        syncMagicDataSimulation(magicData, activeCast, stack, elapsedTicks);
        if (elapsedTicks > 0L && elapsedTicks % CONTINUOUS_CAST_INTERVAL_TICKS == 0L
                && !castPulse(player, activeCast, magicData)) {
            // マナ切れ後も盾使用は維持し、この構えでは魔法だけ再開させない。
            stopActiveCast(player, activeCast, magicData, true, true);
            USE_STATES.put(player.getUUID(), new UseState(true, null));
            return;
        }

        activeCast.spell().onServerCastTick(player.level(), activeCast.spellLevel(), player, magicData);
    }

    public static void finishUse(ServerPlayer player) {
        var state = USE_STATES.remove(player.getUUID());
        if (state != null && state.activeCast() != null) {
            var magicData = MagicData.getPlayerMagicData(player);
            if (magicData != null) {
                stopActiveCast(player, state.activeCast(), magicData, true, false);
            }
        }
    }

    public static boolean shouldBypassMagicManager(MagicData magicData) {
        var player = ((MagicDataAccessor) magicData).apprenticecodex$getServerPlayer();
        if (player == null) {
            return false;
        }
        var state = USE_STATES.get(player.getUUID());
        return state != null && state.activeCast() != null
                && magicData.getSyncedData().isCasting()
                && state.activeCast().spell().getSpellId().equals(magicData.getCastingSpellId());
    }

    public static boolean tryRecoverMana(ServerPlayer player) {
        var now = player.level().getGameTime();
        var next = NEXT_MANA_RECOVERY_TICKS.getOrDefault(player.getUUID(), 0L);
        if (now < next) {
            return false;
        }
        NEXT_MANA_RECOVERY_TICKS.put(player.getUUID(), now + BulwarkGreatshield.MANA_RECOVERY_COOLDOWN_TICKS);
        BulwarkGreatshield.recoverManaAfterBlock(player);
        // MagicData のサーバー値変更だけでは HUD へ即時反映されないため、Iron's Spells の同期パケットを明示送信する。
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(MagicData.getPlayerMagicData(player)));
        return true;
    }

    public static boolean isDurabilityConsumptionSuppressed(ServerPlayer player, long gameTime) {
        return gameTime < NEXT_DURABILITY_CONSUMPTION_TICKS.getOrDefault(player.getUUID(), 0L);
    }

    public static void rememberDurabilityConsumed(ServerPlayer player, long gameTime) {
        // Iron's Spells は耐久値以外の ItemStack NBT 更新を装備変更として扱い、CONTINUOUS を中断する。
        NEXT_DURABILITY_CONSUMPTION_TICKS.put(
                player.getUUID(),
                gameTime + BulwarkGreatshield.DURABILITY_SUPPRESSION_TICKS + 1L
        );
    }

    public static void clear(ServerPlayer player) {
        finishUse(player);
        NEXT_DURABILITY_CONSUMPTION_TICKS.remove(player.getUUID());
        NEXT_MANA_RECOVERY_TICKS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Iron's の通常死亡処理へ詠唱完了とクールダウン付与を任せるため、先に盾専用ランタイムだけを外す。
            discardRuntime(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Clone 後の MagicData に模擬詠唱がコピーされていても、死亡前のランタイムへ再接続しない。
            discardRuntime(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        USE_STATES.clear();
        NEXT_DURABILITY_CONSUMPTION_TICKS.clear();
        NEXT_MANA_RECOVERY_TICKS.clear();
    }

    private static void discardRuntime(ServerPlayer player) {
        USE_STATES.remove(player.getUUID());
        NEXT_DURABILITY_CONSUMPTION_TICKS.remove(player.getUUID());
        NEXT_MANA_RECOVERY_TICKS.remove(player.getUUID());
    }

    private static boolean canStartCast(
            ServerPlayer player,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            MagicData magicData
    ) {
        var castResult = spell.canBeCastedBy(spellLevel, castSource, magicData, player);
        if (castResult.message != null && !isCooldownCastError(castResult.message.getContents())) {
            // 構え直すたびに出るクールダウン警告だけは抑え、マナ不足など解消が必要な失敗理由は通知する。
            player.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }
        return castResult.isSuccess()
                && spell.checkPreCastConditions(player.level(), spellLevel, player, magicData)
                && !MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(
                        player,
                        spell.getSpellId(),
                        spellLevel,
                        spell.getSchoolType(),
                        castSource
                ));
    }

    private static boolean isCooldownCastError(ComponentContents contents) {
        return contents instanceof TranslatableContents translatable
                && COOLDOWN_CAST_ERROR_KEY.equals(translatable.getKey());
    }

    private static boolean castPulse(ServerPlayer player, ContinuousCast activeCast, MagicData magicData) {
        if (!activeCast.spell().canBeCastedBy(
                activeCast.spellLevel(),
                activeCast.castSource(),
                magicData,
                player
        ).isSuccess()) {
            return false;
        }
        activeCast.spell().castSpell(
                player.level(),
                activeCast.spellLevel(),
                player,
                activeCast.castSource(),
                false
        );
        return true;
    }

    private static void stopActiveCast(
            ServerPlayer player,
            ContinuousCast activeCast,
            MagicData magicData,
            boolean triggerCooldown,
            boolean preserveShieldUse
    ) {
        if (triggerCooldown) {
            SpellCooldownHelper.addCooldownRespectingCreativeConfig(
                    player,
                    activeCast.spell(),
                    activeCast.castSource()
            );
        }
        var finishCast = (Runnable) () -> activeCast.spell().onServerCastComplete(
                player.level(), activeCast.spellLevel(), player, magicData, true
        );
        if (preserveShieldUse) {
            ShieldCastUseContext.runPreservingShieldUse(magicData, finishCast);
        } else {
            finishCast.run();
        }
        clearMagicDataSimulation(magicData, activeCast.slot());
    }

    private static void syncMagicDataSimulation(
            MagicData magicData,
            ContinuousCast activeCast,
            ItemStack castingItem,
            long elapsedTicks
    ) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(activeCast.spellLevel());
        accessor.apprenticecodex$setCastDuration(activeCast.castDuration());
        accessor.apprenticecodex$setCastDurationRemaining(
                ContinuousCastDurationSimulation.computeRemaining(activeCast.castDuration(), elapsedTicks)
        );
        accessor.apprenticecodex$setCastSource(activeCast.castSource());
        accessor.apprenticecodex$setCastType(CastType.CONTINUOUS);
        magicData.setPlayerCastingItem(castingItem);
    }

    private static void clearMagicDataSimulation(MagicData magicData, String slot) {
        magicData.getSyncedData().setIsCasting(false, "", 0, slot);
        magicData.resetAdditionalCastData();
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(0);
        accessor.apprenticecodex$setCastDuration(0);
        accessor.apprenticecodex$setCastDurationRemaining(0);
        accessor.apprenticecodex$setCastSource(CastSource.NONE);
        accessor.apprenticecodex$setCastType(CastType.NONE);
        magicData.setPlayerCastingItem(ItemStack.EMPTY);
    }

    private record UseState(boolean attempted, ContinuousCast activeCast) {
    }

    private record ContinuousCast(
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            String slot,
            int castDuration,
            long startedAt
    ) {
    }
}
