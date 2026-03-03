package jp.aquafactory.apprenticecodex.spell.automagnet;

import jp.aquafactory.apprenticecodex.entity.PersistentSummonWeaponEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AutoMagnetFamiliarEntity extends PersistentSummonWeaponEntity implements GeoEntity {
    private static final double ORBIT_RADIUS = 1.4;
    private static final double ORBIT_HEIGHT = 1.2;
    private static final double ORBIT_SPEED = Math.PI / 45.0;
    private static final double FLOAT_SPEED = Math.PI / 20.0;
    private static final double FLOAT_RANGE = 0.15;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private double orbitOffset;

    public AutoMagnetFamiliarEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
        noPhysics = true;
    }

    public AutoMagnetFamiliarEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
        setNoGravity(true);
        noPhysics = true;
        orbitOffset = pLevel.random.nextDouble() * (Math.PI * 2.0);
        setStandbyPosition(owner);
    }

    @Override
    protected void defineSynchedData() {
        // 同期が必要なデータは持たない.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        orbitOffset = pCompound.contains("OrbitOffset") ? pCompound.getDouble("OrbitOffset") : 0.0;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putDouble("OrbitOffset", orbitOffset);
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive()) {
            discard();
            return;
        }

        if (owner.level() != level) {
            // 次元移動直後は再召喚側で復元する.
            discard();
            return;
        }

        noPhysics = true;
        var target = calculateOrbitPosition(owner);
        followTargetPosition(target);
        setXRot(0.0f);
        setYRot((float) (-Math.toDegrees(getOrbitAngle()) + 90.0));
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            var height = owner.getBbHeight() * 0.6 + ORBIT_HEIGHT;
            return owner.position().add(
                    Math.cos(orbitOffset) * ORBIT_RADIUS,
                    height,
                    Math.sin(orbitOffset) * ORBIT_RADIUS
            );
        }
        return Vec3.ZERO;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(@NotNull Entity pEntity) {
        // 当たり判定を持たせないため押し返し処理は無効化.
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private Vec3 calculateOrbitPosition(LivingEntity owner) {
        var angle = getOrbitAngle();
        var floatOffset = Math.sin(tickCount * FLOAT_SPEED + orbitOffset) * FLOAT_RANGE;
        var height = owner.getBbHeight() * 0.6 + ORBIT_HEIGHT + floatOffset;
        return owner.position().add(
                Math.cos(angle) * ORBIT_RADIUS,
                height,
                Math.sin(angle) * ORBIT_RADIUS
        );
    }

    private double getOrbitAngle() {
        return orbitOffset + tickCount * ORBIT_SPEED;
    }
}
