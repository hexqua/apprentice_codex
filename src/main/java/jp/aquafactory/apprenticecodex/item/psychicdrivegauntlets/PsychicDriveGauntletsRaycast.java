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
    public record Aim(@Nullable Entity target, Vec3 point) {}

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
        var contact = firstContact(owner, start, end, true);
        return contact == null ? new Aim(null, end) : new Aim(contact.target(), contact.point());
    }

    public static @Nullable Contact firstContact(Player owner, Vec3 start, Vec3 end, boolean checkVisibility) {
        Contact selected = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (var raw : owner.level().getEntities(owner, new AABB(start, end).inflate(RADIUS + EPSILON),
                entity -> !entity.isSpectator() && entity.isPickable())) {
            var target = CombatTools.resolutePartEntity(raw);
            if (!target.isAlive() || target.isRemoved() || !CombatTools.isValidCombatTarget(target, owner)) continue;
            var actual = raw.getBoundingBox();
            var point = intersection(actual.inflate(RADIUS), start, end);
            if (point == null) continue;
            if (checkVisibility) {
                // 膨張した箱が壁の手前へ出ても、照準では壁裏の対象を拾わない。
                var surface = new Vec3(Mth.clamp(point.x, actual.minX, actual.maxX),
                        Mth.clamp(point.y, actual.minY, actual.maxY), Mth.clamp(point.z, actual.minZ, actual.maxZ));
                var obstruction = owner.level().clip(new ClipContext(start, surface,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
                if (obstruction.getType() != HitResult.Type.MISS
                        && start.distanceToSqr(obstruction.getLocation()) + EPSILON < start.distanceToSqr(surface)) continue;
            }
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

    private static boolean containsInclusive(AABB box, Vec3 point) {
        return point.x >= box.minX && point.x <= box.maxX && point.y >= box.minY
                && point.y <= box.maxY && point.z >= box.minZ && point.z <= box.maxZ;
    }
}
