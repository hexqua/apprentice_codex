package jp.aquafactory.apprenticecodex.common.utility;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
            TargetType type,
            Vec3 hitPosition
    ) {}

    public static TargetResult raycastFromEye(LivingEntity source, double range) {
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
                e -> (e instanceof LivingEntity le)
                        && le.isPickable()
                        && le != source
        );

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity le) {
            var center = le.getBoundingBox().getCenter();
            return new TargetResult(TargetType.LIVING_ENTITY, center);
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            return new TargetResult(TargetType.BLOCK, blockHit.getLocation());
        }

        return new TargetResult(TargetType.NONE, end);
    }
}
