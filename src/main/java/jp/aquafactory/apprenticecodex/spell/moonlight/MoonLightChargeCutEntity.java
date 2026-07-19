package jp.aquafactory.apprenticecodex.spell.moonlight;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerResolver;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidHolder;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MoonLightChargeCutEntity extends Entity implements TraceableEntity, CombatOwnerUuidHolder {
    public static final int PROCESS_START_DELAY_TICKS = 2;
    public static final int PROCESS_DURATION_TICKS = 15;
    public static final float START_OFFSET_BLOCKS = 2.0f;
    public static final float SURFACE_OFFSET_BLOCKS = 0.02f;
    public static final float DAMAGE_WIDTH_BLOCKS = 0.9f;
    public static final float DAMAGE_HALF_WIDTH_BLOCKS = DAMAGE_WIDTH_BLOCKS * 0.5f;
    public static final float AREA_HEIGHT_BLOCKS = 6.0f;
    public static final float VISUAL_NEAR_WIDTH_BLOCKS = 0.16f;
    public static final float VISUAL_NEAR_HALF_WIDTH_BLOCKS = VISUAL_NEAR_WIDTH_BLOCKS * 0.5f;
    public static final float VISUAL_FAR_WIDTH_BLOCKS = 0.62f;
    public static final float VISUAL_FAR_HALF_WIDTH_BLOCKS = VISUAL_FAR_WIDTH_BLOCKS * 0.5f;
    public static final float V_NOTCH_ANGLE_DEGREES = 30.0f;
    public static final float MIN_NOTCH_DEPTH = 0.05f;
    public static final float MAX_NOTCH_DEPTH = 4.0f;
    private static final int PORTAL_PARTICLE_COUNT_PER_EMITTER = 2;
    private static final int PORTAL_VERTICAL_EMITTER_COUNT = 7;
    private static final float PORTAL_EMITTER_SPAWN_CHANCE = 0.7f;
    private static final float SEGMENT_MARGIN_BLOCKS = 0.05f;

    private static final EntityDataAccessor<Float> DISTANCE_BLOCKS =
            SynchedEntityData.defineId(MoonLightChargeCutEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PROCESSED_DISTANCE =
            SynchedEntityData.defineId(MoonLightChargeCutEntity.class, EntityDataSerializers.FLOAT);

    private Entity owner;
    private UUID combatOwnerUuid;
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
        combatOwnerUuid = CombatOwnerResolver.captureCombatOwnerUuid(owner);
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

        if (tickCount == PROCESS_START_DELAY_TICKS + 1){
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.MOON_LIGHT_DIMENSION.get(), SoundSource.PLAYERS);
        }

        var elapsedTicks = tickCount - PROCESS_START_DELAY_TICKS;
        var normalizedProgress = Mth.clamp(
                elapsedTicks / (float) Math.max(1, PROCESS_DURATION_TICKS),
                0.0f,
                1.0f
        );
        var easedProgress = easeOutCubic(normalizedProgress);
        var currentDistance = getProcessedDistance();
        var nextDistance = Math.min(maxDistance, maxDistance * easedProgress);
        if (nextDistance > currentDistance) {
            applyDamageAlongSegment(level, livingOwner, currentDistance, nextDistance);
            setProcessedDistance(nextDistance);
        }

        if (normalizedProgress >= 1.0f || nextDistance >= maxDistance) {
            discard();
        }
    }

    private static float easeOutCubic(float value) {
        var t = Mth.clamp(value, 0.0f, 1.0f);
        var inverse = 1.0f - t;
        return 1.0f - inverse * inverse * inverse;
    }

    private void applyDamageAlongSegment(Level level, LivingEntity owner, float segmentStart, float segmentEnd) {
        if (segmentEnd <= segmentStart) {
            return;
        }

        var startPos = position();
        var forward = getForwardDirection();
        var right = calculateRightDirection(forward);
        var up = calculateUpDirection(forward, right);

        var source = CombatOwnerResolver.createDamageSourcePreservingCurrentOwner(
                level,
                this,
                owner,
                combatOwnerUuid,
                DamageTypes.MOON_LIGHT
        );
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
            if (!isInsideSegment(target.getBoundingBox(), startPos, forward, right, up, segmentStart, segmentEnd)) {
                continue;
            }

            damagedEntityIds.add(target.getUUID());
            CombatTools.applyDamage(target, damage, source, school, CombatTools.KnockbackTypes.DEFAULT);
        }
    }

    private boolean isInsideSegment(AABB box, Vec3 startPos, Vec3 forward, Vec3 right, Vec3 up, double segmentStart, double segmentEnd) {
        var forwardProjection = projectAabbToAxis(box, forward);
        var rightProjection = projectAabbToAxis(box, right);
        var upProjection = projectAabbToAxis(box, up);
        var startForwardProjection = startPos.dot(forward);
        var startRightProjection = startPos.dot(right);
        var startUpProjection = startPos.dot(up);
        var minForward = forwardProjection.min() - startForwardProjection;
        var maxForward = forwardProjection.max() - startForwardProjection;
        var minRight = rightProjection.min() - startRightProjection;
        var maxRight = rightProjection.max() - startRightProjection;
        var minUp = upProjection.min() - startUpProjection;
        var maxUp = upProjection.max() - startUpProjection;

        var expandedStart = segmentStart - SEGMENT_MARGIN_BLOCKS;
        var expandedEnd = segmentEnd + SEGMENT_MARGIN_BLOCKS;
        if (maxForward < expandedStart || minForward > expandedEnd) {
            return false;
        }
        if (maxUp < -SEGMENT_MARGIN_BLOCKS || minUp > AREA_HEIGHT_BLOCKS + SEGMENT_MARGIN_BLOCKS) {
            return false;
        }

        return maxRight >= -DAMAGE_HALF_WIDTH_BLOCKS && minRight <= DAMAGE_HALF_WIDTH_BLOCKS;
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
        var direction = Vec3.directionFromRotation(getXRot(), getYRot());
        if (direction.lengthSqr() < 1.0e-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return direction.normalize();
    }

    private static Vec3 calculateRightDirection(Vec3 forward) {
        // forward と worldUp が平行に近いケースで不安定化するためフォールバックを持つ。
        var right = new Vec3(0.0, 1.0, 0.0).cross(forward);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        return right.normalize();
    }

    private static Vec3 calculateUpDirection(Vec3 forward, Vec3 right) {
        var up = forward.cross(right);
        if (up.lengthSqr() < 1.0e-6) {
            up = new Vec3(0.0, 1.0, 0.0);
        }
        return up.normalize();
    }

    private void spawnPortalParticlesClient(Level level) {
        var forward = getForwardDirection();
        var right = calculateRightDirection(forward);
        var up = calculateUpDirection(forward, right);

        var frontCenter = position().add(forward.scale(getProcessedDistance()));
        var notchDepth = getNotchDepth();
        var frontDepthOffset = forward.scale(-notchDepth);
        var leftOffset = right.scale(-VISUAL_FAR_HALF_WIDTH_BLOCKS);
        var rightOffset = right.scale(VISUAL_FAR_HALF_WIDTH_BLOCKS);

        var bottomLeft = frontCenter.add(frontDepthOffset).add(leftOffset);
        var bottomRight = frontCenter.add(frontDepthOffset).add(rightOffset);
        var random = level.getRandom();
        for (var i = 0; i < PORTAL_VERTICAL_EMITTER_COUNT; ++i) {
            // 高さを帯ごとに乱し、左右も独立して間引くことで、切断面の輪郭を保ちながら等間隔の列を崩す。
            if (random.nextFloat() < PORTAL_EMITTER_SPAWN_CHANCE) {
                var leftRatio = (i + random.nextDouble()) / PORTAL_VERTICAL_EMITTER_COUNT;
                var leftVerticalOffset = up.scale(AREA_HEIGHT_BLOCKS * leftRatio);
                var leftVerticalBias = Mth.lerp((float) leftRatio, -0.015f, 0.015f);
                spawnPortalEmitter(level, bottomLeft.add(leftVerticalOffset), forward, 0.025, leftVerticalBias);
            }
            if (random.nextFloat() < PORTAL_EMITTER_SPAWN_CHANCE) {
                var rightRatio = (i + random.nextDouble()) / PORTAL_VERTICAL_EMITTER_COUNT;
                var rightVerticalOffset = up.scale(AREA_HEIGHT_BLOCKS * rightRatio);
                var rightVerticalBias = Mth.lerp((float) rightRatio, -0.015f, 0.015f);
                spawnPortalEmitter(level, bottomRight.add(rightVerticalOffset), forward, 0.025, rightVerticalBias);
            }
        }
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
        return Mth.clamp(VISUAL_FAR_HALF_WIDTH_BLOCKS / tanHalf, MIN_NOTCH_DEPTH, MAX_NOTCH_DEPTH);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        damage = pCompound.getFloat("Damage");
        loadCombatOwnerUuid(pCompound);
        entityData.set(DISTANCE_BLOCKS, pCompound.getFloat("DistanceBlocks"));
        var processedDistance = pCompound.getFloat("ProcessedDistance");
        entityData.set(PROCESSED_DISTANCE, processedDistance);
        previousProcessedDistance = processedDistance;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putFloat("Damage", damage);
        saveCombatOwnerUuid(pCompound);
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
        var right = calculateRightDirection(direction);
        var up = calculateUpDirection(direction, right);
        var start = position();
        var horizontalHalfWidth = Math.max(VISUAL_FAR_HALF_WIDTH_BLOCKS, DAMAGE_HALF_WIDTH_BLOCKS);
        return createOrientedBounds(
                start,
                direction,
                right,
                up,
                getDistanceBlocks(),
                horizontalHalfWidth,
                AREA_HEIGHT_BLOCKS
        ).inflate(0.2);
    }

    private static AABB createOrientedBounds(Vec3 start, Vec3 forward, Vec3 right, Vec3 up,
                                             float distance, float halfWidth, float height) {
        var minX = Double.POSITIVE_INFINITY;
        var minY = Double.POSITIVE_INFINITY;
        var minZ = Double.POSITIVE_INFINITY;
        var maxX = Double.NEGATIVE_INFINITY;
        var maxY = Double.NEGATIVE_INFINITY;
        var maxZ = Double.NEGATIVE_INFINITY;

        for (var forwardLength : new double[]{0.0, distance}) {
            for (var side : new double[]{-halfWidth, halfWidth}) {
                for (var vertical : new double[]{0.0, height}) {
                    var point = start
                            .add(forward.scale(forwardLength))
                            .add(right.scale(side))
                            .add(up.scale(vertical));
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    minZ = Math.min(minZ, point.z);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                    maxZ = Math.max(maxZ, point.z);
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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

    @Override
    public @Nullable UUID getCombatOwnerUuid() {
        return combatOwnerUuid;
    }

    @Override
    public void setCombatOwnerUuid(@Nullable UUID combatOwnerUuid) {
        this.combatOwnerUuid = combatOwnerUuid;
    }

    private record ProjectionRange(double min, double max) {}
}
