package jp.aquafactory.apprenticecodex.common.spells.bulletstream;

import jp.aquafactory.apprenticecodex.client.particles.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BulletStreamMinigunEntity extends SummonWeaponEntity {

    private float damage;
    private float range;

    private int currentDelay;
    private int initialDelay;
    private int nextDelay;


    public BulletStreamMinigunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }
    public BulletStreamMinigunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                1.5,
                12,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル(再ログインで消えるので制御不要)
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.2f,
                    8,
                    0.01f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var locatePosition = getStandbyPosition();
        followTargetPosition(locatePosition);

        // 常に視線先を狙う.
        var aimResult = RaycastTools.raycastFromEye(owner, 64, 1, e -> CombatTools.isValidCombatTarget(e, this));
        var targetVec = aimResult.hitPosition().subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        hasImpulse = true;

        if (initialDelay > 0){
            --initialDelay;
        } else if (currentDelay >0){
            --currentDelay;
        } else {
            fire(level, nextDelay == 0);
            currentDelay = nextDelay;
            if (nextDelay > 0){
                --nextDelay;
            }
        }
    }

    private void fire(Level level, boolean is1tickFire) {
        var owner = getOwner();
        var hitResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this) && e != owner);
        if (hitResult.hitEntity() != null) {
            var target = hitResult.hitEntity();
            var source = CombatTools.getDamageSource(level, this, owner, "bullet_stream");
            CombatTools.applyDamage(target, damage, source, SpellsRegistry.BULLET_STREAM.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }

        if (level instanceof ServerLevel server) {
            if (!is1tickFire || (tickCount % 2 == 0)) {
                var firePosition = position().add(getLookAngle().normalize().scale(1));
                server.sendParticles(new MuzzleFlashParticleOptions(1f), firePosition.x, firePosition.y, firePosition.z, 1, .05, .05, .05, 0);
            }

            var hitPosition = hitResult.hitPosition();
            if (hitResult.hitType() == RaycastTools.TargetType.LIVING_ENTITY) {
                server.sendParticles(ParticleTypes.CRIT, hitPosition.x, hitPosition.y, hitPosition.z, 1, .1, .1, .1, 0);
            }
            if (hitResult.hitType() == RaycastTools.TargetType.BLOCK && tickCount % 2 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, hitPosition.x, hitPosition.y, hitPosition.z, 1, .1, .1, .1, 0);
            }
        }

        // todo:音を差し替えつつ、1tick連射になったらループっぽくする.
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.RIFLE.get(), SoundSource.PLAYERS, 1.0f);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return RotationTools.calculateBehindPosition(owner, -0.3, 1.0, -0.7);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }
    public void setRange(float range) {
        this.range = range;
    }
    public void setTickSettings(int initialDelay, int nextDelay) {
        this.initialDelay = initialDelay;
        this.nextDelay = nextDelay;
    }
}

