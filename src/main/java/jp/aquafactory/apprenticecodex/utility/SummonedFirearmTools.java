package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Predicate;

public final class SummonedFirearmTools {
    private static final double AIM_SEARCH_WIDTH = 0.875D;
    private static final double AIM_ASSIST_INFLATE = 0.35D;

    private SummonedFirearmTools() {
    }

    public static RaycastTools.TargetResult resolveAssistedAim(LivingEntity caster, double range, Predicate<Entity> predicate) {
        var level = caster.level();
        var look = caster.getViewVector(1.0F);
        var start = caster.getEyePosition(1.0F);
        var end = start.add(look.scale(range));

        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));

        var effectiveEnd = blockHit.getType() == HitResult.Type.MISS
                ? end
                : blockHit.getLocation();
        var searchBox = caster.getBoundingBox()
                .expandTowards(look.scale(range))
                .inflate(AIM_SEARCH_WIDTH / 2);

        var entityHit = resolveEntityHit(level, caster, start, effectiveEnd, searchBox, predicate);
        if (entityHit.isPresent()) {
            var hit = entityHit.get();
            return new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY,
                    hit.hitPosition(),
                    hit.entity(),
                    null
            );
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            return new RaycastTools.TargetResult(
                    RaycastTools.TargetType.BLOCK,
                    blockHit.getLocation(),
                    null,
                    blockHit.getBlockPos()
            );
        }

        return new RaycastTools.TargetResult(RaycastTools.TargetType.NONE, end, null, null);
    }

    public static boolean isHeadShot(RaycastTools.TargetResult result) {
        return result.hitEntity() instanceof LivingEntity living
                && CombatTools.isHeadShot(living, result.hitPosition());
    }

    public static boolean shouldApplyUnawareBonus(Entity target, LivingEntity owner) {
        if (target instanceof Mob mob && mob.getTarget() == owner) {
            return false;
        }

        if (target instanceof NeutralMob neutral) {
            var angerTarget = neutral.getPersistentAngerTarget();
            if (angerTarget != null && angerTarget.equals(owner.getUUID())) {
                return false;
            }

            return !neutral.isAngryAt(owner);
        }

        return true;
    }

    public static void suppressNearbyAwareness(Level level, LivingEntity owner, Entity center, double radius) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        var ownerId = owner.getUUID();
        var searchBox = center.getBoundingBox().inflate(radius);
        for (var mob : level.getEntitiesOfClass(Mob.class, searchBox, mob -> mob.isAlive() && mob != center)) {
            if (mob.getTarget() == owner) {
                mob.setTarget(null);
            }

            if (mob.getLastHurtByMob() == owner) {
                mob.setLastHurtByMob(null);
            }

            if (mob instanceof NeutralMob neutral) {
                var angerTarget = neutral.getPersistentAngerTarget();
                if (angerTarget != null && angerTarget.equals(ownerId)) {
                    neutral.stopBeingAngry();
                }
            }
        }
    }

    private static Optional<EntityAimHit> resolveEntityHit(
            Level level,
            LivingEntity caster,
            Vec3 start,
            Vec3 end,
            AABB searchBox,
            Predicate<Entity> predicate
    ) {
        EntityAimHit closest = null;
        var closestDistanceSqr = Double.MAX_VALUE;

        for (var candidate : level.getEntities(caster, searchBox, e -> e.isAlive() && predicate.test(e))) {
            // ProjectileUtil は命中座標がエンティティ基準位置へ落ちるため、視線と少し太らせたAABBの交点を使う。
            var hitPosition = candidate.getBoundingBox().inflate(AIM_ASSIST_INFLATE).clip(start, end);
            if (hitPosition.isEmpty()) {
                continue;
            }

            var distanceSqr = start.distanceToSqr(hitPosition.get());
            if (distanceSqr < closestDistanceSqr) {
                closest = new EntityAimHit(candidate, hitPosition.get());
                closestDistanceSqr = distanceSqr;
            }
        }

        return Optional.ofNullable(closest);
    }

    private record EntityAimHit(Entity entity, Vec3 hitPosition) {
    }
}
