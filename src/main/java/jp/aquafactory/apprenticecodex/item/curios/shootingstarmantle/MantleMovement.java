package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import jp.aquafactory.apprenticecodex.entity.broom.BroomSurfaceScanner;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MantleMovement {
    private MantleMovement() { }

    public static Vec3 direction(float forward, float strafe, float yaw) {
        if (!Float.isFinite(forward) || !Float.isFinite(strafe) || !Float.isFinite(yaw)) return Vec3.ZERO;
        var input = new Vec3(Mth.clamp(strafe, -1, 1), 0, Mth.clamp(forward, -1, 1));
        if (input.lengthSqr() < 1.0e-6) input = new Vec3(0, 0, -1);
        return input.normalize().yRot(-yaw * Mth.DEG_TO_RAD);
    }

    public static int movingTicks(Vec3 previous, Vec3 current, int remaining) {
        return previous != null && current.subtract(previous).horizontalDistanceSqr() > 0.0001
                ? 10 : Math.max(0, remaining - 1);
    }

    public static double vertical(double velocity, double y, double surface, int movingTicks) {
        // 低速落下と同じ重力・空気抵抗を使い、地表近くでは箒同様に目標へ減速する。
        double falling = Math.max(-0.49, Math.min(0, velocity) * 0.98 - 0.01);
        if (!Double.isFinite(surface)) return velocity > 0 ? velocity : falling;
        double error = surface + (movingTicks > 0 ? 2 : 5) - y;
        double controlled = error < -1 ? Math.max(error, falling)
                : Mth.clamp(error * 0.2 + velocity * 0.6, -0.15, 0.15);
        // 外部の上昇は目標高度へ押し戻さず、下降と自律浮遊は従来の制御を使う。
        return velocity > 0 ? Math.max(velocity, controlled) : controlled;
    }

    static void decayExternalUpwardMotion(Player player, double incomingY, double usedY) {
        if (incomingY <= 0 || usedY != incomingY) return;
        var movement = player.getDeltaMovement();
        if (movement.y > 0) {
            // 外套のtravelでは重力処理も置き換わる。1.20.1にはGRAVITY属性がないため、バニラの0.08で上昇慣性を減衰する。
            player.setDeltaMovement(movement.x, movement.y * 0.98 - 0.08, movement.z);
        }
    }

    public static void travel(Player player, Vec3 input, ShootingStarMantleRuntime.State state) {
        if (state.blink.travel(player, state)) return;
        if (state.elemental.travel(player, state)) return;
        state.movingTicks = movingTicks(state.lastPosition, player.position(), state.movingTicks);
        state.lastPosition = player.position();
        var surface = BroomSurfaceScanner.findSurfaceBelow(player.level(), player.getX(), player.getY(), player.getZ(), 16, true);
        player.moveRelative(0.02F, new Vec3(input.x, 0, input.z));
        var motion = player.getDeltaMovement();
        double y = vertical(motion.y, player.getY(), surface.map(BroomSurfaceScanner.Surface::y).orElse(Double.NaN), state.movingTicks);
        var horizontal = state.dashTicks > 0 ? state.dashDirection : new Vec3(motion.x, 0, motion.z);
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
        player.move(MoverType.SELF, player.getDeltaMovement());
        decayExternalUpwardMotion(player, motion.y, y);
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.91, 1, 0.91));
        player.fallDistance = 0;
        if (state.dashTicks > 0 && --state.dashTicks == 0) {
            finishImpulse(player);
        }
        player.calculateEntityAnimation(false);
    }

    public static double hoverVertical(Player player, ShootingStarMantleRuntime.State state) {
        var surface = BroomSurfaceScanner.findSurfaceBelow(player.level(), player.getX(), player.getY(), player.getZ(), 16, true);
        return vertical(player.getDeltaMovement().y, player.getY(), surface.map(BroomSurfaceScanner.Surface::y).orElse(Double.NaN), state.movingTicks);
    }

    public static void finishImpulse(Player player) {
        // 通常は5tick後に停止する。氷のルーンだけは通常の移動減衰へ慣性を引き継ぐ。
        if (!MantleCalibration.retainsDrift(ShootingStarMantleRuntime.findEquipped(player))) {
            player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        }
    }
}
