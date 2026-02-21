package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MantisLeapState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MantisLeapMovementEvent {
    private MantisLeapMovementEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        var player = event.player;
        if (player.level().isClientSide) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE);
        if (!isLeapActive(state)) {
            deactivate(spellData, player, state, true);
            return;
        }

        if (!player.isAlive() || player.isPassenger()) {
            deactivate(spellData, player, state, true);
            return;
        }

        applyNoGravity(spellData, player, state);

        var nextTick = Math.min(state.totalTicks, state.elapsedTicks + 1);
        var currentProgress = state.elapsedTicks / (double) state.totalTicks;
        var nextProgress = nextTick / (double) state.totalTicks;

        var currentPosition = calculateEasedPosition(state, currentProgress);
        var nextPosition = calculateEasedPosition(state, nextProgress);
        var movement = nextPosition.subtract(currentPosition);

        player.fallDistance = 0;
        player.setDeltaMovement(movement);
        player.hasImpulse = true;
        player.hurtMarked = true;

        if (nextTick >= state.totalTicks) {
            spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> s.elapsedTicks = s.totalTicks);
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> s.elapsedTicks = nextTick);
    }

    private static boolean isLeapActive(MantisLeapState state) {
        return state.totalTicks > 0 && state.elapsedTicks < state.totalTicks;
    }

    private static void applyNoGravity(CodexSpellData spellData, Player player, MantisLeapState state) {
        if (state.noGravityApplied) {
            return;
        }

        player.setNoGravity(true);
        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> s.noGravityApplied = true);
    }

    private static Vec3 calculateEasedPosition(MantisLeapState state, double progress) {
        var clamped = Math.max(0.0, Math.min(1.0, progress));
        var eased = easeOutCubic(clamped);

        var x = Mth.lerp(eased, state.startX, state.targetX);
        var y = Mth.lerp(eased, state.startY, state.targetY);
        var z = Mth.lerp(eased, state.startZ, state.targetZ);
        y += calculateArcOffset(clamped, state.arcHeight);

        return new Vec3(x, y, z);
    }

    private static double easeOutCubic(double value) {
        var inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }

    private static double calculateArcOffset(double progress, double arcHeight) {
        return Math.max(0.0, arcHeight) * 4.0 * progress * (1.0 - progress);
    }

    private static void deactivate(CodexSpellData spellData, Player player, MantisLeapState state, boolean stopMovement) {
        if (state.totalTicks == 0 &&
                state.elapsedTicks == 0 &&
                state.startX == 0.0 && state.startY == 0.0 && state.startZ == 0.0 &&
                state.targetX == 0.0 && state.targetY == 0.0 && state.targetZ == 0.0 &&
                state.arcHeight == 0.0 && !state.noGravityApplied) {
            return;
        }

        if (state.noGravityApplied) {
            player.setNoGravity(false);
        }
        if (stopMovement) {
            player.setDeltaMovement(0.0, 0.0, 0.0);
            player.hasImpulse = true;
            player.hurtMarked = true;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> {
            s.totalTicks = 0;
            s.elapsedTicks = 0;
            s.startX = 0.0;
            s.startY = 0.0;
            s.startZ = 0.0;
            s.targetX = 0.0;
            s.targetY = 0.0;
            s.targetZ = 0.0;
            s.arcHeight = 0.0;
            s.noGravityApplied = false;
        });
    }
}
