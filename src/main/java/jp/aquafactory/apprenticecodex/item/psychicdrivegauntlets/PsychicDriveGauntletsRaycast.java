package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PsychicDriveGauntletsRaycast {
    public static final double RANGE = 6.0;
    public static final double RADIUS = 0.5;
    private static final double EPSILON = 1.0e-7;

    public record Contact(Entity target, Vec3 point) {}
    public record Aim(Vec3 origin, Vec3 point, boolean block) {}

    private PsychicDriveGauntletsRaycast() {}

    public static Aim aim(Player owner) {
        return aim(owner, RANGE);
    }

    public static Aim aim(Player owner, double range) {
        var start = owner.getEyePosition();
        var end = start.add(owner.getLookAngle().scale(range));
        var block = owner.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        // 敵の手前で終えると、準備・飛翔中に逃げる対象へ追いつけないため、地形だけで終点を決める。
        return new Aim(start, end, block.getType() == HitResult.Type.BLOCK);
    }

    public static @Nullable Contact firstContact(Player owner, Vec3 start, Vec3 end, Vec3 visibilityOrigin) {
        Contact selected = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (var raw : owner.level().getEntities(owner, new AABB(start, end).inflate(RADIUS + EPSILON),
                entity -> !entity.isSpectator() && entity.isPickable())) {
            var target = CombatTools.resolutePartEntity(raw);
            if (!target.isAlive() || target.isRemoved() || !CombatTools.isValidCombatTarget(target, owner)) continue;
            var actual = raw.getBoundingBox();
            var point = intersection(actual.inflate(RADIUS), start, end);
            if (point == null) continue;
            // 腕が埋まっていても使えるよう、腕の経路ではなく入力時の目から現在の対象への遮蔽を調べる。
            // 膨張した箱だけが壁の手前へ出た対象も、実際の箱への視線で除外する。
            var surface = new Vec3(Mth.clamp(point.x, actual.minX, actual.maxX),
                    Mth.clamp(point.y, actual.minY, actual.maxY), Mth.clamp(point.z, actual.minZ, actual.maxZ));
            var obstruction = owner.level().clip(new ClipContext(visibilityOrigin, surface,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (obstruction.getType() != HitResult.Type.MISS
                    && visibilityOrigin.distanceToSqr(obstruction.getLocation()) + EPSILON
                    < visibilityOrigin.distanceToSqr(surface)) continue;
            var distance = start.distanceToSqr(point);
            if (distance < nearest || (distance == nearest && selected != null
                    && target.getId() < selected.target().getId())) {
                selected = new Contact(target, point);
                nearest = distance;
            }
        }
        return selected;
    }

    public static @Nullable Vec3 intersection(AABB box, Vec3 start, Vec3 end) {
        if (containsInclusive(box, start)) return start;
        var point = box.clip(start, end);
        return point.orElseGet(() -> containsInclusive(box, end) ? end : null);
        // AABB.clipが除外する終端ちょうどの接触も、射程内として扱う。
    }

    public static @Nullable Entity drillTarget(Player owner, Vec3 point, @Nullable Entity previous) {
        Entity selected = null;
        double nearest = Double.POSITIVE_INFINITY;
        var area = new AABB(point, point).inflate(RADIUS + EPSILON);
        for (var raw : owner.level().getEntities(owner, area,
                entity -> !entity.isSpectator() && entity.isPickable())) {
            var target = CombatTools.resolutePartEntity(raw);
            var box = raw.getBoundingBox();
            if (!target.isAlive() || target.isRemoved() || !CombatTools.isValidCombatTarget(target, owner)
                    || !containsInclusive(box.inflate(RADIUS), point)) continue;
            // 同じ敵を優先するが、その敵が離脱していれば拳の近傍だけで次の対象を選ぶ。
            if (target == previous) return target;
            var surface = new Vec3(Mth.clamp(point.x, box.minX, box.maxX),
                    Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
            double distance = point.distanceToSqr(surface);
            if (distance < nearest || (distance == nearest && selected != null && target.getId() < selected.getId())) {
                selected = target;
                nearest = distance;
            }
        }
        return selected;
    }

    private static boolean containsInclusive(AABB box, Vec3 point) {
        // 交点や飛翔の補間座標は、表面での丸めによって箱の外へ出る場合がある。
        // 広域検索・AABB.clipと同じ微小な許容差を終端と継続接触にも適用し、静止対象を取りこぼさない。
        return point.x >= box.minX - EPSILON && point.x <= box.maxX + EPSILON
                && point.y >= box.minY - EPSILON && point.y <= box.maxY + EPSILON
                && point.z >= box.minZ - EPSILON && point.z <= box.maxZ + EPSILON;
    }
}
