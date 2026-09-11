package jp.aquafactory.apprenticecodex.spell.lightningarrow;

import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;

public final class LightningArrowCollision {
    private static final double RADIUS = 1.0;
    private static final double EPSILON = 1.0e-7;

    private LightningArrowCollision() {
    }

    public record Contact(Entity target, Vec3 position, double distance) {
    }

    public static List<Contact> contacts(Level level, Entity projectile, Entity owner, Vec3 start, Vec3 end) {
        var result = new ArrayList<Contact>();
        var movement = end.subtract(start);
        var length = movement.length();
        if (length < EPSILON) return result;
        var direction = movement.scale(1.0 / length);
        for (var raw : level.getEntities(projectile, new AABB(start, end).inflate(RADIUS), Entity::isAlive)) {
            var target = CombatTools.resolutePartEntity(raw);
            if (!CombatTools.isValidCombatTarget(target, owner)) continue;
            var box = raw.getBoundingBox();
            // 膨張AABBの端帽が射程・壁の奥まで届くことを、実部位の投影範囲で防ぐ。
            var centerDistance = box.getCenter().subtract(start).dot(direction);
            var projectionRadius = (Math.abs(direction.x) * box.getXsize()
                    + Math.abs(direction.y) * box.getYsize() + Math.abs(direction.z) * box.getZsize()) * 0.5;
            if (centerDistance - projectionRadius > length + EPSILON
                    || centerDistance + projectionRadius < -EPSILON) continue;
            var expanded = box.inflate(RADIUS);
            var intersection = expanded.contains(start) ? java.util.Optional.of(start) : expanded.clip(start, end);
            if (intersection.isEmpty()) continue;
            var axisPoint = intersection.get();
            var point = new Vec3(Mth.clamp(axisPoint.x, box.minX, box.maxX),
                    Mth.clamp(axisPoint.y, box.minY, box.maxY), Mth.clamp(axisPoint.z, box.minZ, box.maxZ));
            // 中心線を遮らない側壁でも、電撃から実部位への短い線が遮られるなら命中させない。
            var cover = level.clip(new ClipContext(axisPoint, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, projectile));
            if (cover.getType() != HitResult.Type.MISS) continue;
            result.add(new Contact(target, point, start.distanceTo(axisPoint)));
        }
        result.sort(Comparator.comparingDouble(Contact::distance));
        var unique = new LinkedHashMap<UUID, Contact>();
        for (var contact : result) unique.putIfAbsent(contact.target().getUUID(), contact);
        return new ArrayList<>(unique.values());
    }
}
