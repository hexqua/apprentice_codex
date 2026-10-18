package jp.aquafactory.apprenticecodex.utility;

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

public final class RaycastTools {
    private static final double ORIENTED_BOX_EPSILON = 1.0E-7D;
    private static final double OCCLUSION_POINT_INSET = 1.0E-4D;

    private RaycastTools() {}

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

    public record HorizontalOrientedBox(
            Vec3 faceCenter,
            Vec3 forward,
            double halfWidth,
            double halfHeight,
            double depth
    ) {
        public HorizontalOrientedBox {
            var horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
            if (horizontalForward.lengthSqr() <= ORIENTED_BOX_EPSILON) {
                throw new IllegalArgumentException("forward must have a horizontal component");
            }
            if (halfWidth < 0.0D || halfHeight < 0.0D || depth < 0.0D) {
                throw new IllegalArgumentException("box dimensions must not be negative");
            }
            forward = horizontalForward.normalize();
        }
    }

    public record OrientedBoxHit(Entity entity, boolean blockOccluded) {
    }

    /** 固定ヨーの箱を平行移動したときの最初の接触時刻（0～1）。上下移動でも箱は傾けない。 */
    public static OptionalDouble firstMovingHorizontalBoxContact(AABB target, HorizontalOrientedBox box, Vec3 movement) {
        var right = horizontalRight(box.forward());
        var halfDepth = box.depth() * 0.5;
        var center = box.faceCenter().add(box.forward().scale(halfDepth));
        var delta = target.getCenter().subtract(center);
        double enter = 0, exit = 1;
        // 各SAT軸の重なる時間区間を交差する。端点だけの検査では高速投擲が敵を飛び越えてしまう。
        for (var axis : List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1), right, box.forward())) {
            double radius = Math.abs(axis.dot(right)) * box.halfWidth()
                    + Math.abs(axis.y) * box.halfHeight() + Math.abs(axis.dot(box.forward())) * halfDepth
                    + (Math.abs(axis.x) * target.getXsize() + Math.abs(axis.y) * target.getYsize()
                    + Math.abs(axis.z) * target.getZsize()) * 0.5 + ORIENTED_BOX_EPSILON;
            double offset = delta.dot(axis), speed = movement.dot(axis);
            if (Math.abs(speed) < ORIENTED_BOX_EPSILON) {
                if (Math.abs(offset) > radius) return OptionalDouble.empty();
            } else {
                double a = (offset - radius) / speed, b = (offset + radius) / speed;
                enter = Math.max(enter, Math.min(a, b));
                exit = Math.min(exit, Math.max(a, b));
                if (enter > exit) return OptionalDouble.empty();
            }
        }
        return OptionalDouble.of(enter);
    }

    public static AABB movingHorizontalBoxBounds(HorizontalOrientedBox box, Vec3 movement) {
        var start = createHorizontalOrientedBoxBounds(box);
        return start.minmax(start.move(movement)).inflate(ORIENTED_BOX_EPSILON);
    }

    private static final class OrientedBoxHitAccumulator {
        private final Entity entity;
        private boolean blockOccluded = true;

        private OrientedBoxHitAccumulator(Entity entity) {
            this.entity = entity;
        }

        private void include(boolean partBlockOccluded) {
            blockOccluded &= partBlockOccluded;
        }

        private OrientedBoxHit toResult() {
            return new OrientedBoxHit(entity, blockOccluded);
        }
    }

    /**
     * 既存の照準・弾道・移動先を維持する互換API。boxWidthは候補検索幅だけを指定する。
     * 最終判定はProjectileUtilの固定0.3膨張、命中位置は0.1膨張で再計算し、失敗時は対象中央を返す。
     * 幅を命中判定にも反映する救済にはraycastFromEyeWithAimAssistを使用する。
     */
    public static TargetResult raycast(Entity source, Vec3 look, double blockRange, double entityRange, double boxWidth, Predicate<Entity> predicate){
        return raycast(source, look, blockRange, entityRange, boxWidth, ClipContext.Block.COLLIDER, predicate);
    }

    /** {@link #raycast(Entity, Vec3, double, double, double, Predicate)} と同じ互換判定でブロック形状を指定する。 */
    public static TargetResult raycast(Entity source, Vec3 look, double blockRange, double entityRange, double boxWidth,
                                       ClipContext.Block blockShape, Predicate<Entity> predicate){
        var level = source.level();

        var start = source.getEyePosition(1.0F);
        var end = start.add(look.scale(blockRange));

        var blockHit = level.clip(new ClipContext(
                start,
                end,
                blockShape,
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
                        && !e.isSpectator()
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

    public static TargetResult raycast(Entity source, Vec3 look, double range, double boxWidth,
                                       ClipContext.Block blockShape, Predicate<Entity> predicate){
        return raycast(source, look, range, range, boxWidth, blockShape, predicate);
    }

    /** boxWidthは候補検索幅。最終判定と命中位置は既存の {@link #raycast} の仕様を維持する。 */
    public static TargetResult raycastFromEye(Entity source, double range, double boxWidth, Predicate<Entity> predicate) {
        return raycast(source, source.getViewVector(1.0F), range, boxWidth, predicate);
    }

    /**
     * 視線への照準補助。全幅assistWidthの半分だけ対象AABBを各軸へ膨張し、最初に交差する有効対象を返す。
     * 中心線は衝突ブロックで止め、対象上の可視点も射程内かつ停止位置より手前にあることを要求する。
     * 命中位置は仮想的な膨張箱上ではなく対象内に置き、稲妻などの終点が敵の横に残るのを防ぐ。
     */
    public static TargetResult raycastFromEyeWithAimAssist(Entity source, double range, double assistWidth,
                                                          Predicate<Entity> predicate) {
        if (!Double.isFinite(range) || range <= 0 || !Double.isFinite(assistWidth) || assistWidth < 0) {
            throw new IllegalArgumentException("Aim assist requires a positive finite range and a nonnegative finite width");
        }
        var level = source.level();
        var start = source.getEyePosition(1.0F);
        var forward = source.getViewVector(1.0F).normalize();
        var end = start.add(forward.scale(range));
        var blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, source));
        var effectiveEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        var maxDistance = start.distanceTo(effectiveEnd);
        var halfWidth = assistWidth / 2;
        var searchBox = new AABB(start, effectiveEnd).inflate(halfWidth);
        Entity closest = null;
        Vec3 impact = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        for (var candidate : level.getEntities(source, searchBox,
                e -> e != source && e.isAlive() && !e.isSpectator() && e.isPickable() && predicate.test(e))) {
            var targetBox = candidate.getBoundingBox();
            var assistedBox = targetBox.inflate(halfWidth);
            var intersection = assistedBox.contains(start) ? Optional.of(start) : assistedBox.clip(start, effectiveEnd);
            if (intersection.isEmpty()) continue;
            var distanceSqr = start.distanceToSqr(intersection.get());
            if (distanceSqr >= closestDistanceSqr) continue;
            var hitPosition = findAimAssistImpact(level, source, start, forward, range, maxDistance,
                    targetBox, intersection.get());
            if (hitPosition == null) continue;
            closest = candidate;
            impact = hitPosition;
            closestDistanceSqr = distanceSqr;
        }
        if (closest != null) return new TargetResult(TargetType.LIVING_ENTITY, impact, closest, null);
        if (blockHit.getType() != HitResult.Type.MISS) {
            return new TargetResult(TargetType.BLOCK, blockHit.getLocation(), null, blockHit.getBlockPos());
        }
        return new TargetResult(TargetType.NONE, end, null, null);
    }

    @Nullable
    private static Vec3 findAimAssistImpact(Level level, Entity source, Vec3 start, Vec3 forward,
                                            double range, double maxDistance, AABB targetBox, Vec3 intersection) {
        var nearest = new Vec3(Mth.clamp(intersection.x, targetBox.minX, targetBox.maxX),
                Mth.clamp(intersection.y, targetBox.minY, targetBox.maxY),
                Mth.clamp(intersection.z, targetBox.minZ, targetBox.maxZ));
        var points = new ArrayList<Vec3>();
        points.add(insetTowardsCenter(nearest, targetBox.getCenter()));
        // 中央だけで遮蔽を決めると、体の一部が見えている敵まで取り逃す。既存の可視点群を共有する。
        points.addAll(createOcclusionTargetPoints(targetBox));
        for (var point : points) {
            var offset = point.subtract(start);
            var depth = offset.dot(forward);
            // 膨張箱だけが手前へ伸びても、壁の奥や射程外の実体には命中させない。
            if (depth < 0 || depth > maxDistance || offset.lengthSqr() > range * range) continue;
            var blockHit = level.clip(new ClipContext(start, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, source));
            if (blockHit.getType() == HitResult.Type.MISS) return point;
        }
        return null;
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

    public static boolean hasLineOfSight(Level level, Entity source, Vec3 from, Entity target) {
        return !isAabbBlockOccluded(level, source, from, target.getBoundingBox());
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
                                && !e.isSpectator()
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


    public static Set<Entity> hitsAabb(
            Level level,
            Vec3 point,
            double radius,
            Predicate<Entity> filter
    ) {
        var aabb = new AABB(point, point).inflate(radius + 0.5);
        var candidates = level.getEntities((Entity) null, aabb, filter);
        var hits = new HashSet<Entity>();

        for (Entity e : candidates) {
            var box = e.getBoundingBox().inflate(radius);

            if (box.contains(point)) {
                hits.add(e);
            }
        }

        return hits;
    }

    public static List<OrientedBoxHit> hitsHorizontalOrientedBox(
            Level level,
            Entity source,
            HorizontalOrientedBox box,
            Predicate<Entity> filter
    ) {
        var broadBounds = createHorizontalOrientedBoxBounds(box).inflate(ORIENTED_BOX_EPSILON);
        var sourceInsideBlock = isInsideBlockCollision(level, source, box.faceCenter());
        var hitsByTarget = new LinkedHashMap<UUID, OrientedBoxHitAccumulator>();

        for (var rawTarget : level.getEntities(source, broadBounds, Entity::isAlive)) {
            if (!intersectsHorizontalOrientedBox(rawTarget.getBoundingBox(), box)) {
                continue;
            }

            // ForgeはPartEntityも候補へ返すため、部位AABBで交差を確定してから親単位へ集約する。
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (!target.isAlive() || !filter.test(target)) {
                continue;
            }

            var targetHit = hitsByTarget.computeIfAbsent(
                    target.getUUID(),
                    ignored -> new OrientedBoxHitAccumulator(target)
            );
            var partBlockOccluded = sourceInsideBlock
                    || isAabbBlockOccluded(level, source, box.faceCenter(), rawTarget.getBoundingBox());
            targetHit.include(partBlockOccluded);
        }

        return hitsByTarget.values().stream()
                .map(OrientedBoxHitAccumulator::toResult)
                .toList();
    }

    private static AABB createHorizontalOrientedBoxBounds(HorizontalOrientedBox box) {
        var right = horizontalRight(box.forward());
        var halfDepth = box.depth() * 0.5D;
        var center = box.faceCenter().add(box.forward().scale(halfDepth));
        var halfX = Math.abs(right.x) * box.halfWidth() + Math.abs(box.forward().x) * halfDepth;
        var halfZ = Math.abs(right.z) * box.halfWidth() + Math.abs(box.forward().z) * halfDepth;
        return new AABB(
                center.x - halfX,
                box.faceCenter().y - box.halfHeight(),
                center.z - halfZ,
                center.x + halfX,
                box.faceCenter().y + box.halfHeight(),
                center.z + halfZ
        );
    }

    private static boolean intersectsHorizontalOrientedBox(AABB targetBox, HorizontalOrientedBox box) {
        var right = horizontalRight(box.forward());
        var halfDepth = box.depth() * 0.5D;
        var boxCenter = box.faceCenter().add(box.forward().scale(halfDepth));
        var targetCenter = targetBox.getCenter();
        var centerDelta = targetCenter.subtract(boxCenter);
        var targetHalfX = targetBox.getXsize() * 0.5D;
        var targetHalfY = targetBox.getYsize() * 0.5D;
        var targetHalfZ = targetBox.getZsize() * 0.5D;

        if (Math.abs(centerDelta.y) > box.halfHeight() + targetHalfY + ORIENTED_BOX_EPSILON) {
            return false;
        }

        return overlapsOnHorizontalAxis(centerDelta, new Vec3(1.0D, 0.0D, 0.0D),
                right, box.forward(), box.halfWidth(), halfDepth, targetHalfX, targetHalfZ)
                && overlapsOnHorizontalAxis(centerDelta, new Vec3(0.0D, 0.0D, 1.0D),
                right, box.forward(), box.halfWidth(), halfDepth, targetHalfX, targetHalfZ)
                && overlapsOnHorizontalAxis(centerDelta, right,
                right, box.forward(), box.halfWidth(), halfDepth, targetHalfX, targetHalfZ)
                && overlapsOnHorizontalAxis(centerDelta, box.forward(),
                right, box.forward(), box.halfWidth(), halfDepth, targetHalfX, targetHalfZ);
    }

    private static boolean overlapsOnHorizontalAxis(
            Vec3 centerDelta,
            Vec3 axis,
            Vec3 orientedRight,
            Vec3 orientedForward,
            double orientedHalfWidth,
            double orientedHalfDepth,
            double targetHalfX,
            double targetHalfZ
    ) {
        var centerDistance = Math.abs(centerDelta.dot(axis));
        var orientedRadius = Math.abs(orientedRight.dot(axis)) * orientedHalfWidth
                + Math.abs(orientedForward.dot(axis)) * orientedHalfDepth;
        var targetRadius = Math.abs(axis.x) * targetHalfX + Math.abs(axis.z) * targetHalfZ;
        return centerDistance <= orientedRadius + targetRadius + ORIENTED_BOX_EPSILON;
    }

    private static Vec3 horizontalRight(Vec3 forward) {
        return new Vec3(-forward.z, 0.0D, forward.x);
    }

    private static boolean isInsideBlockCollision(Level level, Entity source, Vec3 point) {
        var pointBounds = new AABB(point, point).inflate(ORIENTED_BOX_EPSILON);
        for (var collisionShape : level.getBlockCollisions(source, pointBounds)) {
            for (var collisionBox : collisionShape.toAabbs()) {
                if (point.x > collisionBox.minX + ORIENTED_BOX_EPSILON
                        && point.x < collisionBox.maxX - ORIENTED_BOX_EPSILON
                        && point.y > collisionBox.minY + ORIENTED_BOX_EPSILON
                        && point.y < collisionBox.maxY - ORIENTED_BOX_EPSILON
                        && point.z > collisionBox.minZ + ORIENTED_BOX_EPSILON
                        && point.z < collisionBox.maxZ - ORIENTED_BOX_EPSILON) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAabbBlockOccluded(Level level, Entity source, Vec3 start, AABB targetBox) {
        for (var targetPoint : createOcclusionTargetPoints(targetBox)) {
            var blockHit = level.clip(new ClipContext(
                    start,
                    targetPoint,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    source
            ));
            if (blockHit.getType() == HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    private static List<Vec3> createOcclusionTargetPoints(AABB box) {
        var center = box.getCenter();
        var points = new ArrayList<Vec3>(15);
        points.add(center);
        points.add(insetTowardsCenter(new Vec3(box.minX, center.y, center.z), center));
        points.add(insetTowardsCenter(new Vec3(box.maxX, center.y, center.z), center));
        points.add(insetTowardsCenter(new Vec3(center.x, box.minY, center.z), center));
        points.add(insetTowardsCenter(new Vec3(center.x, box.maxY, center.z), center));
        points.add(insetTowardsCenter(new Vec3(center.x, center.y, box.minZ), center));
        points.add(insetTowardsCenter(new Vec3(center.x, center.y, box.maxZ), center));

        for (var x : new double[]{box.minX, box.maxX}) {
            for (var y : new double[]{box.minY, box.maxY}) {
                for (var z : new double[]{box.minZ, box.maxZ}) {
                    points.add(insetTowardsCenter(new Vec3(x, y, z), center));
                }
            }
        }
        return points;
    }

    private static Vec3 insetTowardsCenter(Vec3 point, Vec3 center) {
        var toCenter = center.subtract(point);
        var distance = toCenter.length();
        if (distance <= OCCLUSION_POINT_INSET) {
            return center;
        }
        return point.add(toCenter.scale(OCCLUSION_POINT_INSET / distance));
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
            var target = CombatTools.resolutePartEntity(e);
            if (!target.isAlive()) continue;
            var box = e.getBoundingBox().inflate(radius);

            for (var i = 0; i <= steps; ++i) {
                var t = i / (double) steps;
                var p = start.add(dir.scale(len * t));

                if (box.contains(p)) {
                    // 部位の箱で命中を確定してから本体へ集約し、複数部位による多重ダメージを防ぐ。
                    hits.add(target);
                    break;
                }
            }
        }

        return hits;
    }
}
