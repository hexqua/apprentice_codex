package jp.aquafactory.apprenticecodex.spell.flyswatter;

import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/** 候補の評価と実際の飛行で、同じ曲線分割と弾の太さを使う。 */
public final class FlySwatterTrajectory {
    public static final double FLIGHT_SPEED = 2.2;
    private static final int SPREAD_PATTERNS = 8;
    private static final double SPREAD_STRENGTH = 0.6;
    private static final double WIDE_SPREAD_STRENGTH = 1.2;
    private static final double WIDE_START_MULTIPLIER = 2;
    private static final double WIDE_FULL_DISTANCE = 16;
    private static final double START_TANGENT_RATIO = 0.65;
    private static final double MIN_START_TANGENT = 2;
    private static final double MAX_START_TANGENT = 24;
    private static final double END_TANGENT_RATIO = 0.5;
    private static final double MIN_END_TANGENT = 1;
    private static final double MAX_END_TANGENT = 32;
    private static final int MIN_ARRIVAL_TICKS = 4;
    private static final int MAX_ARRIVAL_TICKS = 180;
    private static final double SEGMENT_LENGTH = 0.25;
    private static final double CURVE_ERROR = 0.02;
    private static final int MAX_DEPTH = 12;

    private FlySwatterTrajectory() {}

    public record Plan(LockOnRayCurve curve, int arrivalTicks, int pattern) {}
    public record Candidate(LockOnRayCurve curve, int pattern) {}
    public record Sample(double fraction, Vec3 position) {}
    public record Obstruction(double fraction, Vec3 position, @Nullable BlockHitResult hit, boolean unloaded) {}

    public static Plan select(Level level, Entity projectile, Vec3 origin, Vec3 facing, Vec3 target, int shot) {
        var candidates = candidates(origin, facing, target, shot);
        Candidate best = null;
        Evaluation bestEvaluation = null;
        double farthest = -1;
        var states = new HashMap<BlockPos, BlockState>();
        for (var candidate : candidates) {
            var evaluation = new Evaluation(level, projectile, candidate.curve(), states);
            evaluation.advance();
            var obstruction = evaluation.obstruction;
            if (obstruction == null) return plan(candidate, evaluation.length);
            double distance = origin.distanceToSqr(obstruction.position());
            if (distance > farthest) {
                farthest = distance;
                best = candidate;
                bestEvaluation = evaluation;
            }
        }
        // 全候補が塞がれていても、長さの残りを計算するのは採用した一つだけ。
        Objects.requireNonNull(bestEvaluation).finishLength();
        return plan(Objects.requireNonNull(best), bestEvaluation.length);
    }

    public static List<Candidate> candidates(Vec3 origin, Vec3 facing, Vec3 target, int shot) {
        var offset = target.subtract(origin);
        double distance = offset.length();
        var forward = unit(facing, new Vec3(0, 0, 1));
        var right = unit(forward.cross(Math.abs(forward.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)),
                new Vec3(1, 0, 0));
        var up = right.cross(forward).normalize();
        var arrivalDirection = unit(offset, forward);
        double startLength = Mth.clamp(distance * START_TANGENT_RATIO, MIN_START_TANGENT, MAX_START_TANGENT);
        var endTangent = arrivalDirection.scale(Mth.clamp(distance * END_TANGENT_RATIO, MIN_END_TANGENT, MAX_END_TANGENT));
        double wideScale = Mth.clamp(distance / WIDE_FULL_DISTANCE, 0, 1);
        // Listの各要素は参照された時にだけ生成する。先頭が通れば残り17曲線は作らない。
        return new AbstractList<>() {
            @Override public int size() { return SPREAD_PATTERNS * 2 + 2; }

            @Override public Candidate get(int index) {
                Objects.checkIndex(index, size());
                if (index < SPREAD_PATTERNS * 2) {
                    boolean wide = index < SPREAD_PATTERNS;
                    int pattern = Math.floorMod(index + shot, SPREAD_PATTERNS);
                    double angle = pattern * Math.PI * 2 / SPREAD_PATTERNS;
                    var spread = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                    double strength = wide ? Mth.lerp(wideScale, SPREAD_STRENGTH, WIDE_SPREAD_STRENGTH) : SPREAD_STRENGTH;
                    double length = wide ? startLength * (1 + wideScale * (WIDE_START_MULTIPLIER - 1)) : startLength;
                    var direction = forward.add(spread.scale(strength)).normalize();
                    return new Candidate(new LockOnRayCurve(origin, target, direction.scale(length), endTangent),
                            pattern + (wide ? 0 : SPREAD_PATTERNS));
                }
                var tangent = index == SPREAD_PATTERNS * 2 ? forward.scale(startLength) : offset;
                // 最後は直線。従来の控えめな8候補とともに、狭い場所の通行可能性を維持する。
                return new Candidate(new LockOnRayCurve(origin, target, tangent,
                        index == SPREAD_PATTERNS * 2 ? endTangent : offset), index);
            }
        };
    }

