package jp.aquafactory.apprenticecodex.spell.catchflame;

import jp.aquafactory.apprenticecodex.spell.shock.ShockImpactParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class CatchFlameImpactEntity extends Entity {
    private static final EntityDataAccessor<Float> INCOMING_X =
            SynchedEntityData.defineId(CatchFlameImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INCOMING_Y =
            SynchedEntityData.defineId(CatchFlameImpactEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> INCOMING_Z =
            SynchedEntityData.defineId(CatchFlameImpactEntity.class, EntityDataSerializers.FLOAT);

    private boolean particlesSpawned;

    public CatchFlameImpactEntity(EntityType<? extends CatchFlameImpactEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(INCOMING_X, 0.0F);
        builder.define(INCOMING_Y, 0.0F);
        builder.define(INCOMING_Z, 1.0F);
    }

    public void setup(Vec3 incoming) {
        var normalized = incoming.lengthSqr() > 1.0E-8D ? incoming.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        entityData.set(INCOMING_X, (float) normalized.x);
        entityData.set(INCOMING_Y, (float) normalized.y);
        entityData.set(INCOMING_Z, (float) normalized.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !particlesSpawned) {
            particlesSpawned = true;
            ShockImpactParticles.spawn(level(), position(), getIncoming(), ShockImpactParticles.Palette.FIRE);
        }
        if (!level().isClientSide && tickCount >= 2) {
            discard();
        }
    }

    private Vec3 getIncoming() {
        return new Vec3(entityData.get(INCOMING_X), entityData.get(INCOMING_Y), entityData.get(INCOMING_Z));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
