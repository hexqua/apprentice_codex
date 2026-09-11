package jp.aquafactory.apprenticecodex.spell.bloodyarrow;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 血のオーブを、壁の向こうや深い穴へ散らさず、歩いて回収できる足場に配置する。 */
public final class BloodOrbPlacement {
    private BloodOrbPlacement() {}

    public static @Nullable Vec3 findAnchor(Entity caster, Vec3 impact) {
        var ground = groundBelow(caster.level(), caster, impact.add(0, 0.25, 0), 32);
        if (ground != null) return ground;
        // 高空・奈落の命中では無制限に下を探索せず、術者の足場へ戻す。
        return groundBelow(caster.level(), caster, caster.position().add(0, 1, 0), 4);
    }

    public static Vec3 destination(Entity caster, Vec3 anchor, double angle, double radius) {
        for (double distance = radius; distance >= 0.75; distance -= 0.5) {
            Vec3 previous = anchor;
            boolean reachable = true;
            // 終点だけでなく経路の足場も調べ、穴の向こうの足場を採用しない。
            for (double step = Math.min(0.5, distance); ; step = Math.min(distance, step + 0.5)) {
                var sample = anchor.add(Math.cos(angle) * step, 1.25, Math.sin(angle) * step);
                var ground = groundBelow(caster.level(), caster, sample, 2.5);
                if (ground == null || Math.abs(ground.y - previous.y) > 0.75
                        || !clearLine(caster.level(), caster, previous, ground)) {
                    reachable = false;
                    break;
                }
                previous = ground;
                if (step >= distance) break;
            }
            if (reachable) return previous;
        }
        return anchor;
    }

    public static boolean clearLine(Level level, Entity context, Vec3 start, Vec3 end) {
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, context)).getType() == HitResult.Type.MISS;
    }

    private static @Nullable Vec3 groundBelow(Level level, Entity context, Vec3 start, double depth) {
        var end = start.add(0, -depth, 0);
        if (!level.hasChunkAt(BlockPos.containing(start)) || !level.hasChunkAt(BlockPos.containing(end))) return null;
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != net.minecraft.core.Direction.UP) return null;
        var feet = hit.getLocation().add(0, 0.01, 0);
        var space = new AABB(feet.x - 0.3, feet.y, feet.z - 0.3, feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
        // 人が立てる空間と液体の有無を基準にし、オーブの小さい当たり箱だけで判断しない。
        if (!level.noCollision(context, space) || level.containsAnyLiquid(space)) return null;
        return hit.getLocation().add(0, 0.65, 0);
    }
}