    private static Plan plan(Candidate candidate, double length) {
        int ticks = Mth.clamp((int) Math.ceil(length / FLIGHT_SPEED), MIN_ARRIVAL_TICKS, MAX_ARRIVAL_TICKS);
        return new Plan(candidate.curve(), ticks, candidate.pattern());
    }

    public static List<Sample> samples(LockOnRayCurve curve) {
        var samples = new ArrayList<Sample>();
        new CurveSamples(curve).forEachRemaining(samples::add);
        return samples;
    }

    private static final class CurveSamples implements Iterator<Sample> {
        private final LockOnRayCurve curve;
        private final List<Interval> pending = new ArrayList<>();

        CurveSamples(LockOnRayCurve curve) {
            this.curve = curve;
            pending.add(new Interval(0, 1, 0));
        }

        @Override public boolean hasNext() { return !pending.isEmpty(); }

        @Override public Sample next() {
            while (!pending.isEmpty()) {
                var interval = pending.remove(pending.size() - 1);
                var start = curve.position(interval.from());
                var end = curve.position(interval.to());
                double span = (interval.to() - interval.from()) / 3;
                var control1 = start.add(curve.tangent(interval.from()).scale(span));
                var control2 = end.subtract(curve.tangent(interval.to()).scale(span));
                if (interval.depth() >= MAX_DEPTH || (start.distanceTo(end) <= SEGMENT_LENGTH
                        && distanceToSegment(control1, start, end) <= CURVE_ERROR
                        && distanceToSegment(control2, start, end) <= CURVE_ERROR)) {
                    return new Sample(interval.to(), end);
                }
                double middle = (interval.from() + interval.to()) * 0.5;
                pending.add(new Interval(middle, interval.to(), interval.depth() + 1));
                pending.add(new Interval(interval.from(), middle, interval.depth() + 1));
            }
            throw new NoSuchElementException("No remaining trajectory samples");
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        double t = delta.lengthSqr() < 1.0e-12 ? 0 : Mth.clamp(point.subtract(start).dot(delta) / delta.lengthSqr(), 0, 1);
        return point.distanceTo(start.add(delta.scale(t)));
    }

    public static @Nullable Obstruction firstObstruction(Level level, Entity projectile, LockOnRayCurve curve) {
        var evaluation = new Evaluation(level, projectile, curve, new HashMap<>());
        evaluation.advance();
        return evaluation.obstruction;
    }

    private static final class Evaluation {
        private final Level level;
        private final Entity projectile;
        private final Map<BlockPos, BlockState> states;
        private final Iterator<Sample> samples;
        private Vec3 from;
        private double fraction;
        private double length;
        private @Nullable Obstruction obstruction;

