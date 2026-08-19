package jp.aquafactory.apprenticecodex.entity.broom;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HoverrideBroomRushAttack {
    public static final double MINIMUM_SPEED = 0.5D;
    public static final double MAXIMUM_DAMAGE_SPEED = 0.7D;
    public static final double CONTACT_PADDING = 0.5D;
    public static final int MAX_TARGETS_PER_TICK = 4;
    public static final int MAX_OBSERVATION_TICKS = 2;
    public static final double MOVEMENT_TOLERANCE_PER_TICK = 0.05D;
    public static final double MINIMUM_KNOCKBACK = 0.5D;
    public static final double MAXIMUM_KNOCKBACK = 1.0D;

    private HoverrideBroomRushAttack() {
    }

    public static double speedRatio(double speed) {
        return Mth.clamp(
                (speed - MINIMUM_SPEED) / (MAXIMUM_DAMAGE_SPEED - MINIMUM_SPEED),
                0.0D,
                1.0D
        );
    }

    public static float baseDamage(double speed, double minimumDamage, double maximumDamage) {
        return (float)Mth.lerp(speedRatio(speed), minimumDamage, maximumDamage);
    }

    public static double knockbackStrength(double speed) {
        return Mth.lerp(speedRatio(speed), MINIMUM_KNOCKBACK, MAXIMUM_KNOCKBACK);
    }

    public static boolean intersectsPath(AABB broomBox, Vec3 movement, AABB targetBox) {
        var startBox = broomBox.move(movement.reverse());
        var halfX = broomBox.getXsize() * 0.5D + CONTACT_PADDING;
        var halfY = broomBox.getYsize() * 0.5D + CONTACT_PADDING;
        var halfZ = broomBox.getZsize() * 0.5D + CONTACT_PADDING;
        var expandedTarget = targetBox.inflate(halfX, halfY, halfZ);
        var start = startBox.getCenter();
        var end = broomBox.getCenter();
        return expandedTarget.contains(start) || expandedTarget.clip(start, end).isPresent();
    }

    public static double pathEntryDistanceSqr(AABB broomBox, Vec3 movement, AABB targetBox) {
        var startBox = broomBox.move(movement.reverse());
        var halfX = broomBox.getXsize() * 0.5D + CONTACT_PADDING;
        var halfY = broomBox.getYsize() * 0.5D + CONTACT_PADDING;
        var halfZ = broomBox.getZsize() * 0.5D + CONTACT_PADDING;
        var expandedTarget = targetBox.inflate(halfX, halfY, halfZ);
        var start = startBox.getCenter();
        if (expandedTarget.contains(start)) {
            return 0.0D;
        }
        return expandedTarget.clip(start, broomBox.getCenter())
                .map(start::distanceToSqr)
                .orElse(Double.POSITIVE_INFINITY);
    }
}
