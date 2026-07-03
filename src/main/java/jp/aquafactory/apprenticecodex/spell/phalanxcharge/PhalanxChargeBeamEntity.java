package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class PhalanxChargeBeamEntity extends Entity implements TraceableEntity {
    public static final int LIFE_TICKS = 4;

    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(PhalanxChargeBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(PhalanxChargeBeamEntity.class, EntityDataSerializers.FLOAT);

    private Entity owner;
    private float damage;
    private boolean attackResolved;

    public PhalanxChargeBeamEntity(EntityType<? extends PhalanxChargeBeamEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public PhalanxChargeBeamEntity(EntityType<? extends PhalanxChargeBeamEntity> entityType, Level level, Entity owner) {
        this(entityType, level);
        this.owner = owner;
    }

    @Override
    public Entity getOwner() {
        return owner;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(LENGTH, 1.0f);
        entityData.define(RADIUS, 0.15f);
    }

    @Override
    public void tick() {
        var level = level();
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (owner == null || owner.isRemoved()) {
            discard();
            return;
        }

        if (!attackResolved) {
            damageHitTargets(level);
            attackResolved = true;
        }

        if (tickCount >= LIFE_TICKS) {
            discard();
        }
    }

    public void setup(Level level, float maxLength, float radius, float damage) {
        this.damage = damage;
        entityData.set(RADIUS, radius);
        updateLength(level, maxLength);
    }

    private void damageHitTargets(Level level) {
        var start = position();
        var end = start.add(getLookAngle().normalize().scale(getLength()));
        var targets = RaycastTools.sampleBeamHits(
                level,
                start,
                end,
                getRadius(),
                0.2,
                e -> e != owner
                        && e.isAlive()
                        && CombatTools.isValidCombatTarget(e, owner)
        );

        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.PHALANX_CHARGE);
        for (var target : targets) {
            CombatTools.applyDamage(
                    target,
                    damage,
                    source,
                    SpellRegistry.PHALANX_CHARGE.get().getSchoolType(),
                    CombatTools.KnockbackTypes.DEFAULT
            );
        }
    }

    private void updateLength(Level level, float maxLength) {
        var blockHit = level.clip(new ClipContext(
                position(),
                position().add(getLookAngle().scale(maxLength)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        var hitLength = (float) blockHit.getLocation().distanceTo(position());
        entityData.set(LENGTH, Math.max(0.0f, hitLength));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        damage = pCompound.getFloat("Damage");
        entityData.set(LENGTH, pCompound.getFloat("Length"));
        entityData.set(RADIUS, pCompound.getFloat("Radius"));
        attackResolved = pCompound.getBoolean("AttackResolved");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Length", getLength());
        pCompound.putFloat("Radius", getRadius());
        pCompound.putBoolean("AttackResolved", attackResolved);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 96 * 96;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        var length = getLength();
        var radius = getRadius();

        var dir = Vec3.directionFromRotation(getXRot(), getYRot()).normalize();
        var start = position();
        var end = start.add(dir.scale(length));
        var inflate = Math.max(0.5, radius * 2.0);
        return new AABB(start, end).inflate(inflate);
    }

    public float getLifeProgress(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / LIFE_TICKS, 0.0f, 1.0f);
    }

    public float getLength() {
        return entityData.get(LENGTH);
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }
}
