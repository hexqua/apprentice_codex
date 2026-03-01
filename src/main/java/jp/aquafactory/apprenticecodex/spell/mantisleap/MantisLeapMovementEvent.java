package jp.aquafactory.apprenticecodex.spell.mantisleap;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellData;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MantisLeapState;
import jp.aquafactory.apprenticecodex.spell.mantisleap.MantisLeapBladeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MantisLeapMovementEvent {
    private static final int POST_LEAP_INVULNERABLE_TICKS = 20;
    private static final int STAGNANT_TICK_LIMIT = 4;
    private static final double STAGNANT_PROGRESS_EPSILON_SQ = 0.01;
    private static final double WATCHDOG_MIN_DISTANCE_SQ = 1.0;
    private static final double[] LANDING_Y_OFFSETS = new double[]{0.0, 0.5, 1.0, -0.5, 1.5, -1.0, 2.0};
    private static final double[][] LANDING_XZ_OFFSETS = new double[][]{
            {0.0, 0.0},
            {0.35, 0.0},
            {-0.35, 0.0},
            {0.0, 0.35},
            {0.0, -0.35},
            {0.7, 0.0},
            {-0.7, 0.0},
            {0.0, 0.7},
            {0.0, -0.7}
    };

    private MantisLeapMovementEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        var level = player.level();

        // クライアント側も動作させないと跳躍のズレが大きくなるため、isClientSideで早期リターンをさせない.
        var isClientSide = level.isClientSide;
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE);

        clearExpiredPostLeapInvulnerability(spellData, level, state);

        if (!isLeapActive(state)) {
            if (isLeapCompleted(state)) {
                finalizeLeap(level, player, state, !isClientSide);
                startPostLeapInvulnerability(spellData, level, state);
            }
            deactivate(spellData, player, state, true);
            return;
        }

        if (!player.isAlive() || player.isPassenger()) {
            startPostLeapInvulnerability(spellData, level, state);
            deactivate(spellData, player, state, true);
            return;
        }

        applyNoGravity(spellData, player, state);

        var targetPosition = getTargetPosition(state);
        var currentDistanceSq = player.position().distanceToSqr(targetPosition);
        var stagnantTicks = calculateStagnantTicks(state, currentDistanceSq);
        if (stagnantTicks >= STAGNANT_TICK_LIMIT) {
            finalizeLeap(level, player, state, !isClientSide);
            startPostLeapInvulnerability(spellData, level, state);
            deactivate(spellData, player, state, true);
            return;
        }

        var nextTick = Math.min(state.totalTicks, state.elapsedTicks + 1);
        var nextProgress = nextTick / (double) state.totalTicks;
        var nextPosition = calculateEasedPosition(state, nextProgress);
        var movement = nextPosition.subtract(player.position());

        player.fallDistance = 0;
        player.setDeltaMovement(movement);
        player.hasImpulse = true;
        player.hurtMarked = true;

        if (nextTick >= state.totalTicks) {
            spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> {
                s.elapsedTicks = s.totalTicks;
                s.lastDistanceToTargetSq = currentDistanceSq;
                s.stagnantTicks = stagnantTicks;
            });
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> {
            s.elapsedTicks = nextTick;
            s.lastDistanceToTargetSq = currentDistanceSq;
            s.stagnantTicks = stagnantTicks;
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        var level = player.level();
        if (level.isClientSide) {
            return;
        }

        // VOID/kill 等の環境要因は対象外とし、モブ由来攻撃のみを無効化する.
        if (!(event.getSource().getEntity() instanceof Mob)) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE);
        if (isLeapActive(state) || isPostLeapInvulnerable(level, state)) {
            event.setCanceled(true);
        }
    }

    private static boolean isLeapActive(MantisLeapState state) {
        return state.totalTicks > 0 && state.elapsedTicks < state.totalTicks;
    }

    private static boolean isLeapCompleted(MantisLeapState state) {
        return state.totalTicks > 0 && state.elapsedTicks >= state.totalTicks;
    }

    private static boolean isPostLeapInvulnerable(Level level, MantisLeapState state) {
        return state.postLeapInvulnerableUntilGameTime > level.getGameTime();
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

    private static Vec3 getTargetPosition(MantisLeapState state) {
        return new Vec3(state.targetX, state.targetY, state.targetZ);
    }

    private static int calculateStagnantTicks(MantisLeapState state, double currentDistanceSq) {
        if (currentDistanceSq < WATCHDOG_MIN_DISTANCE_SQ || state.lastDistanceToTargetSq < 0.0) {
            return 0;
        }

        if (currentDistanceSq <= state.lastDistanceToTargetSq - STAGNANT_PROGRESS_EPSILON_SQ) {
            return 0;
        }

        return state.stagnantTicks + 1;
    }

    private static void triggerSlash(Level level, MantisLeapState state) {
        if (state.bladeEntityId < 0) {
            return;
        }

        var entity = level.getEntity(state.bladeEntityId);
        if (entity instanceof MantisLeapBladeEntity blade && !blade.isSlashed()) {
            blade.slash(level);
        }
    }

    private static void finalizeLeap(Level level, Player player, MantisLeapState state, boolean doSlash) {
        var target = getTargetPosition(state);
        var landing = findSafeLandingPosition(level, player, target);
        player.setPos(landing.x, landing.y, landing.z);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.fallDistance = 0;
        if (doSlash) {
            triggerSlash(level, state);
        }
    }

    private static Vec3 findSafeLandingPosition(Level level, Player player, Vec3 target) {
        var current = player.position();
        for (var yOffset : LANDING_Y_OFFSETS) {
            for (var offset : LANDING_XZ_OFFSETS) {
                var candidate = target.add(offset[0], yOffset, offset[1]);
                if (canPlaceAt(level, player, current, candidate)) {
                    return candidate;
                }
            }
        }

        return target;
    }

    private static boolean canPlaceAt(Level level, Player player, Vec3 from, Vec3 candidate) {
        var movedBox = player.getBoundingBox().move(candidate.subtract(from));
        return level.noCollision(player, movedBox);
    }

    private static void startPostLeapInvulnerability(CodexSpellData spellData, Level level, MantisLeapState state) {
        var newUntil = level.getGameTime() + POST_LEAP_INVULNERABLE_TICKS;
        if (state.postLeapInvulnerableUntilGameTime >= newUntil) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> s.postLeapInvulnerableUntilGameTime = newUntil);
    }

    private static void clearExpiredPostLeapInvulnerability(CodexSpellData spellData, Level level, MantisLeapState state) {
        if (state.postLeapInvulnerableUntilGameTime == 0L) {
            return;
        }
        if (state.postLeapInvulnerableUntilGameTime > level.getGameTime()) {
            return;
        }

        spellData.edit(CodexSpellStateTypeRegister.MANTIS_LEAP_STATE, s -> s.postLeapInvulnerableUntilGameTime = 0L);
    }

    private static void deactivate(CodexSpellData spellData, Player player, MantisLeapState state, boolean stopMovement) {
        if (state.totalTicks == 0 &&
                state.elapsedTicks == 0 &&
                state.startX == 0.0 && state.startY == 0.0 && state.startZ == 0.0 &&
                state.targetX == 0.0 && state.targetY == 0.0 && state.targetZ == 0.0 &&
                state.arcHeight == 0.0 && state.bladeEntityId == -1 &&
                state.lastDistanceToTargetSq < 0.0 && state.stagnantTicks == 0 &&
                !state.noGravityApplied) {
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
            s.bladeEntityId = -1;
            s.lastDistanceToTargetSq = -1.0;
            s.stagnantTicks = 0;
            s.noGravityApplied = false;
        });
    }
}

