package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellchargedGreatswordChargeEvent {
    private static final Map<UUID, ContinuousCastKey> RECORDED_CONTINUOUS_CASTS = new HashMap<>();

    private SpellchargedGreatswordChargeEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SpellchargedGreatsword)) {
            return;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(event.getSpellId());
        var magicData = MagicData.getPlayerMagicData(player);
        var recordResult = shouldRecordCast(player, magicData, spell, event.getSpellLevel(), event.getCastSource());
        if (recordResult == CastRecordResult.IGNORE) {
            return;
        }

        var gameTime = player.level().getGameTime();
        var levelIncreased = recordResult == CastRecordResult.ADD_CHARGE
                && SpellchargedGreatsword.addCharge(
                        stack,
                        gameTime,
                        SpellchargedGreatsword.computeChargeGainTicks(spell, event.getSpellLevel())
                );
        if (recordResult == CastRecordResult.REFRESH_DECAY_DELAY) {
            SpellchargedGreatsword.refreshChargeDecayDelay(stack, gameTime);
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        player.containerMenu.broadcastChanges();
        if (levelIncreased) {
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundRegistry.SPELLCHARGE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        clearStaleContinuousCast(player);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        SpellchargedGreatsword.resetAllChargeState(event.getEntity().getItem());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        RECORDED_CONTINUOUS_CASTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        RECORDED_CONTINUOUS_CASTS.remove(event.getEntity().getUUID());
    }

    private static CastRecordResult shouldRecordCast(
            ServerPlayer player,
            MagicData magicData,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource
    ) {
        if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
            return CastRecordResult.IGNORE;
        }

        if (spell.getCastType() != CastType.CONTINUOUS
                || !matchesActiveContinuousCast(magicData, spell, spellLevel, castSource)) {
            return CastRecordResult.ADD_CHARGE;
        }

        var key = ContinuousCastKey.from(player, magicData);
        var previousKey = RECORDED_CONTINUOUS_CASTS.get(player.getUUID());
        if (key.equals(previousKey)) {
            return CastRecordResult.REFRESH_DECAY_DELAY;
        }

        // Iron's 1.20.1 Forge は CONTINUOUS の効果 tick ごとに SpellOnCastEvent を出すため、同じ詠唱中は初回だけ蓄積する。
        RECORDED_CONTINUOUS_CASTS.put(player.getUUID(), key);
        return CastRecordResult.ADD_CHARGE;
    }

    private static boolean matchesActiveContinuousCast(
            MagicData magicData,
            AbstractSpell spell,
            int spellLevel,
            CastSource castSource
    ) {
        return magicData != null
                && magicData.isCasting()
                && magicData.getCastType() == CastType.CONTINUOUS
                && Objects.equals(magicData.getCastingSpellId(), spell.getSpellId())
                && magicData.getCastingSpellLevel() == spellLevel
                && magicData.getCastSource() == castSource;
    }

    private static void clearStaleContinuousCast(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !magicData.isCasting() || magicData.getCastType() != CastType.CONTINUOUS) {
            RECORDED_CONTINUOUS_CASTS.remove(player.getUUID());
            return;
        }

        var activeKey = ContinuousCastKey.from(player, magicData);
        var previousKey = RECORDED_CONTINUOUS_CASTS.get(player.getUUID());
        if (previousKey != null && !previousKey.equals(activeKey)) {
            RECORDED_CONTINUOUS_CASTS.remove(player.getUUID());
        }
    }

    private record ContinuousCastKey(
            UUID playerId,
            String spellId,
            int spellLevel,
            CastSource castSource,
            String castingEquipmentSlot
    ) {
        static ContinuousCastKey from(ServerPlayer player, MagicData magicData) {
            return new ContinuousCastKey(
                    player.getUUID(),
                    magicData.getCastingSpellId(),
                    magicData.getCastingSpellLevel(),
                    magicData.getCastSource(),
                    magicData.getCastingEquipmentSlot()
            );
        }
    }

    private enum CastRecordResult {
        IGNORE,
        ADD_CHARGE,
        REFRESH_DECAY_DELAY
    }
}
