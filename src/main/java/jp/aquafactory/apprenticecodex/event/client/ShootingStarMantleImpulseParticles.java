package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleCalibration;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ShootingStarMantleImpulseParticles {
    private static final int MAX_ICE_COAST_TICKS = 8;
    private static final AdditiveGlowParticleOptions COMET_SPARK =
            new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.16F,
                    0.38F, 0.58F, 1.0F, 3, 11, 3,
                    0.85F, 1.25F, 0.75F, 0.95F, 0.03F, 0.65F, 0.65F, true);
    private static final AdditiveGlowParticleOptions ICE_SPARK =
            new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.12F,
                    0.48F, 0.88F, 1.0F, 2, 9, 3,
                    0.8F, 1.2F, 0.55F, 0.8F, 0.03F, 0.6F, 0.55F, true);
    private static final AdditiveGlowParticleOptions STAR =
            new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_RHOMBUS.get(), 0.15F,
                    1.0F, 0.87F, 0.36F, 2, 14, 3,
                    0.85F, 1.2F, 0.65F, 0.85F, 0.04F, 0.68F, 0.6F, false);
    private static Vec3 previousPosition;
    private static boolean wasDashing;
    private static int dashAge;
    private static int iceCoastTicks;

    private ShootingStarMantleImpulseParticles() { }

    static boolean tick(LocalPlayer player) {
        if (player == null || !ShootingStarMantleRuntime.isHovering(player)) {
            reset();
            return false;
        }
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        // Blinkや属性ダッシュには専用の演出があるため、通常の高速移動だけを対象にする。
        if (MantleCalibration.usesBlink(stack) || MantleCalibration.elementalKind(stack) != 0) {
            reset();
            return false;
        }
        boolean dashing = ShootingStarMantleRuntime.state(player).dashTicks > 0;
        if (dashing) {
            if (!wasDashing) {
                previousPosition = null;
                dashAge = 0;
                iceCoastTicks = 0;
                spawnBurst(player);
            }
            wasDashing = true;
            dashAge++;
            spawnTrail(player, false);
            return true;
        }
        if (wasDashing) {
            // 氷のルーンでは5tick後も慣性が残る。拒否された短い予測移動には余韻を付けない。
            iceCoastTicks = dashAge >= 3 && MantleCalibration.retainsDrift(stack) ? MAX_ICE_COAST_TICKS : 0;
            wasDashing = false;
        }
        if (iceCoastTicks > 0 && MantleCalibration.retainsDrift(stack)
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.04D
                && previousPosition != null
                && player.position().subtract(previousPosition).horizontalDistanceSqr() > 0.01D) {
            spawnTrail(player, true);
            iceCoastTicks--;
            return true;
        }
        reset();
        return false;
    }

    private static void spawnBurst(LocalPlayer player) {
        var forward = Vec3.directionFromRotation(0, player.getYRot());
        var right = new Vec3(-forward.z, 0, forward.x);
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 2; i++) {
                var position = player.position().add(forward.scale(i == 0 ? 0.35D : -0.2D))
                        .add(right.scale(side * (0.55D + i * 0.15D)))
                        .add(0, player.getEyeHeight() - 0.55D, 0);
                var velocity = right.scale(side * 0.025D).add(0, 0.012D, 0);
                player.level().addParticle(i == 0 ? COMET_SPARK : STAR,
                        position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }

    private static void spawnTrail(LocalPlayer player, boolean coasting) {
        var current = player.position();
        var travel = previousPosition == null ? Vec3.ZERO : current.subtract(previousPosition);
        if (travel.lengthSqr() > 64.0D) travel = Vec3.ZERO;
        var horizontal = new Vec3(travel.x, 0, travel.z);
        var direction = horizontal.lengthSqr() > 1.0e-4D
                ? horizontal.normalize() : ShootingStarMantleRuntime.state(player).dashDirection;
        var right = new Vec3(-direction.z, 0, direction.x);
        int count = Mth.clamp((int) Math.ceil(travel.length() / (coasting ? 0.8D : 0.45D)),
                coasting ? 1 : 2, coasting ? 3 : 5);
        var random = player.getRandom();
        for (int i = 0; i < count; i++) {
            var base = current.subtract(travel.scale((i + random.nextDouble()) / count));
            var position = base.subtract(direction.scale(0.3D))
                    .add(right.scale((random.nextDouble() - 0.5D) * 0.7D))
                    .add(0, 0.8D + random.nextDouble() * 0.6D, 0);
            var velocity = direction.scale(-0.015D)
                    .add(right.scale((random.nextDouble() - 0.5D) * 0.018D))
                    .add(0, 0.006D, 0);
            var particle = coasting ? ICE_SPARK : random.nextInt(8) == 0 ? STAR : COMET_SPARK;
            player.level().addParticle(particle, position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z);
        }
        previousPosition = current;
    }

    static void reset() {
        previousPosition = null;
        wasDashing = false;
        dashAge = 0;
        iceCoastTicks = 0;
    }
}
