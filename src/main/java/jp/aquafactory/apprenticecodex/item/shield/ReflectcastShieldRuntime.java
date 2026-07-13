package jp.aquafactory.apprenticecodex.item.shield;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.TriggeredSpellCastHelper;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncReflectcastShieldEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ReflectcastShieldRuntime {
    private static final int CONTINUOUS_CAST_INTERVAL_TICKS = 10;
    private static final Map<UUID, Long> NEXT_SPELL_TRIGGER_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_DURABILITY_CONSUMPTION_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, ContinuousCast> ACTIVE_CONTINUOUS_CASTS = new ConcurrentHashMap<>();

    private ReflectcastShieldRuntime() {
    }

    public static boolean tryTriggerSpell(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        var activeCast = ACTIVE_CONTINUOUS_CASTS.get(player.getUUID());
        if (activeCast != null) {
            return false;
        }

        var now = player.level().getGameTime();
        if (now < NEXT_SPELL_TRIGGER_TICKS.getOrDefault(player.getUUID(), 0L)) {
            return false;
        }

        var spellData = ReflectcastShield.resolveCastSpell(player, stack);
        if (spellData == null || spellData == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY) {
            return false;
        }
        var shield = (ReflectcastShield) stack.getItem();
        var spell = spellData.getSpell();
        if (!shield.canUseConfiguredSpell(stack, spell, spellData.getLevel())) {
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.isCasting()) {
            return false;
        }
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var castSource = ReflectcastShield.resolveCastSource(player, stack);
        var slot = hand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND;
        var triggered = spell.getCastType() == CastType.CONTINUOUS
                ? tryStartContinuousCast(player, stack, hand, spell, spellLevel, castSource, slot, magicData, now)
                : tryStartTriggeredCast(player, stack, hand, spell, spellLevel, castSource, slot, magicData);
        if (triggered) {
            rememberSpellTriggered(player, now);
        }
        return triggered;
    }

    public static void tickContinuousCast(ServerPlayer player, ItemStack stack) {
        var activeCast = ACTIVE_CONTINUOUS_CASTS.get(player.getUUID());
        if (activeCast == null) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !magicData.getSyncedData().isCasting()
                || !activeCast.spell().getSpellId().equals(magicData.getCastingSpellId())) {
            ACTIVE_CONTINUOUS_CASTS.remove(player.getUUID());
            return;
        }

        var elapsedTicks = Math.max(0L, player.level().getGameTime() - activeCast.startedAt());
        syncMagicDataSimulation(magicData, activeCast, stack, elapsedTicks);
        if (elapsedTicks > 0L && elapsedTicks % CONTINUOUS_CAST_INTERVAL_TICKS == 0L
                && !castPulse(player, activeCast, magicData)) {
            stopActiveCast(player, activeCast, magicData, true);
            ACTIVE_CONTINUOUS_CASTS.remove(player.getUUID());
            return;
        }
        activeCast.spell().onServerCastTick(player.level(), activeCast.spellLevel(), player, magicData);
    }

    public static void finishUse(ServerPlayer player) {
        var activeCast = ACTIVE_CONTINUOUS_CASTS.remove(player.getUUID());
        if (activeCast == null) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null) {
            stopActiveCast(player, activeCast, magicData, true);
        }
    }

    public static boolean shouldBypassMagicManager(MagicData magicData) {
        var player = ((MagicDataAccessor) magicData).apprenticecodex$getServerPlayer();
        if (player == null) {
            return false;
        }
        var activeCast = ACTIVE_CONTINUOUS_CASTS.get(player.getUUID());
        return activeCast != null && magicData.getSyncedData().isCasting()
                && activeCast.spell().getSpellId().equals(magicData.getCastingSpellId());
    }

    public static boolean isDurabilityConsumptionSuppressed(ServerPlayer player, long gameTime) {
        return gameTime < NEXT_DURABILITY_CONSUMPTION_TICKS.getOrDefault(player.getUUID(), 0L);
    }

    public static boolean isSpellTriggerSuppressed(ServerPlayer player, long gameTime) {
        return gameTime < NEXT_SPELL_TRIGGER_TICKS.getOrDefault(player.getUUID(), 0L);
    }

    public static void rememberSpellTriggered(ServerPlayer player, long gameTime) {
        NEXT_SPELL_TRIGGER_TICKS.put(
                player.getUUID(), gameTime + ReflectcastShield.SPELL_TRIGGER_SUPPRESSION_TICKS
        );
    }

    public static void rememberDurabilityConsumed(ServerPlayer player, long gameTime) {
        // ItemStack NBT の更新は Iron's Spells に装備変更と判定され、CONTINUOUS を中断する。
        NEXT_DURABILITY_CONSUMPTION_TICKS.put(
                player.getUUID(),
                gameTime + ReflectcastShield.DURABILITY_SUPPRESSION_TICKS + 1L
        );
    }

    public static void clear(ServerPlayer player) {
        finishUse(player);
        NEXT_SPELL_TRIGGER_TICKS.remove(player.getUUID());
        NEXT_DURABILITY_CONSUMPTION_TICKS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSpell().getCastType() != CastType.LONG) {
            return;
        }
        var magicData = MagicData.getPlayerMagicData(player);
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof ReflectcastShield) || !ReflectcastShield.hasSilverRing(castingItem)) {
            return;
        }
        if (magicData == null) {
            return;
        }
        var spellLevel = magicData.getCastingSpellLevel() > 0 ? magicData.getCastingSpellLevel() : 1;
        event.setEffectiveCooldown(resolveLongCastCooldownTicks(
                player, event.getSpell(), spellLevel, event.getEffectiveCooldown()
        ));
    }

    public static int resolveLongCastCooldownTicks(
            ServerPlayer player,
            AbstractSpell spell,
            int spellLevel,
            int currentEffectiveCooldown
    ) {
        return currentEffectiveCooldown + Math.max(0, spell.getEffectiveCastTime(spellLevel, player));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NEXT_SPELL_TRIGGER_TICKS.clear();
        NEXT_DURABILITY_CONSUMPTION_TICKS.clear();
        ACTIVE_CONTINUOUS_CASTS.clear();
    }

    private static boolean tryStartTriggeredCast(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            String slot,
            MagicData magicData
    ) {
        var remainingUseTicks = player.getUseItemRemainingTicks();
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                player.level(),
                player,
                castSource,
                true,
                slot
        );
        if (casted) {
            TriggeredSpellCastHelper.applyLongCastDurationOverride(player, spellLevel, spell, magicData, slot, 0);
            player.startUsingItem(hand);
            ((LivingEntityAccessor) player).apprenticecodex$setUseItemRemaining(remainingUseTicks);
        }
        return casted;
    }

    private static boolean tryStartContinuousCast(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            String slot,
            MagicData magicData,
            long now
    ) {
        if (!canStartCast(player, spell, spellLevel, castSource, magicData)) {
            return false;
        }
        var activeCast = new ContinuousCast(spell, spellLevel, castSource, slot, now);
        magicData.initiateCast(spell, spellLevel, CONTINUOUS_CAST_INTERVAL_TICKS, castSource, slot);
        magicData.setPlayerCastingItem(stack);
        syncMagicDataSimulation(magicData, activeCast, stack, 0L);
        spell.onServerPreCast(player.level(), spellLevel, player, magicData);
        ACTIVE_CONTINUOUS_CASTS.put(player.getUUID(), activeCast);
        if (castPulse(player, activeCast, magicData)) {
            sendEffectStart(player, stack, hand);
            return true;
        }
        stopActiveCast(player, activeCast, magicData, true);
        ACTIVE_CONTINUOUS_CASTS.remove(player.getUUID());
        return false;
    }

    private static void sendEffectStart(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return;
        }
        var imbuedSpell = spellContainer.getSpellAtIndex(0);
        if (imbuedSpell == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY
                || imbuedSpell.getSpell() == null) {
            return;
        }
        Networks.sendToPlayer(player, new SyncReflectcastShieldEffectPacket(
                hand,
                imbuedSpell.getSpell().getSpellId()
        ));
    }

    private static boolean canStartCast(
            ServerPlayer player,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            MagicData magicData
    ) {
        return spell.canBeCastedBy(spellLevel, castSource, magicData, player).isSuccess()
                && spell.checkPreCastConditions(player.level(), spellLevel, player, magicData)
                && !MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(
                player,
                spell.getSpellId(),
                spellLevel,
                spell.getSchoolType(),
                castSource
        ));
    }

    private static boolean castPulse(ServerPlayer player, ContinuousCast activeCast, MagicData magicData) {
        if (!activeCast.spell().canBeCastedBy(
                activeCast.spellLevel(), activeCast.castSource(), magicData, player
        ).isSuccess()) {
            return false;
        }
        activeCast.spell().castSpell(
                player.level(), activeCast.spellLevel(), player, activeCast.castSource(), false
        );
        return true;
    }

    private static void stopActiveCast(
            ServerPlayer player,
            ContinuousCast activeCast,
            MagicData magicData,
            boolean triggerCooldown
    ) {
        if (triggerCooldown) {
            MagicHelper.MAGIC_MANAGER.addCooldown(player, activeCast.spell(), activeCast.castSource());
        }
        activeCast.spell().onServerCastComplete(
                player.level(), activeCast.spellLevel(), player, magicData, true
        );
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
        accessor.apprenticecodex$setCastDuration(CONTINUOUS_CAST_INTERVAL_TICKS);
        accessor.apprenticecodex$setCastDurationRemaining(
                CONTINUOUS_CAST_INTERVAL_TICKS - (int) (elapsedTicks % CONTINUOUS_CAST_INTERVAL_TICKS)
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

    private record ContinuousCast(
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource,
            String slot,
            long startedAt
    ) {
    }
}
