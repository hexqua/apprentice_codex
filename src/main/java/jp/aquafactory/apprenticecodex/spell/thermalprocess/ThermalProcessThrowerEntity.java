package jp.aquafactory.apprenticecodex.spell.thermalprocess;

import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ThermalProcessThrowerEntity extends SummonWeaponEntity {
    private static final int ATTACK_START_DELAY_TICKS = 10;
    private static final double ATTACK_RADIUS = 0.25;
    private static final double ATTACK_SAMPLE_STEP = 0.2;
    private static final double FIRE_OFFSET = 0.65;
    private static final EntityDataAccessor<Boolean> IS_ATTACKING =
            SynchedEntityData.defineId(ThermalProcessThrowerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RANGE_SYNC =
            SynchedEntityData.defineId(ThermalProcessThrowerEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float range;
    private int startupTick;

    public ThermalProcessThrowerEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThermalProcessThrowerEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(IS_ATTACKING, false);
        entityData.define(RANGE_SYNC, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        range = pCompound.getFloat("Range");
        entityData.set(RANGE_SYNC, range);
        startupTick = pCompound.getInt("StartupTick");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Range", range);
        pCompound.putInt("StartupTick", startupTick);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                0.7,
                10,
                0.04f,
                0.01,
                ParticleTypes.FLAME,
                level
        );
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();
        if (level.isClientSide) {
            if (firstTick) {
                EffectTools.createRingParticle(
                        position(),
                        getLookAngle(),
                        0.2f,
                        8,
                        0.01f,
                        0.01,
                        ParticleTypes.FLAME,
                        level
                );
            }

            if (entityData.get(IS_ATTACKING)) {
                spawnBeamParticlesClient(level);
            }
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var standbyPosition = getStandbyPosition();
        setDeltaMovement(Vec3.ZERO);
        setPos(standbyPosition.x, standbyPosition.y, standbyPosition.z);

        var aimResult = RaycastTools.raycastFromEye(owner, range, 0.5, this::canAimAt);
        var beamStart = getBeamStartPosition();
        var aimVector = aimResult.hitPosition().subtract(beamStart);
        if (aimVector.lengthSqr() > 1.0e-6) {
            var yawPitch = RotationTools.calculateYawPitchByDirection(aimVector);
            setYRot(yawPitch.yaw());
            setXRot(yawPitch.pitch());
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }

        if (startupTick < ATTACK_START_DELAY_TICKS) {
            ++startupTick;
            return;
        }

        if (!entityData.get(IS_ATTACKING)) {
            entityData.set(IS_ATTACKING, true);
        }

        fireBeam(level, owner, aimResult.hitPosition());
    }

    private boolean canAimAt(Entity target) {
        if (target == this) {
            return false;
        }

        // アイテムに照射する機能のため、アイテムへ視線を向けられるようにする.
        if (target instanceof ItemEntity) {
            return true;
        }

        return CombatTools.isValidCombatTarget(target, this);
    }

    private Vec3 getBeamStartPosition() {
        var look = getLookAngle();
        if (look.lengthSqr() < 1.0e-6) {
            look = new Vec3(0, 0, 1);
        }
        return position().add(look.normalize().scale(FIRE_OFFSET));
    }

    private Vec3 resolveBeamEnd(Level level, Vec3 beamStart, Vec3 direction) {
        var maxBeamEnd = beamStart.add(direction.scale(getEffectiveRange()));
        var blockHit = level.clip(new ClipContext(beamStart, maxBeamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maxBeamEnd;
    }

    private void fireBeam(ServerLevel level, LivingEntity owner, Vec3 aimPosition) {
        var beamStart = getBeamStartPosition();
        var toAim = aimPosition.subtract(beamStart);
        var direction = toAim.lengthSqr() > 1.0e-6 ? toAim.normalize() : getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0e-6) {
            return;
        }

        var beamEnd = resolveBeamEnd(level, beamStart, direction);
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.THERMAL_PROCESS);
        var school = SpellRegistry.THERMAL_PROCESS.get().getSchoolType();
        var hits = RaycastTools.sampleBeamHits(
                level,
                beamStart,
                beamEnd,
                ATTACK_RADIUS,
                ATTACK_SAMPLE_STEP,
                e -> e != this && e != owner && CombatTools.isValidCombatTarget(e, owner)
        );

        for (var target : hits) {
            var applied = CombatTools.applyDamage(target, damage, source, school, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            if (!applied || !(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            applyOrUpdateThermalProcessing(livingTarget);
        }
    }

    private void applyOrUpdateThermalProcessing(LivingEntity target) {
        var current = target.getEffect(EffectRegistry.THERMAL_PROCESSING.get());
        var nextAmplifier = current == null
                ? 0
                : Math.min(current.getAmplifier() + 1, ThermalProcessing.MAX_AMPLIFIER);

        target.addEffect(new MobEffectInstance(
                EffectRegistry.THERMAL_PROCESSING.get(),
                ThermalProcessing.BASE_DURATION_TICKS,
                nextAmplifier,
                false,
                true,
                true
        ));

        if (nextAmplifier >= ThermalProcessing.MAX_AMPLIFIER) {
            target.setSecondsOnFire(ThermalProcessing.IGNITE_TICKS / 20);
        }
    }

    private void spawnBeamParticlesClient(Level level) {
        var direction = getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0e-6) {
            return;
        }

        if (tickCount % 2 != 0 ){
            return;
        }

        var beamStart = getBeamStartPosition();
        var beamEnd = resolveBeamEnd(level, beamStart, direction);
        var beamLength = beamStart.distanceTo(beamEnd);
        var particleCount = Math.max(3, (int) Math.ceil(beamLength * 3));
        var random = level.getRandom();

        for (var i = 0; i <= particleCount; i++) {
            var t = i / (double) particleCount;
            var pos = beamStart.add(beamEnd.subtract(beamStart).scale(t));
            var jitterScale = 0.03;
            var vx = direction.x * (0.05 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.01;
            var vy = direction.y * (0.05 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.01;
            var vz = direction.z * (0.05 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.01;
            level.addParticle(
                    ParticleHelper.FIRE_EMITTER,
                    pos.x + (random.nextDouble() - 0.5) * jitterScale,
                    pos.y + (random.nextDouble() - 0.5) * jitterScale,
                    pos.z + (random.nextDouble() - 0.5) * jitterScale,
                    vx,
                    vy,
                    vz
            );
        }
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.5, 0.7, -0.25);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setRange(float range) {
        this.range = range;
        entityData.set(RANGE_SYNC, range);
    }

    private float getEffectiveRange() {
        var synced = entityData.get(RANGE_SYNC);
        return synced > 0 ? synced : range;
    }
}
