package jp.aquafactory.apprenticecodex.spell.worldflatter;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WorldFlatterDrillEntity extends SummonWeaponEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation ROTATE = RawAnimation.begin().thenLoop("spin");
    private static final double BLOCK_ATTACH_DISTANCE = 0.5;
    private static final double MOB_ATTACH_DISTANCE = 0.25;
    private static final double BLOCK_ATTACH_COMPLETE_DISTANCE_SQR = 0.04;
    private static final double TARGET_CHANGED_EPSILON_SQR = 0.0025;
    private static final int CRACK_ID_SLOT_BITS = 5;
    private static final int CRACK_ID_SLOT_COUNT = 1 << CRACK_ID_SLOT_BITS;

    private float damage;
    private float toolSpeed;
    private int reachSpeed;
    private RaycastTools.TargetType targetType = RaycastTools.TargetType.NONE;
    private Vec3 ownerTargetHitPos = Vec3.ZERO;
    private @Nullable BlockPos ownerTargetBlockPos;
    private @Nullable LivingEntity ownerTargetEntity;
    private @Nullable Vec3 moveStartPos;
    private @Nullable Vec3 moveTargetPos;
    private int moveTick;
    private @Nullable BlockPos breakCenterPos;
    private @Nullable Vec3 breakSideNormal;
    private @Nullable BlockState breakRepresentativeState;
    private float breakProgress;
    private final Map<BlockPos, Integer> crackProgressIds = new LinkedHashMap<>();

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
    }

    @Override
    public void tickOnServer(ServerLevel level){
        if (!(getOwner() instanceof LivingEntity owner)) {
            resetBreakProgress(level);
            discard();
            return;
        }

        if (targetType == RaycastTools.TargetType.LIVING_ENTITY && ownerTargetEntity != null) {
            resetBreakProgress(level);
            handleLivingTarget(owner, ownerTargetEntity);
            return;
        }

        if (targetType == RaycastTools.TargetType.BLOCK && ownerTargetBlockPos != null) {
            handleBlockTarget(level, owner, ownerTargetBlockPos, ownerTargetHitPos);
            return;
        }

        resetBreakProgress(level);
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

    public void setToolSpeed(float toolSpeed) {
        this.toolSpeed = toolSpeed;
    }

    public void updateOwnerTarget(Level level, RaycastTools.TargetResult result) {
        var shouldReset = shouldResetByTargetChange(result);
        targetType = result.hitType();
        ownerTargetHitPos = result.hitPosition();
        ownerTargetBlockPos = result.hitBlock();

        if (result.hitEntity() instanceof LivingEntity target) {
            ownerTargetEntity = target;
        } else {
            ownerTargetEntity = null;
        }

        if (shouldReset) {
            resetBreakProgress(level);
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

    private void handleBlockTarget(Level level, LivingEntity owner, BlockPos targetPos, Vec3 hitPos) {
        if (level.isEmptyBlock(targetPos)) {
            resetBreakProgress(level);
            moveToTarget(getStandbyPosition(), owner.getYRot(), owner.getXRot());
            return;
        }

        // 側面の計算はエンティティ共用の判定を使っているため、当たっている場所から計算する.
        var blockCenter = Vec3.atCenterOf(targetPos);
        var sideNormal = resolveBlockSideNormal(blockCenter, hitPos, owner.getViewVector(1.0F));
        var attachPosition = blockCenter.add(sideNormal.scale(0.5 + BLOCK_ATTACH_DISTANCE));

        var directionToBlock = blockCenter.subtract(attachPosition);
        var yawPitch = RotationTools.calculateYawPitchByDirection(directionToBlock);
        moveToTarget(attachPosition, yawPitch.yaw(), yawPitch.pitch());

        if (position().distanceToSqr(attachPosition) <= BLOCK_ATTACH_COMPLETE_DISTANCE_SQR) {
            onBlockAttached(level, owner, targetPos, sideNormal);
        }
    }

    private void onBlockAttached(Level level, LivingEntity owner, BlockPos targetPos, Vec3 sideNormal) {
        if (breakCenterPos == null
                || !breakCenterPos.equals(targetPos)
                || !isSameNormal(breakSideNormal, sideNormal)) {
            resetBreakProgress(level);
            breakCenterPos = targetPos.immutable();
            breakSideNormal = sideNormal;
            breakRepresentativeState = level.getBlockState(breakCenterPos);
        }

        processBlockBreak(level, owner);
    }

    private void processBlockBreak(Level level, LivingEntity owner) {
        if (breakCenterPos == null || breakSideNormal == null) {
            resetBreakProgress(level);
            return;
        }

        var representativeState = level.getBlockState(breakCenterPos);
        if (breakRepresentativeState != null && !representativeState.equals(breakRepresentativeState)) {
            resetBreakProgress(level);
            return;
        }

        if (!canBreakTarget(level, breakCenterPos, representativeState, representativeState)) {
            resetBreakProgress(level);
            return;
        }

        var hardness = representativeState.getDestroySpeed(level, breakCenterPos);
        if (hardness < 0.0f) {
            resetBreakProgress(level);
            return;
        }

        var breakableBlocks = collectBreakableBlocks(level, breakCenterPos, breakSideNormal, representativeState);
        if (breakableBlocks.isEmpty()) {
            resetBreakProgress(level);
            return;
        }

        var breakDelta = hardness <= 0.0f ? 1.0f : (toolSpeed / (hardness * 30.0f));
        breakProgress += breakDelta;
        var stage = Math.min(9, (int) (breakProgress * 10.0f));
        updateCracks(level, breakableBlocks, stage);

        if (breakProgress < 1.0f) {
            return;
        }

        if (!(owner instanceof ServerPlayer server)) {
            return;
        }

        for (var pos : breakableBlocks) {
            var state = level.getBlockState(pos);
            if (!canBreakTarget(level, pos, state, representativeState)) {
                continue;
            }
            BlockTools.breakBlockByPlayerHands(level, server, pos, WorldFlatter.createDummyTool(server));
        }

        resetBreakProgress(level);
    }

    private Set<BlockPos> collectBreakableBlocks(Level level, BlockPos centerPos, Vec3 sideNormal, BlockState representativeState) {
        var positions = new LinkedHashSet<BlockPos>(9);
        for (var axisA = -1; axisA <= 1; axisA++) {
            for (var axisB = -1; axisB <= 1; axisB++) {
                var pos = offsetOnParallelPlane(centerPos, sideNormal, axisA, axisB);
                var state = level.getBlockState(pos);
                if (!canBreakTarget(level, pos, state, representativeState)) {
                    continue;
                }
                positions.add(pos.immutable());
            }
        }
        return positions;
    }

    private static BlockPos offsetOnParallelPlane(BlockPos centerPos, Vec3 normal, int axisA, int axisB) {
        if (Math.abs(normal.x) > 0.5) {
            return centerPos.offset(0, axisA, axisB);
        }
        if (Math.abs(normal.y) > 0.5) {
            return centerPos.offset(axisA, 0, axisB);
        }
        return centerPos.offset(axisA, axisB, 0);
    }

    private static boolean canBreakTarget(Level level, BlockPos pos, BlockState state, @Nullable BlockState representativeState) {
        if (state.isAir()) {
            return false;
        }

        var isSameAsRepresentative = representativeState != null && state.is(representativeState.getBlock());
        if (state.is(Tags.Blocks.ORES) && !isSameAsRepresentative) {
            return false;
        }

        if (state.getDestroySpeed(level, pos) < 0.0f) {
            return false;
        }

        if (state.requiresCorrectToolForDrops() && !TierSortingRegistry.isCorrectTierForDrops(Tiers.IRON, state)) {
            return false;
        }

        return true;
    }

    private void updateCracks(Level level, Set<BlockPos> currentBlocks, int stage) {
        var iterator = crackProgressIds.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (currentBlocks.contains(entry.getKey())) {
                continue;
            }
            level.destroyBlockProgress(entry.getValue(), entry.getKey(), -1);
            iterator.remove();
        }

        var crackIdBase = getId() << CRACK_ID_SLOT_BITS;
        for (var pos : currentBlocks) {
            var crackId = crackProgressIds.computeIfAbsent(pos, ignored -> allocateCrackProgressId(crackIdBase));
            level.destroyBlockProgress(crackId, pos, stage);
        }
    }

    private int allocateCrackProgressId(int crackIdBase) {
        for (int slot = 0; slot < CRACK_ID_SLOT_COUNT; slot++) {
            var candidate = crackIdBase + slot;
            if (!crackProgressIds.containsValue(candidate)) {
                return candidate;
            }
        }

        return crackIdBase + CRACK_ID_SLOT_COUNT + crackProgressIds.size();
    }

    private void resetBreakProgress(Level level) {
        if (!level.isClientSide) {
            for (var entry : crackProgressIds.entrySet()) {
                level.destroyBlockProgress(entry.getValue(), entry.getKey(), -1);
            }
        }

        crackProgressIds.clear();
        breakCenterPos = null;
        breakSideNormal = null;
        breakRepresentativeState = null;
        breakProgress = 0.0f;
    }

    private boolean shouldResetByTargetChange(RaycastTools.TargetResult result) {
        if (breakCenterPos == null || breakSideNormal == null) {
            return false;
        }

        if (result.hitType() != RaycastTools.TargetType.BLOCK || result.hitBlock() == null) {
            return true;
        }

        if (!breakCenterPos.equals(result.hitBlock())) {
            return true;
        }

        var fallbackLook = getOwnerLookFallback();
        var newNormal = resolveBlockSideNormal(Vec3.atCenterOf(result.hitBlock()), result.hitPosition(), fallbackLook);
        return !isSameNormal(breakSideNormal, newNormal);
    }

    private Vec3 getOwnerLookFallback() {
        if (getOwner() instanceof LivingEntity owner) {
            var ownerLook = owner.getViewVector(1.0F);
            if (ownerLook.lengthSqr() >= 1.0E-6) {
                return ownerLook;
            }
        }

        var look = getLookAngle();
        if (look.lengthSqr() >= 1.0E-6) {
            return look;
        }
        return new Vec3(0, -1, 0);
    }

    private static boolean isSameNormal(@Nullable Vec3 a, @Nullable Vec3 b) {
        if (a == null || b == null) {
            return false;
        }
        return Math.abs(a.x - b.x) < 1.0E-6
                && Math.abs(a.y - b.y) < 1.0E-6
                && Math.abs(a.z - b.z) < 1.0E-6;
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
    public void remove(@NotNull RemovalReason reason) {
        var level = level();
        if (!level.isClientSide) {
            resetBreakProgress(level);
        }
        super.remove(reason);
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
