package jp.aquafactory.apprenticecodex.spell.shock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ShockBoltEntity extends Entity {
    private static final float MAX_JITTER_RADIUS = 1.5f;

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
