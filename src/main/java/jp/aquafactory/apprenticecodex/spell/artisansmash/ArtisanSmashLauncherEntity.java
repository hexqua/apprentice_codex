package jp.aquafactory.apprenticecodex.spell.artisansmash;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ArtisanSmashLauncherEntity extends SummonWeaponEntity {
    public static final int MAX_RECOIL_TICK = 10;

    private static final float AIM_PITCH_UP_DEGREES = 15.0f;
    private static final double FIRE_OFFSET = 0.75;
    private static final EntityDataAccessor<Integer> RECOIL_TICK =
            SynchedEntityData.defineId(ArtisanSmashLauncherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_RELEASED =
            SynchedEntityData.defineId(ArtisanSmashLauncherEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage;
    private float splashRadius;
    private float speed;
    private int recoilTick;
    private boolean isReleased;

    public ArtisanSmashLauncherEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public ArtisanSmashLauncherEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RECOIL_TICK, 0);
        entityData.define(IS_RELEASED, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        splashRadius = tag.getFloat("SplashRadius");
        speed = tag.getFloat("Speed");
        recoilTick = tag.getInt("RecoilTick");
        isReleased = tag.getBoolean("IsReleased");
        entityData.set(RECOIL_TICK, recoilTick);
        entityData.set(IS_RELEASED, isReleased);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("SplashRadius", splashRadius);
        tag.putFloat("Speed", speed);
        tag.putInt("RecoilTick", recoilTick);
        tag.putBoolean("IsReleased", isReleased);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                1.0,
                12,
                0.08f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.25f,
                    10,
                    0.01f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (isReleased) {
            if (recoilTick > 0) {
                --recoilTick;
                entityData.set(RECOIL_TICK, recoilTick);
            } else {
                discard();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(Mth.clamp(owner.getXRot() - AIM_PITCH_UP_DEGREES, -90.0f, 90.0f));
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    public void fire(Level level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            return;
        }

        var direction = getLookAngle();
        if (direction.lengthSqr() < 1.0e-6) {
            direction = owner.getLookAngle();
        }
        direction = direction.normalize();

        var projectile = new ArtisanSmashShellEntity(EntityRegistry.ARTISAN_SMASH_SHELL.get(), level, owner);
        projectile.setPos(position().add(direction.scale(FIRE_OFFSET)));
        projectile.setup(damage, splashRadius, direction, speed);
        level.addFreshEntity(projectile);

        recoilTick = MAX_RECOIL_TICK;
        isReleased = true;
        entityData.set(RECOIL_TICK, recoilTick);
        entityData.set(IS_RELEASED, true);
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.VANILLA_PROJECTILE_SHOOT.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.5, -0.7, -0.25);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSplashRadius(float splashRadius) {
        this.splashRadius = splashRadius;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getRecoilTick() {
        return entityData.get(RECOIL_TICK);
    }

    public boolean getIsReleased() {
        return entityData.get(IS_RELEASED);
    }
}
