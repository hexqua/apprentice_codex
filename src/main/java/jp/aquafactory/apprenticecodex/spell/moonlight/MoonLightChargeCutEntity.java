package jp.aquafactory.apprenticecodex.spell.moonlight;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MoonLightChargeCutEntity extends Entity implements TraceableEntity {
    public static final int PROCESS_START_DELAY_TICKS = 10;
    public static final int PROCESS_DURATION_TICKS = 20;
    public static final float START_OFFSET_BLOCKS = 2.0f;
    public static final float AREA_WIDTH_BLOCKS = 0.5f;
    public static final float AREA_HEIGHT_BLOCKS = 2.0f;
    public static final float AREA_HALF_WIDTH_BLOCKS = AREA_WIDTH_BLOCKS * 0.5f;
    public static final float V_NOTCH_ANGLE_DEGREES = 30.0f;
    public static final float MIN_NOTCH_DEPTH = 0.05f;
    public static final float MAX_NOTCH_DEPTH = 4.0f;
    private static final int PORTAL_PARTICLE_COUNT_PER_EMITTER = 2;
    private static final float SEGMENT_MARGIN_BLOCKS = 0.05f;

    private static final EntityDataAccessor<Float> DISTANCE_BLOCKS =
            SynchedEntityData.defineId(MoonLightChargeCutEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PROCESSED_DISTANCE =
            SynchedEntityData.defineId(MoonLightChargeCutEntity.class, EntityDataSerializers.FLOAT);

    private Entity owner;
    private float damage;
    private float previousProcessedDistance;
    private final Set<UUID> damagedEntityIds = new HashSet<>();

    public MoonLightChargeCutEntity(EntityType<? extends MoonLightChargeCutEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public MoonLightChargeCutEntity(EntityType<? extends MoonLightChargeCutEntity> entityType, Level level, Entity owner) {
        this(entityType, level);
        this.owner = owner;
    }

    @Override
    public Entity getOwner() {
        return owner;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DISTANCE_BLOCKS, 0.0f);
        entityData.define(PROCESSED_DISTANCE, 0.0f);
    }

    @Override
    public void tick() {
        var level = level();
        previousProcessedDistance = getProcessedDistance();
        super.tick();

        if (level.isClientSide) {
            if (isProcessingStarted()) {
                spawnPortalParticlesClient(level);
            }
            return;
        }

        if (!(owner instanceof LivingEntity livingOwner) || owner.isRemoved()) {
            discard();
            return;
        }

        var maxDistance = getDistanceBlocks();
        if (maxDistance <= 0.0f) {
            discard();
            return;
        }

        if (tickCount <= PROCESS_START_DELAY_TICKS) {
            return;
        }

        var processSpeed = maxDistance / Math.max(1, PROCESS_DURATION_TICKS);
        var currentDistance = getProcessedDistance();
        var nextDistance = Math.min(maxDistance, currentDistance + processSpeed);
        if (nextDistance > currentDistance) {
            applyDamageAlongSegment(level, livingOwner, currentDistance, nextDistance);
            setProcessedDistance(nextDistance);
        }

        if (nextDistance >= maxDistance) {
            discard();
        }
    }

    private void applyDamageAlongSegment(Level level, LivingEntity owner, float segmentStart, float segmentEnd) {
        if (segmentEnd <= segmentStart) {
            return;
        }

        var startPos = position();
        var forward = getForwardDirection();
        var right = new Vec3(-forward.z, 0.0, forward.x);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }

        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.MOON_LIGHT);
        var school = SpellRegistry.MOON_LIGHT.get().getSchoolType();
        var candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBoxForCulling(),
                target -> target != owner
                        && target.isAlive()
                        && !damagedEntityIds.contains(target.getUUID())
                        && CombatTools.isValidCombatTarget(target, owner)
        );

        for (var target : candidates) {
            if (!isInsideSegment(target.getBoundingBox(), startPos, forward, right, segmentStart, segmentEnd)) {
                continue;
            }

            damagedEntityIds.add(target.getUUID());
            CombatTools.applyDamage(target, damage, source, school, CombatTools.KnockbackTypes.DEFAULT);
        }
    }

    private boolean isInsideSegment(AABB box, Vec3 startPos, Vec3 forward, Vec3 right, double segmentStart, double segmentEnd) {
        var baseY = startPos.y;
        if (box.maxY < baseY || box.minY > baseY + AREA_HEIGHT_BLOCKS) {
            return false;
        }

        var forwardProjection = projectAabbToAxis(box, forward);
        var rightProjection = projectAabbToAxis(box, right);
        var startForwardProjection = startPos.dot(forward);
        var startRightProjection = startPos.dot(right);
        var minForward = forwardProjection.min() - startForwardProjection;
        var maxForward = forwardProjection.max() - startForwardProjection;
        var minRight = rightProjection.min() - startRightProjection;
        var maxRight = rightProjection.max() - startRightProjection;

        var expandedStart = segmentStart - SEGMENT_MARGIN_BLOCKS;
        var expandedEnd = segmentEnd + SEGMENT_MARGIN_BLOCKS;
        if (maxForward < expandedStart || minForward > expandedEnd) {
            return false;
        }

        return maxRight >= -AREA_HALF_WIDTH_BLOCKS && minRight <= AREA_HALF_WIDTH_BLOCKS;
    }

    private ProjectionRange projectAabbToAxis(AABB box, Vec3 axis) {
        var center = box.getCenter();
        var centerProjection = center.dot(axis);
        var halfX = box.getXsize() * 0.5;
        var halfY = box.getYsize() * 0.5;
        var halfZ = box.getZsize() * 0.5;
        var halfProjectionSize = Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
        return new ProjectionRange(centerProjection - halfProjectionSize, centerProjection + halfProjectionSize);
    }

    private Vec3 getForwardDirection() {
        var direction = Vec3.directionFromRotation(0.0f, getYRot());
        if (direction.lengthSqr() < 1.0e-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return direction.normalize();
    }

    private void spawnPortalParticlesClient(Level level) {
        var forward = getForwardDirection();
        var right = new Vec3(-forward.z, 0.0, forward.x);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }

        var frontCenter = position().add(forward.scale(getProcessedDistance()));
        var notchDepth = getNotchDepth();
        var frontDepthOffset = forward.scale(-notchDepth);
        var topY = position().y + AREA_HEIGHT_BLOCKS;
        var bottomY = position().y;

        var topLeft = new Vec3(
                frontCenter.x + frontDepthOffset.x - right.x * AREA_HALF_WIDTH_BLOCKS,
                topY,
                frontCenter.z + frontDepthOffset.z - right.z * AREA_HALF_WIDTH_BLOCKS
        );
        var topRight = new Vec3(
                frontCenter.x + frontDepthOffset.x + right.x * AREA_HALF_WIDTH_BLOCKS,
                topY,
                frontCenter.z + frontDepthOffset.z + right.z * AREA_HALF_WIDTH_BLOCKS
        );
        var bottomLeft = new Vec3(topLeft.x, bottomY, topLeft.z);
        var bottomRight = new Vec3(topRight.x, bottomY, topRight.z);

        spawnPortalEmitter(level, topLeft, forward, 0.025, 0.015);
        spawnPortalEmitter(level, topRight, forward, 0.025, 0.015);
        spawnPortalEmitter(level, bottomLeft, forward, 0.025, -0.015);
        spawnPortalEmitter(level, bottomRight, forward, 0.025, -0.015);
    }

    private void spawnPortalEmitter(Level level, Vec3 origin, Vec3 forward, double lateralScale, double verticalBias) {
        var random = level.getRandom();
        for (var i = 0; i < PORTAL_PARTICLE_COUNT_PER_EMITTER; ++i) {
            var jitterX = (random.nextDouble() - 0.5) * 0.05;
            var jitterY = (random.nextDouble() - 0.5) * 0.05;
            var jitterZ = (random.nextDouble() - 0.5) * 0.05;
            var vx = -forward.x * (0.02 + random.nextDouble() * 0.02) + (random.nextDouble() - 0.5) * lateralScale;
            var vy = verticalBias + (random.nextDouble() - 0.5) * 0.02;
            var vz = -forward.z * (0.02 + random.nextDouble() * 0.02) + (random.nextDouble() - 0.5) * lateralScale;
            level.addParticle(
                    ParticleTypes.PORTAL,
                    origin.x + jitterX,
                    origin.y + jitterY,
                    origin.z + jitterZ,
                    vx,
                    vy,
                    vz
            );
        }
    }

    private float getNotchDepth() {
        var halfAngleRad = (V_NOTCH_ANGLE_DEGREES * 0.5f) * Mth.DEG_TO_RAD;
        var tanHalf = (float) Math.tan(halfAngleRad);
        if (tanHalf <= 1.0e-4f) {
            return MIN_NOTCH_DEPTH;
        }
        return Mth.clamp(AREA_HALF_WIDTH_BLOCKS / tanHalf, MIN_NOTCH_DEPTH, MAX_NOTCH_DEPTH);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        damage = pCompound.getFloat("Damage");
        entityData.set(DISTANCE_BLOCKS, pCompound.getFloat("DistanceBlocks"));
        var processedDistance = pCompound.getFloat("ProcessedDistance");
        entityData.set(PROCESSED_DISTANCE, processedDistance);
        previousProcessedDistance = processedDistance;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("DistanceBlocks", getDistanceBlocks());
        pCompound.putFloat("ProcessedDistance", getProcessedDistance());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 128 * 128;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var direction = getForwardDirection();
        var start = position();
        var end = start.add(direction.scale(getDistanceBlocks()));
        var horizontalBounds = new AABB(start, end).inflate(AREA_HALF_WIDTH_BLOCKS + 0.2, 0.0, AREA_HALF_WIDTH_BLOCKS + 0.2);
        return new AABB(
                horizontalBounds.minX,
                start.y,
                horizontalBounds.minZ,
                horizontalBounds.maxX,
                start.y + AREA_HEIGHT_BLOCKS,
                horizontalBounds.maxZ
        );
    }

    public void setup(float distanceBlocks, float damage) {
        this.damage = Math.max(0.0f, damage);
        setDistanceBlocks(distanceBlocks);
        setProcessedDistance(0.0f);
        previousProcessedDistance = 0.0f;
    }

    public boolean isProcessingStarted() {
        return tickCount > PROCESS_START_DELAY_TICKS;
    }

    public float getProcessedDistanceForRender(float partialTick) {
        return Mth.lerp(partialTick, previousProcessedDistance, getProcessedDistance());
    }

    public float getDistanceBlocks() {
        return entityData.get(DISTANCE_BLOCKS);
    }

    public float getProcessedDistance() {
        return entityData.get(PROCESSED_DISTANCE);
    }

    private void setDistanceBlocks(float distanceBlocks) {
        entityData.set(DISTANCE_BLOCKS, Math.max(0.0f, distanceBlocks));
    }

    private void setProcessedDistance(float processedDistance) {
        entityData.set(PROCESSED_DISTANCE, Mth.clamp(processedDistance, 0.0f, getDistanceBlocks()));
    }

    private record ProjectionRange(double min, double max) {}
}
