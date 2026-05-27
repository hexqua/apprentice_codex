package jp.aquafactory.apprenticecodex.spell.spectralwing;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SpectralWingState;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpectralWingFlightEvent {
    private static final int WATER_DEACTIVATE_GRACE_TICKS = 4;

    private SpectralWingFlightEvent() {
    }

    public static void onSpectralWingEffectRemoved(LivingEntity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide) {
            return;
        }

        clearWingState(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        var player = event.player;
        var level = player.level();
        if (level.isClientSide) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE);
        if (!state.active) {
            clearLingeringVisual(player);
            return;
        }

        if (state.launchGraceTicks > 0) {
            spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, s -> --s.launchGraceTicks);
        }
        updateWaterGrace(player, spellData, state);
        if (shouldDeactivate(player, state)) {
            deactivate(player, spellData, state);
            return;
        }

        player.fallDistance = 0.0f;
        SpectralWing.refreshVisualEffect(player);

        if (!player.isFallFlying() && canStartFallFlying(player)) {
            // 1.20.1 の tryToStartFallFlying は Elytra 装備を要求するため、
            // この魔法中だけ直接 fall flying フラグを立てる。
            player.startFallFlying();
            player.hasImpulse = true;
            player.hurtMarked = true;
        }
    }

    private static boolean shouldDeactivate(Player player, SpectralWingState state) {
        if (!state.startedBySpell) {
            return true;
        }

        if (state.waterGraceTicks > WATER_DEACTIVATE_GRACE_TICKS) {
            return true;
        }

        if (player.onGround() && state.launchGraceTicks <= 0) {
            return true;
        }

        return player.getAbilities().flying || player.isPassenger() || player.onClimbable();
    }

    private static boolean canStartFallFlying(Player player) {
        return !player.onGround()
                && !player.isInWaterOrBubble()
                && !player.getAbilities().flying
                && !player.isPassenger()
                && !player.onClimbable()
                && !player.isSwimming();
    }

    private static void updateWaterGrace(Player player, CodexSpellData spellData, SpectralWingState state) {
        boolean touchingWater = player.isInWaterOrBubble() || player.isSwimming();
        if (touchingWater) {
            spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, s -> ++s.waterGraceTicks);
        } else if (state.waterGraceTicks > 0) {
            spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, s -> s.waterGraceTicks = 0);
        }
    }

    private static void deactivate(Player player, CodexSpellData spellData, SpectralWingState state) {
        stopFallFlying(player);

        player.removeEffect(EffectRegistry.SPECTRAL_WING.get());
        if (!state.active && !state.startedBySpell && state.launchGraceTicks == 0) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, SpectralWingState::reset);
    }

    private static void clearLingeringVisual(Player player) {
        if (player.hasEffect(EffectRegistry.SPECTRAL_WING.get())) {
            player.removeEffect(EffectRegistry.SPECTRAL_WING.get());
        }
    }

    private static void clearWingState(Player player) {
        stopFallFlying(player);
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData != null) {
            spellData.edit(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE, SpectralWingState::reset);
        }
    }

    private static void stopFallFlying(Player player) {
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
    }
}
