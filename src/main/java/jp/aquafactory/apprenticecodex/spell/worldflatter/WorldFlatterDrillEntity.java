package jp.aquafactory.apprenticecodex.spell.worldflatter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WorldFlatterDrillEntity extends SummonWeaponEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation ROTATE = RawAnimation.begin().thenLoop("spin");
    private static final double BLOCK_ATTACH_DISTANCE = 0.5;
    private static final double MOB_ATTACH_DISTANCE = 0.25;
    private static final double TARGET_CHANGED_EPSILON_SQR = 0.0025;

    private float damage;
    private int reachSpeed;
    private RaycastTools.TargetType targetType = RaycastTools.TargetType.NONE;
    private Vec3 ownerTargetHitPos = Vec3.ZERO;
    private @Nullable BlockPos ownerTargetBlockPos;
    private @Nullable LivingEntity ownerTargetEntity;
    private @Nullable Vec3 moveStartPos;
    private @Nullable Vec3 moveTargetPos;
    private int moveTick;

    public WorldFlatterDrillEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public WorldFlatterDrillEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
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

        if (targetType == RaycastTools.TargetType.LIVING_ENTITY && ownerTargetEntity != null) {
            handleLivingTarget(owner, ownerTargetEntity);
            return;
        }

        if (targetType == RaycastTools.TargetType.BLOCK && ownerTargetBlockPos != null) {
            handleBlockTarget(owner, ownerTargetBlockPos, ownerTargetHitPos);
            return;
        }

        moveToTarget(getStandbyPosition(), owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.6, 0.5, -0.8);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setReachSpeed(int reachSpeed) {
        this.reachSpeed = reachSpeed;
    }

    public void updateOwnerTarget(RaycastTools.TargetResult result) {
        targetType = result.hitType();
        ownerTargetHitPos = result.hitPosition();
        ownerTargetBlockPos = result.hitBlock();

        if (result.hitEntity() instanceof LivingEntity target) {
            ownerTargetEntity = target;
        } else {
            ownerTargetEntity = null;
        }
    }

    private void handleLivingTarget(LivingEntity owner, LivingEntity target) {
        if (!target.isAlive()) {
            moveToTarget(getStandbyPosition(), owner.getYRot(), owner.getXRot());
            return;
        }

        var ownerLook = owner.getViewVector(1.0F);
        if (ownerLook.lengthSqr() < 1.0E-6) {
            ownerLook = target.position().subtract(owner.position()).normalize();
        } else {
            ownerLook = ownerLook.normalize();
        }

        var targetCenter = target.getBoundingBox().getCenter();
        var attachPosition = targetCenter.subtract(ownerLook.scale(MOB_ATTACH_DISTANCE));
        moveToTarget(attachPosition, owner.getYRot(), owner.getXRot());
        performMobAttack(owner);
    }

    private void performMobAttack(LivingEntity owner) {
        if (tickCount % 2 != 0) {
            return;
        }

        var level = level();
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.WORLD_FLATTER);
        var hitResult = RaycastTools.sampleBeamHits(
                level,
                position(),
                position().add(getLookAngle().normalize().scale(1.0)),
                1,
                0.5,
                e -> e != owner && CombatTools.isValidCombatTarget(e, owner)
        );

        for (var hit : hitResult) {
            CombatTools.applyDamage(
                    hit,
                    damage,
                    source,
                    SpellRegistry.WORLD_FLATTER.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK
            );
        }
    }

    private void handleBlockTarget(LivingEntity owner, BlockPos targetPos, Vec3 hitPos) {
        var level = level();
        if (level.isEmptyBlock(targetPos)) {
            moveToTarget(getStandbyPosition(), owner.getYRot(), owner.getXRot());
            return;
        }

        var blockCenter = Vec3.atCenterOf(targetPos);
        var sideNormal = resolveBlockSideNormal(blockCenter, hitPos, owner.getViewVector(1.0F));
        var attachPosition = blockCenter.add(sideNormal.scale(0.5 + BLOCK_ATTACH_DISTANCE));

        var directionToBlock = blockCenter.subtract(attachPosition);
        var yawPitch = RotationTools.calculateYawPitchByDirection(directionToBlock);
        moveToTarget(attachPosition, yawPitch.yaw(), yawPitch.pitch());

        if (position().distanceToSqr(attachPosition) <= 0.04) {
            onBlockAttached(targetPos, hitPos);
        }
    }

    private void onBlockAttached(BlockPos targetPos, Vec3 hitPos) {
        // TODO: implement block interaction when attached to a block.
        ApprenticeCodex.LOGGER.info("Block attached: {}", targetPos);
    }

    private void moveToTarget(Vec3 targetPos, float yaw, float pitch) {
        if (moveTargetPos == null || moveTargetPos.distanceToSqr(targetPos) > TARGET_CHANGED_EPSILON_SQR) {
            moveStartPos = position();
            moveTargetPos = targetPos;
            moveTick = 0;
        }

        if (moveStartPos == null || moveTargetPos == null) {
            moveStartPos = position();
            moveTargetPos = position();
            moveTick = 0;
        }

        setYRot(yaw);
        setXRot(pitch);
        setRot(getYRot(), getXRot());
        hasImpulse = true;

        var moveDuration = Math.max(1, reachSpeed);
        moveTick = Math.min(moveTick + 1, moveDuration);
        var t = moveTick / (double) moveDuration;
        var eased = 1.0 - (1.0 - t) * (1.0 - t);
        var delta = moveTargetPos.subtract(moveStartPos).scale(eased);
        var newPos = moveStartPos.add(delta);
        setPos(newPos.x, newPos.y, newPos.z);
    }

    private static Vec3 resolveBlockSideNormal(Vec3 blockCenter, Vec3 hitPos, Vec3 fallbackLook) {
        var offset = hitPos.subtract(blockCenter);
        if (offset.lengthSqr() < 1.0E-6) {
            return axisAlignedNormal(fallbackLook.scale(-1));
        }
        return axisAlignedNormal(offset);
    }

    private static Vec3 axisAlignedNormal(Vec3 vec) {
        var absX = Math.abs(vec.x);
        var absY = Math.abs(vec.y);
        var absZ = Math.abs(vec.z);

        if (absX >= absY && absX >= absZ) {
            return new Vec3(signOrOne(vec.x), 0, 0);
        }
        if (absY >= absX && absY >= absZ) {
            return new Vec3(0, signOrOne(vec.y), 0);
        }
        return new Vec3(0, 0, signOrOne(vec.z));
    }

    private static double signOrOne(double value) {
        return value < 0.0 ? -1.0 : 1.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(ROTATE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
