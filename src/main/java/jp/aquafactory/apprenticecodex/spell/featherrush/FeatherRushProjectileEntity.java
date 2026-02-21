package jp.aquafactory.apprenticecodex.spell.featherrush;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class FeatherRushProjectileEntity extends ThrowableProjectile {
    private static final int LIFE_TICKS = 20 * 5;
    private static final int BACKWARD_TICKS = 10;
    private static final double BACKWARD_SPEED = 0.05;
    private static final double FORWARD_SPEED = 1.75;
    private static final double MIN_VECTOR_LENGTH_SQR = 1.0e-6;

    private float damage;
    private int backwardTicksRemaining = BACKWARD_TICKS;
    private boolean forwardDirectionFixed;
    private Vec3 backwardDirection = new Vec3(0.0, 0.0, -1.0);
    private Vec3 forwardDirection = new Vec3(0.0, 0.0, 1.0);

    public FeatherRushProjectileEntity(EntityType<? extends FeatherRushProjectileEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public FeatherRushProjectileEntity(EntityType<? extends FeatherRushProjectileEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, owner, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        var level = level();
        if (level.isClientSide) {
            return;
        }

        if (tickCount > LIFE_TICKS) {
            discard();
            return;
        }

        updateVelocity();

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
        }

        if (isRemoved()) {
            return;
        }

        move(MoverType.SELF, getDeltaMovement());
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    private void updateVelocity() {
        if (backwardTicksRemaining > 0) {
            --backwardTicksRemaining;
            setDeltaMovement(backwardDirection.scale(BACKWARD_SPEED));
            return;
        }

        if (!forwardDirectionFixed) {
            lockForwardDirectionByOwnerLook();
        }

        setDeltaMovement(forwardDirection.scale(FORWARD_SPEED));
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hit) {
        super.onHitEntity(hit);

        var level = level();
        if (level.isClientSide) {
            return;
        }

        var owner = getOwner();
        if (CombatTools.isValidCombatTarget(hit.getEntity(), owner)) {
            var target = CombatTools.resolutePartEntity(hit.getEntity());
            var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.FEATHER_RUSH);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.FEATHER_RUSH.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            AudioTools.playSoundFromEntity(level, this, SoundEvents.ARROW_HIT, SoundSource.PLAYERS);
        }

        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("BackwardTicksRemaining", backwardTicksRemaining);
        tag.putBoolean("ForwardDirectionFixed", forwardDirectionFixed);
        tag.putDouble("BackwardX", backwardDirection.x);
        tag.putDouble("BackwardY", backwardDirection.y);
        tag.putDouble("BackwardZ", backwardDirection.z);
        tag.putDouble("ForwardX", forwardDirection.x);
        tag.putDouble("ForwardY", forwardDirection.y);
        tag.putDouble("ForwardZ", forwardDirection.z);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            damage = tag.getFloat("Damage");
        }
        if (tag.contains("BackwardTicksRemaining")) {
            backwardTicksRemaining = tag.getInt("BackwardTicksRemaining");
        } else {
            backwardTicksRemaining = BACKWARD_TICKS;
        }
        if (tag.contains("ForwardDirectionFixed")) {
            forwardDirectionFixed = tag.getBoolean("ForwardDirectionFixed");
        } else {
            forwardDirectionFixed = false;
        }
        if (tag.contains("BackwardX") && tag.contains("BackwardY") && tag.contains("BackwardZ")) {
            backwardDirection = normalizeOrFallback(new Vec3(
                    tag.getDouble("BackwardX"),
                    tag.getDouble("BackwardY"),
                    tag.getDouble("BackwardZ")
            ), backwardDirection);
        }
        if (tag.contains("ForwardX") && tag.contains("ForwardY") && tag.contains("ForwardZ")) {
            forwardDirection = normalizeOrFallback(new Vec3(
                    tag.getDouble("ForwardX"),
                    tag.getDouble("ForwardY"),
                    tag.getDouble("ForwardZ")
            ), forwardDirection);
        }
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        double max = 96.0;
        return distanceSqr < max * max;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setStraightFlightDirections(Vec3 initialBackwardDirection, Vec3 initialForwardDirection, int additionalDelayTicks) {
        backwardDirection = normalizeOrFallback(initialBackwardDirection, new Vec3(0.0, 0.0, -1.0));
        forwardDirection = normalizeOrFallback(initialForwardDirection, new Vec3(0.0, 0.0, 1.0));
        backwardTicksRemaining = BACKWARD_TICKS + Math.max(0, additionalDelayTicks);
        forwardDirectionFixed = false;
        setDeltaMovement(backwardDirection.scale(BACKWARD_SPEED));
        ProjectileUtil.rotateTowardsMovement(this, 1.0f);
    }

    private void lockForwardDirectionByOwnerLook() {
        if (getOwner() instanceof LivingEntity owner) {
            forwardDirection = normalizeOrFallback(owner.getLookAngle(), forwardDirection);
        }
        forwardDirectionFixed = true;
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector.lengthSqr() > MIN_VECTOR_LENGTH_SQR) {
            return vector.normalize();
        }

        if (fallback.lengthSqr() > MIN_VECTOR_LENGTH_SQR) {
            return fallback.normalize();
        }

        return new Vec3(0.0, 0.0, 1.0);
    }
}
