package jp.aquafactory.apprenticecodex.common.spells.arcanebeam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

// todo:ビームエンティティは絶対汎用性があるので実装が完了したら抽象化して切り出す.
public class ArcaneBeamEntity extends Entity implements TraceableEntity {

    private static final EntityDataAccessor<Integer> COLOR_ARGB_OUTER =
            SynchedEntityData.defineId(ArcaneBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_ARGB_INNER =
            SynchedEntityData.defineId(ArcaneBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(ArcaneBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(ArcaneBeamEntity.class, EntityDataSerializers.FLOAT);

    // 永続化はしないからUUIDは持たない.
    private Entity owner;

    public ArcaneBeamEntity(EntityType<? extends ArcaneBeamEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Entity getOwner() {
        return owner;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }


    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR_ARGB_OUTER, 0xAA33D6FF);
        entityData.define(COLOR_ARGB_INNER, 0xFFFFFFFF);
        entityData.define(LENGTH, 1.0f);
        entityData.define(RADIUS, 1.0f);
    }

    @Override
    public void tick() {
        @SuppressWarnings("resource") var level = level();

        super.tick();

        // todo:ビームを追従させる、ただし追従はアーケインビームだけかもしれないので切り出さないかも.
        if(level.isClientSide){
            return;
        }

        if (getOwner() == null) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        // 永続化はしない.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        // 永続化はしない.
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 64 * 64;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        float length = getLength();
        float radius = getRadius();

        var dir = Vec3.directionFromRotation(getXRot(), getYRot()).normalize();
        var start = position();
        var end = start.add(dir.scale(length));

        // AABBはビーム区間を包含し、太さ分inflate.
        var inflate = Math.max(0.5, radius * 2.0);
        return new AABB(start, end).inflate(inflate);
    }

    public void setup(int outerArgb, int innerArgb, float length, float radius) {
        this.entityData.set(COLOR_ARGB_OUTER, outerArgb);
        this.entityData.set(COLOR_ARGB_INNER, innerArgb);
        this.entityData.set(LENGTH, length);
        this.entityData.set(RADIUS, radius);
    }

    public int getColorARGBOuter() {
        return entityData.get(COLOR_ARGB_OUTER);
    }

    public int getColorARGBInner() {
        return entityData.get(COLOR_ARGB_INNER);
    }

    public float getLength() {
        return entityData.get(LENGTH);
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }

}
