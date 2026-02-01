package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public class RaycastTools {

    private RaycastTools() {
        // do nothing.
    }

    public enum TargetType {
        NONE,
        LIVING_ENTITY,
        BLOCK,
    }

    public record TargetResult(
            TargetType hitType,
            Vec3 hitPosition,
            Entity hitEntity
    ) {}

    public static TargetResult raycastFromEye(LivingEntity source, double range, Predicate<Entity> predicate) {
        var level = source.level();

        var start = source.getEyePosition(1.0F);
        var look = source.getViewVector(1.0F);
        var end = start.add(look.scale(range));

        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                source
        ));

        var effectiveEnd = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            effectiveEnd = blockHit.getLocation();
        }

        var searchBox = source.getBoundingBox()
                .expandTowards(look.scale(range))
                .inflate(1.0D);

        var entityHit = ProjectileUtil.getEntityHitResult(
                level,
                source,
                start,
                effectiveEnd,
                searchBox,
                e -> e != source
                        && e.isAlive()
                        && predicate.test(e)
        );

        if (entityHit != null) {
            var e = entityHit.getEntity();
            var center = e.getBoundingBox().getCenter();
            return new TargetResult(TargetType.LIVING_ENTITY, center, e);
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            return new TargetResult(TargetType.BLOCK, blockHit.getLocation(), null);
        }

        return new TargetResult(TargetType.NONE, end, null);
    }

    public static boolean hasLineOfSight(Level level, Entity source, Entity target) {
        var from = getEntityTargetPosition(source);
        var to = getEntityTargetPosition(target);

        BlockHitResult hit = level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                source
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static Vec3 getEntityTargetPosition(Entity entity){
        if (entity instanceof LivingEntity le) return le.getEyePosition();
        return entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    public static Optional<Entity> findNearestEntityInForwardBox(
            Level level,
            Entity source,
            Vec3 dir,
            double range,
            double halfWidth,
            double halfHeight,
            Predicate<Entity> predicate,
            boolean blockOcclusion
    ) {
        if (range <= 0) {
            return Optional.empty();
        }

        var origin = getEntityTargetPosition(source);
        var forward = (dir.lengthSqr() > 0 ? dir : source.getLookAngle()).normalize();

        var worldUp = new Vec3(0, 1, 0);
        var right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0e-6) {
            right = forward.cross(new Vec3(1, 0, 0));
        }
        final var fixedRight = right.normalize();

        var upVec = right.cross(forward).normalize();
        var inflate = range + Math.max(halfWidth, halfHeight);
        var candidates = source.getBoundingBox().inflate(inflate);


        return level.getEntities(source, candidates, e ->
                        e != source
                                && e.isAlive()
                                && predicate.test(e)
                )
                .stream()
                .filter(e -> {
                    var p = getEntityTargetPosition(e);
                    var v = p.subtract(origin);

                    var x = v.dot(fixedRight);
                    var y = v.dot(upVec);
                    var z = v.dot(forward);

                    // 回転させた直方体の範囲内に入っているか.
                    if (z > range) return false;
                    if (Math.abs(x) > halfWidth) return false;
                    if (Math.abs(y) > halfHeight) return false;
                    return (z >= 0);
                })
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(source)))
                .filter(e -> !blockOcclusion || hasLineOfSight(level, source, e))
                .findFirst();
    }
}
