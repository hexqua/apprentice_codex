package jp.aquafactory.apprenticecodex.common.spells.arcanebeam;

import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

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
    private float damage;

    public ArcaneBeamEntity(EntityType<? extends ArcaneBeamEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ArcaneBeamEntity(EntityType<? extends ArcaneBeamEntity> entityType, Level level, Entity owner) {
        super(entityType, level);
        this.owner = owner;
    }

    @Override
    public Entity getOwner() {
        return owner;
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
        var level = level();
        super.tick();

        if(level.isClientSide){
            return;
        }

        // 追従は魔法処理側.
        if (owner == null || owner.isRemoved()) {
            discard();
            return;
        }

        // 線分のサンプルを取る都合上重いので5tickに1回にする.
        if (tickCount % 5 != 0) {
            damageHitTarget(level);
        }
    }

    private void damageHitTarget(Level level){
        var entities = RaycastTools.sampleBeamHits(
                level,
                position(),
                position().add(getLookAngle().normalize().scale(getLength())),
                getRadius(),
                0.2,
                e -> e != owner
                        && e.isAlive()
                        && CombatTools.isValidCombatTarget(e, owner)
        );

        var source = CombatTools.getDamageSource(level, this, owner, "arcane_beam");
        for(var entity : entities){
            CombatTools.applyDamage(entity, damage, source, SpellsRegistry.ARCANE_BEAM.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // 永続化はしない.
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        // 永続化はしない.
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 64 * 64;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var length = getLength();
        var radius = getRadius();

        var dir = Vec3.directionFromRotation(getXRot(), getYRot()).normalize();
        var start = position();
        var end = start.add(dir.scale(length));

        // AABBはビーム区間を包含し、太さ分inflate.
        var inflate = Math.max(0.5, radius * 2.0);
        return new AABB(start, end).inflate(inflate);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setup(int outerArgb, int innerArgb, float length, float radius) {
        this.entityData.set(COLOR_ARGB_OUTER, outerArgb);
        this.entityData.set(COLOR_ARGB_INNER, innerArgb);
        this.entityData.set(LENGTH, length);
        this.entityData.set(RADIUS, radius);
    }

    public void updateLength(float maxLength, Level level) {
        var currentLength = entityData.get(LENGTH);
        var blockHit = level.clip(new ClipContext(
                position(),
                position().add(getLookAngle().scale(maxLength)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                this
        ));

        var hitLength = blockHit.getLocation().distanceTo(position());

        // 頻繁な更新が走らないように微小な差を無視するようにする.
        if(Math.abs(currentLength - hitLength) > 0.001) {
            entityData.set(LENGTH, (float) hitLength);
        }
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
