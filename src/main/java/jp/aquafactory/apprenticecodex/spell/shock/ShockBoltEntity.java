package jp.aquafactory.apprenticecodex.spell.shock;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ShockBoltEntity extends Entity {
    private static final float MAX_JITTER_RADIUS = 1.5f;
    private static final int IMPACT_RHOMBUS_COUNT = 3;
    private static final int IMPACT_SPARK_COUNT = 9;
    private static final float IMPACT_RHOMBUS_SIZE_MIN = 0.16f;
    private static final float IMPACT_RHOMBUS_SIZE_MAX = 0.24f;
    private static final float IMPACT_SPARK_SIZE_MIN = 0.07f;
    private static final float IMPACT_SPARK_SIZE_MAX = 0.12f;
    private static final float IMPACT_RED = 0.42f;
    private static final float IMPACT_GREEN = 0.86f;
    private static final float IMPACT_BLUE = 1.0f;
    private static final int IMPACT_RHOMBUS_LIFETIME = 6;
    private static final int IMPACT_RHOMBUS_LIFETIME_VARIANCE = 2;
    private static final int IMPACT_SPARK_LIFETIME = 7;
    private static final int IMPACT_SPARK_LIFETIME_VARIANCE = 3;

    private static final EntityDataAccessor<Float> END_X =
            SynchedEntityData.defineId(ShockBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y =
            SynchedEntityData.defineId(ShockBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z =
            SynchedEntityData.defineId(ShockBoltEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE_TICKS =
            SynchedEntityData.defineId(ShockBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PATH_SEED =
            SynchedEntityData.defineId(ShockBoltEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAS_IMPACT =
            SynchedEntityData.defineId(ShockBoltEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean impactParticlesSpawned;

    public ShockBoltEntity(EntityType<? extends ShockBoltEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(END_X, 0.0f);
        entityData.define(END_Y, 0.0f);
        entityData.define(END_Z, 0.0f);
        entityData.define(LIFE_TICKS, 6);
        entityData.define(PATH_SEED, 0);
        entityData.define(HAS_IMPACT, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide && hasImpact() && !impactParticlesSpawned) {
            impactParticlesSpawned = true;
            spawnImpactParticles();
        }

        if (tickCount >= getLifeTicks()) {
            discard();
        }
    }

    public void setup(Vec3 endPosition, int lifeTicks, int pathSeed, boolean hasImpact) {
        entityData.set(END_X, (float) endPosition.x);
        entityData.set(END_Y, (float) endPosition.y);
        entityData.set(END_Z, (float) endPosition.z);
        entityData.set(LIFE_TICKS, lifeTicks);
        entityData.set(PATH_SEED, pathSeed);
        entityData.set(HAS_IMPACT, hasImpact);
    }

    public Vec3 getEndPosition() {
        return new Vec3(entityData.get(END_X), entityData.get(END_Y), entityData.get(END_Z));
    }

    public int getLifeTicks() {
        return entityData.get(LIFE_TICKS);
    }

    public int getPathSeed() {
        return entityData.get(PATH_SEED);
    }

    public boolean hasImpact() {
        return entityData.get(HAS_IMPACT);
    }

    public float getLifeProgress(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / Math.max(1.0f, getLifeTicks()), 0.0f, 1.0f);
    }

    private void spawnImpactParticles() {
        var impactPosition = getEndPosition();
        var incoming = impactPosition.subtract(position());
        if (incoming.lengthSqr() <= 1.0e-8) {
            return;
        }

        var forward = incoming.normalize();
        var backward = forward.scale(-1.0);
        var right = computeRightVector(forward);
        var up = forward.cross(right).normalize();
        var random = level().random;

        for (var i = 0; i < IMPACT_RHOMBUS_COUNT; ++i) {
            spawnImpactRhombus(random, impactPosition, backward, right, up);
        }
        for (var i = 0; i < IMPACT_SPARK_COUNT; ++i) {
            spawnImpactSpark(random, impactPosition, backward, right, up);
        }
    }

    private void spawnImpactRhombus(RandomSource random, Vec3 impactPosition, Vec3 backward, Vec3 right, Vec3 up) {
        var offset = createImpactOffset(random, backward, right, up, 0.12, 0.02, 0.045);
        var velocity = backward.scale(0.025 + random.nextDouble() * 0.035)
                .add(right.scale((random.nextDouble() - 0.5) * 0.12))
                .add(up.scale((random.nextDouble() - 0.5) * 0.12));
        var size = Mth.lerp(random.nextFloat(), IMPACT_RHOMBUS_SIZE_MIN, IMPACT_RHOMBUS_SIZE_MAX);

        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                        size,
                        IMPACT_RED,
                        IMPACT_GREEN,
                        IMPACT_BLUE,
                        2,
                        IMPACT_RHOMBUS_LIFETIME,
                        IMPACT_RHOMBUS_LIFETIME_VARIANCE,
                        0.9f,
                        1.15f,
                        0.86f,
                        1.0f,
                        0.02f,
                        0.4f,
                        0.52f,
                        false
                ),
                impactPosition.x + offset.x,
                impactPosition.y + offset.y,
                impactPosition.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private void spawnImpactSpark(RandomSource random, Vec3 impactPosition, Vec3 backward, Vec3 right, Vec3 up) {
        var offset = createImpactOffset(random, backward, right, up, 0.1, 0.025, 0.05);
        var velocity = backward.scale(0.07 + random.nextDouble() * 0.11)
                .add(right.scale((random.nextDouble() - 0.5) * 0.24))
                .add(up.scale((random.nextDouble() - 0.5) * 0.24));
        var size = Mth.lerp(random.nextFloat(), IMPACT_SPARK_SIZE_MIN, IMPACT_SPARK_SIZE_MAX);

        level().addParticle(
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_SPARK.get(),
                        size,
                        IMPACT_RED,
                        IMPACT_GREEN,
                        IMPACT_BLUE,
                        3,
                        IMPACT_SPARK_LIFETIME,
                        IMPACT_SPARK_LIFETIME_VARIANCE,
                        0.9f,
                        1.2f,
                        0.88f,
                        1.0f,
                        0.02f,
                        0.55f,
                        0.58f,
                        true
                ),
                impactPosition.x + offset.x,
                impactPosition.y + offset.y,
                impactPosition.z + offset.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private static Vec3 createImpactOffset(RandomSource random, Vec3 backward, Vec3 right, Vec3 up,
                                           double spread, double minBackward, double maxBackward) {
        return backward.scale(Mth.lerp(random.nextDouble(), minBackward, maxBackward))
                .add(right.scale((random.nextDouble() - 0.5) * spread))
                .add(up.scale((random.nextDouble() - 0.5) * spread));
    }

    private static Vec3 computeRightVector(Vec3 forward) {
        var right = new Vec3(0.0, 1.0, 0.0).cross(forward);
        if (right.lengthSqr() <= 1.0e-8) {
            right = new Vec3(1.0, 0.0, 0.0).cross(forward);
        }
        return right.normalize();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        entityData.set(END_X, tag.getFloat("EndX"));
        entityData.set(END_Y, tag.getFloat("EndY"));
        entityData.set(END_Z, tag.getFloat("EndZ"));
        entityData.set(LIFE_TICKS, tag.getInt("LifeTicks"));
        entityData.set(PATH_SEED, tag.getInt("PathSeed"));
        entityData.set(HAS_IMPACT, tag.getBoolean("HasImpact"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putFloat("EndX", entityData.get(END_X));
        tag.putFloat("EndY", entityData.get(END_Y));
        tag.putFloat("EndZ", entityData.get(END_Z));
        tag.putInt("LifeTicks", getLifeTicks());
        tag.putInt("PathSeed", getPathSeed());
        tag.putBoolean("HasImpact", hasImpact());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return new AABB(position(), getEndPosition()).inflate(MAX_JITTER_RADIUS + 0.5);
    }
}
