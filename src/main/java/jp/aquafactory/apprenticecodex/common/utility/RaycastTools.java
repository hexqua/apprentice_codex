package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
            Entity hitEntity,
            BlockPos hitBlock
    ) {}

    public static TargetResult raycast(Entity source, Vec3 look, double blockRange, double entityRange, double boxWidth, Predicate<Entity> predicate){
        var level = source.level();

        var start = source.getEyePosition(1.0F);
        var end = start.add(look.scale(blockRange));

        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                source
        ));

        var effectiveEnd = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            effectiveEnd = blockHit.getLocation();
        }

        var searchBox = source.getBoundingBox()
                .expandTowards(look.scale(entityRange))
                .inflate(boxWidth / 2);

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
            var hitBox = e.getBoundingBox().inflate(0.1);
            var dir = look.normalize();
            var rayHitPosition = rayAabbHit(start, dir, start.distanceTo(effectiveEnd), hitBox);
            if (rayHitPosition == null) {
                // 再判定に失敗したら中央にフォールバック.
                rayHitPosition = e.getBoundingBox().getCenter();
            }

            return new TargetResult(TargetType.LIVING_ENTITY, rayHitPosition, e, null);
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            return new TargetResult(TargetType.BLOCK, blockHit.getLocation(), null, blockHit.getBlockPos());
        }

        return new TargetResult(TargetType.NONE, end, null, null);
    }


    public static TargetResult raycastRangeAttribute(LivingEntity source, double boxWidth, Predicate<Entity> predicate){
        var blockRange = source.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        var entityRange = source.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        return raycast(source, source.getViewVector(1.0F), blockRange, entityRange, boxWidth, predicate);
    }

    public static TargetResult raycast(Entity source, Vec3 look, double range, double boxWidth, Predicate<Entity> predicate){
        return raycast(source, look, range, range, boxWidth, predicate);
    }

    public static TargetResult raycastFromEye(Entity source, double range, double boxWidth, Predicate<Entity> predicate) {
        return raycast(source, source.getViewVector(1.0F), range, boxWidth, predicate);
    }

    @Nullable
    private static Vec3 rayAabbHit(Vec3 start, Vec3 dirNormalized, double maxDistance, AABB box) {
        var tMin = 0.0;
        var tMax = maxDistance;

        // 0: x, 1: y, 2: z
        for (var axis = 0; axis < 3; axis++) {
            var s = axis == 0 ? start.x : axis == 1 ? start.y : start.z;
            var d = axis == 0 ? dirNormalized.x : axis == 1 ? dirNormalized.y : dirNormalized.z;
            var min = axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
            var max = axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;

            if (Math.abs(d) < 1e-9) {
                if (s < min || s > max) return null;
                continue;
            }

            var invD = 1.0 / d;
            var t1 = (min - s) * invD;
            var t2 = (max - s) * invD;
            if (t1 > t2) {
                var tmp = t1;
                t1 = t2;
                t2 = tmp;
            }

            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);

            if (tMin > tMax) {
                return null;
            }
        }

        var tHit = (tMin > 1e-6) ? tMin : tMax;
        if (tHit < 0.0 || tHit > maxDistance) {
            return null;
        }

        return start.add(dirNormalized.scale(tHit));
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

    public static Vec3 randomRotateInCone(Vec3 dirNormalized, float maxAngleDeg, RandomSource random) {
        // 度数を計算用のラジアンに変換.
        var maxAngleRad = maxAngleDeg * ((float) Math.PI / 180f);

        // 円錐内の角度を「面積一様」になるようサンプリング.
        var u = random.nextDouble();
        var v = random.nextDouble();
        var cosMax = Math.cos(maxAngleRad);
        var cosTheta = Mth.lerp(u, cosMax, 1.0);
        var sinTheta = Math.sqrt(Math.max(0.0, 1.0 - cosTheta * cosTheta));
        var phi = 2.0 * Math.PI * v;

        // 回転させる.
        var up = Math.abs(dirNormalized.y) < 0.999 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        var uVec = dirNormalized.cross(up).normalize();
        var vVec = dirNormalized.cross(uVec).normalize();
        return
                uVec.scale(sinTheta * Math.cos(phi))
                        .add(vVec.scale(sinTheta * Math.sin(phi)))
                        .add(dirNormalized.scale(cosTheta))
                        .normalize();
    }

    public static Set<Entity> sampleBeamHits(
            Level level,
            Vec3 start,
            Vec3 end,
            double radius,
            double step,
            Predicate<Entity> filter
    ) {
        var delta = end.subtract(start);
        var len = delta.length();
        if (len < 1.0e-6) return Set.of();

        var dir = delta.scale(1.0 / len);
        var broad = new AABB(start, end).inflate(radius + 0.5);
        var candidates = level.getEntities((Entity) null, broad, filter);
        var hits = new HashSet<Entity>();

        int steps = Math.max(1, (int) Math.ceil(len / step));
        for (Entity e : candidates) {
            if (!(e instanceof LivingEntity le)) continue;
            var box = e.getBoundingBox().inflate(radius);

            for (var i = 0; i <= steps; ++i) {
                var t = i / (double) steps;
                var p = start.add(dir.scale(len * t));

                if (box.contains(p)) {
                    hits.add(le);
                    break;
                }
            }
        }

        return hits;
    }
}