        Evaluation(Level level, Entity projectile, LockOnRayCurve curve, Map<BlockPos, BlockState> states) {
            this.level = level;
            this.projectile = projectile;
            this.states = states;
            samples = new CurveSamples(curve);
            from = curve.start();
        }

        void advance() {
            walk(true);
        }

        void finishLength() {
            walk(false);
        }

        private void walk(boolean checkBlocks) {
            while (samples.hasNext()) {
                var sample = samples.next();
                var end = sample.position();
                var hit = checkBlocks ? traceSegment(level, projectile, from, end, states) : null;
                length += from.distanceTo(end);
                if (hit != null) {
                    double t = fraction + (sample.fraction() - fraction) * hit.fraction();
                    obstruction = new Obstruction(t, hit.position(), hit.hit(), hit.unloaded());
                }
                from = end;
                fraction = sample.fraction();
                if (hit != null) return;
            }
        }
    }

    private record Interval(double from, double to, int depth) {}

    public static @Nullable Obstruction traceSegment(Level level, Entity projectile, Vec3 from, Vec3 to) {
        return traceSegment(level, projectile, from, to, new HashMap<>());
    }

    private static @Nullable Obstruction traceSegment(Level level, Entity projectile, Vec3 from, Vec3 to,
                                                      Map<BlockPos, BlockState> states) {
        // 極端な距離でも深さ上限で巨大な領域を走査したり、チャンクをロードしたりしない。
        if (from.distanceTo(to) > SEGMENT_LENGTH * 2) return new Obstruction(0, from, null, true);
        double halfWidth = projectile.getBbWidth() * 0.5;
        double height = projectile.getBbHeight();
        // 柵など、所属ブロックの外まで伸びる衝突形状も含める。
        var bounds = new AABB(from, to).inflate(halfWidth + CURVE_ERROR + 1).expandTowards(0, height, 0);
        var min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        var max = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        // getBlockState / collision shape より先に、区間が触れる全チャンクの存在を確認する。
        for (var pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return new Obstruction(0, from, null, true);
        }
        Vec3 nearest = null;
        BlockPos block = null;
        var context = new FlightCollisionContext(projectile, from.y);
        for (var pos : BlockPos.betweenClosed(min, max)) {
            var key = pos.immutable();
            var state = states.computeIfAbsent(key, level::getBlockState);
            if (state.isAir()) continue;
            var boxes = state.getCollisionShape(level, key, context).toAabbs();
            for (var box : boxes) {
                // Entity座標は足元。ブロック形状を弾の寸法で拡張し、座標の掃引へ変換する。
                var expanded = box.move(pos).move(0, -height * 0.5, 0)
                        .inflate(halfWidth + CURVE_ERROR, height * 0.5 + CURVE_ERROR, halfWidth + CURVE_ERROR);
                var point = expanded.contains(from) ? from : expanded.clip(from, to).orElse(null);
                if (point != null && (nearest == null || from.distanceToSqr(point) < from.distanceToSqr(nearest))) {
                    nearest = point;
                    block = pos.immutable();
                }
            }
        }
        if (nearest == null) return null;
        var delta = to.subtract(from);
        double fraction = delta.lengthSqr() < 1.0e-12 ? 0 : from.distanceTo(nearest) / delta.length();
        var direction = Direction.getNearest(delta.x, delta.y, delta.z).getOpposite();
        return new Obstruction(fraction, nearest, new BlockHitResult(nearest, direction, block, fraction == 0), false);
    }

    private static Vec3 unit(Vec3 vector, Vec3 fallback) {
        return vector.lengthSqr() < 1.0e-12 ? fallback : vector.normalize();
    }

    private static final class FlightCollisionContext extends EntityCollisionContext {
        FlightCollisionContext(Entity projectile, double bottom) {
            // 足場などの形状は弾の高度で変わる。候補評価でも発射位置でなく、各区間の高度を使う。
            super(projectile.isDescending(), bottom, ItemStack.EMPTY, fluid -> false, projectile);
        }
    }
}
