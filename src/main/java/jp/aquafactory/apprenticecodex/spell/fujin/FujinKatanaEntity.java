package jp.aquafactory.apprenticecodex.spell.fujin;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FujinKatanaEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {
    static final int FIRST_SLASH_TICK = 10;
    static final int SLASH_INTERVAL_TICKS = 5;
    static final float SLASH_ANIMATION_SPEED = 3.0F;
    public static final String BLADE_CACHE_KEY = "default";
    public static final String SCABBARD_CACHE_KEY = "scabbard";

    private static final EntityDataAccessor<Boolean> SHOW_TRAIL =
            SynchedEntityData.defineId(FujinKatanaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ANIM_STANDBY = RawAnimation.begin().thenLoop("standby");
    private static final RawAnimation ANIM_SLASH_0TO1 = RawAnimation.begin().thenPlayAndHold("slash0to1");
    private static final RawAnimation ANIM_SLASH_1TO2 = RawAnimation.begin().thenPlayAndHold("slash1to2");
    private static final RawAnimation ANIM_SLASH_2TO1 = RawAnimation.begin().thenPlayAndHold("slash2to1");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage;
    private float projectileRange;
    private int slashPhase;

    public FujinKatanaEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public FujinKatanaEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOW_TRAIL, false);
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        setRot(getYRot(), getXRot());

        if (tickCount >= FIRST_SLASH_TICK
                && (tickCount - FIRST_SLASH_TICK) % SLASH_INTERVAL_TICKS == 0) {
            fireSlash(level, owner);
        }
    }

    private void fireSlash(Level level, LivingEntity owner) {
        triggerSlashAnimation();
        entityData.set(SHOW_TRAIL, true);

        var direction = getLookAngle().normalize();
        var projectile = new FujinSlashProjectileEntity(EntityRegistry.FUJIN_SLASH_PROJECTILE.get(), level, owner);
        projectile.setPos(position().add(direction.scale(0.35D)));
        projectile.setDamage(damage);
        projectile.setMaxTravelDistance(projectileRange);
        projectile.setCombatOwnerUuid(getCombatOwnerUuid());
        projectile.shoot(direction);
        level.addFreshEntity(projectile);

        AudioTools.playSoundFromEntity(level, this, SoundRegistry.KATANA_SLASH.get(), SoundSource.PLAYERS);
        AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);
    }

    private void triggerSlashAnimation() {
        switch (slashPhase) {
            case 0 -> {
                triggerAnim("main", "slash0to1");
                slashPhase = 1;
            }
            case 1 -> {
                triggerAnim("main", "slash1to2");
                slashPhase = 2;
            }
            default -> {
                triggerAnim("main", "slash2to1");
                slashPhase = 1;
            }
        }
    }

    @Override
    public void onClientRemoval() {
        spawnRemovalLineParticle(BLADE_CACHE_KEY);
        spawnRemovalLineParticle(SCABBARD_CACHE_KEY);
        GeoBonePoseCache.remove(getUUID());
        super.onClientRemoval();
    }

    private void spawnRemovalLineParticle(String cacheKey) {
        var pose = GeoBonePoseCache.getPrev(getUUID(), cacheKey);
        if (pose == null) {
            return;
        }

        var yawDeg = RotationTools.calculateYawPitchByEntity(this, 1.0F).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var rootLocal = pose.root().subtract(position());
        var tipLocal = pose.tip().subtract(position());
        var rootWorld = rootLocal.yRot(yawRad).add(position());
        var tipWorld = tipLocal.yRot(yawRad).add(position());
        EffectTools.createLineParticle(rootWorld, tipWorld, 0.25D, 0.1D, 0.01D, ParticleTypes.END_ROD, level());
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setProjectileRange(float projectileRange) {
        this.projectileRange = Math.max(0.0F, projectileRange);
    }

    @Override
    public boolean isTrailActive() {
        return entityData.get(SHOW_TRAIL);
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFF00E5FF;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 0.0D, 0.0D, -0.75D);
        }
        return Vec3.ZERO;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        projectileRange = tag.getFloat("ProjectileRange");
        slashPhase = tag.getInt("SlashPhase");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("ProjectileRange", projectileRange);
        tag.putInt("SlashPhase", slashPhase);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", state -> {
                    state.getController().setAnimation(ANIM_STANDBY);
                    return PlayState.CONTINUE;
                })
                .triggerableAnim("slash0to1", ANIM_SLASH_0TO1)
                .triggerableAnim("slash1to2", ANIM_SLASH_1TO2)
                .triggerableAnim("slash2to1", ANIM_SLASH_2TO1)
                .setAnimationSpeedHandler(entity -> (double) SLASH_ANIMATION_SPEED));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
