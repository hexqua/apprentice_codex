package jp.aquafactory.apprenticecodex.entity.floatmountbroom;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FloatmountBroomEntity extends Entity implements GeoEntity {
    public static final float WIDTH = 0.8F;
    public static final float HEIGHT = 0.5F;
    public static final int DISMOUNT_CONFIRM_TICKS = 30;
    public static final double DANGEROUS_HEIGHT = 3.0D;

    private static final double HOVER_HEIGHT = 0.5D;
    private static final double MAX_UNMOUNTED_FALL_SPEED = 0.1D;
    private static final double MAX_UNMOUNTED_RISE_SPEED = 0.1D;
    private static final double HORIZONTAL_ACCELERATION = 0.04D;
    private static final double MAX_HORIZONTAL_SPEED = 0.35D;
    private static final double EMERGENCY_MAX_HORIZONTAL_SPEED = 0.1D;
    private static final double VERTICAL_ACCELERATION = 0.05D;
    private static final double MAX_VERTICAL_SPEED = 0.15D;
    private static final float TURN_ACCELERATION = 1.0F;
    private static final float MAX_TURN_SPEED = 10.0F;
    private static final double POWERED_HORIZONTAL_DAMPING = 0.9D;
    private static final double COAST_HORIZONTAL_DAMPING = 0.85D;
    private static final float TURN_DAMPING = 0.9F;
    private static final float INPUT_EPSILON = 1.0e-4F;
    private static final int SERVER_INPUT_TIMEOUT_TICKS = 30;
    private static final String EMERGENCY_LANDING_TAG = "EmergencyLanding";
    private static final String LOW_MANA_WARNING_SHOWN_TAG = "LowManaWarningShown";
    /**
     * 箒のEntity原点から乗員のvehicle attachmentまでの高さ。モデル調整ではこの値だけを変更する。
     */
    public static final float RIDER_ATTACHMENT_Y = 0.15F;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation MOUNT = RawAnimation.begin().thenLoop("mount");

    private static final EntityDataAccessor<Integer> HURT_TIME =
            SynchedEntityData.defineId(FloatmountBroomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HURT_DIRECTION =
            SynchedEntityData.defineId(FloatmountBroomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(FloatmountBroomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> EMERGENCY_LANDING =
            SynchedEntityData.defineId(FloatmountBroomEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private float localForwardInput;
    private float localStrafeInput;
    private boolean localAscending;
    private boolean descendingInput;
    private float serverForwardInput;
    private boolean serverAscending;
    private long lastServerInputGameTime = Long.MIN_VALUE;
    private boolean lowManaWarningShown;
    private float turnSpeed;
    private boolean breaking;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    public FloatmountBroomEntity(EntityType<?> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HURT_TIME, 0);
        builder.define(HURT_DIRECTION, 1);
        builder.define(DAMAGE, 0.0F);
        builder.define(EMERGENCY_LANDING, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        setEmergencyLanding(tag.getBoolean(EMERGENCY_LANDING_TAG));
        lowManaWarningShown = tag.getBoolean(LOW_MANA_WARNING_SHOWN_TAG);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putBoolean(EMERGENCY_LANDING_TAG, isEmergencyLanding());
        tag.putBoolean(LOW_MANA_WARNING_SHOWN_TAG, lowManaWarningShown);
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        decayDamageAnimation();

        if (isVehicle()) {
            if (level().isClientSide && isControlledByLocalInstance()) {
                applyControlledMovement();
            } else if (!level().isClientSide && getControllingPassenger() instanceof Player player) {
                tickServerManaState(player);
                setDeltaMovement(Vec3.ZERO);
            }
        } else {
            if (!level().isClientSide) {
                resetFlightState();
            }
            applyUnoccupiedMovement();
        }

        checkInsideBlocks();
    }

    private void decayDamageAnimation() {
        if (getHurtTime() > 0) {
            setHurtTime(getHurtTime() - 1);
        }
        if (getDamage() > 0.0F) {
            setDamage(Math.max(0.0F, getDamage() - 1.0F));
        }
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 10;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    private void applyControlledMovement() {
        turnSpeed = Mth.clamp((turnSpeed + localStrafeInput * TURN_ACCELERATION) * TURN_DAMPING,
                -MAX_TURN_SPEED, MAX_TURN_SPEED);
        setYRot(getYRot() + turnSpeed);

        var movement = getDeltaMovement();
        var yaw = getYRot() * Mth.DEG_TO_RAD;
        var forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        // 高所作業で停止しやすくしつつ、入力中はボート相当の滑らかな加速を維持する。
        var horizontalDamping = Math.abs(localForwardInput) > INPUT_EPSILON
                ? POWERED_HORIZONTAL_DAMPING
                : COAST_HORIZONTAL_DAMPING;
        var horizontal = new Vec3(movement.x, 0.0D, movement.z)
                .scale(horizontalDamping)
                .add(forward.scale(localForwardInput * HORIZONTAL_ACCELERATION));
        var maxHorizontalSpeed = isEmergencyLanding()
                ? EMERGENCY_MAX_HORIZONTAL_SPEED
                : MAX_HORIZONTAL_SPEED;
        if (horizontal.length() > maxHorizontalSpeed) {
            horizontal = horizontal.normalize().scale(maxHorizontalSpeed);
        }

        var emergencyLanding = isEmergencyLanding();
        var verticalTarget = localAscending ? MAX_VERTICAL_SPEED : descendingInput ? -MAX_VERTICAL_SPEED : 0.0D;
        var vertical = emergencyLanding
                ? -MAX_VERTICAL_SPEED
                : Mth.clamp(movement.y + Mth.clamp(verticalTarget - movement.y,
                        -VERTICAL_ACCELERATION, VERTICAL_ACCELERATION), -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
    }

    private void applyUnoccupiedMovement() {
        var movement = getDeltaMovement();
        var horizontal = new Vec3(movement.x * COAST_HORIZONTAL_DAMPING, 0.0D,
                movement.z * COAST_HORIZONTAL_DAMPING);
        var vertical = movement.y;

        if (isInWaterOrBubble() || isInLava() || !level().noCollision(this, getBoundingBox().deflate(0.01D))) {
            vertical = Math.min(MAX_UNMOUNTED_RISE_SPEED, vertical + 0.03D);
        } else {
            var surface = FloatmountBroomSurfaceScanner.findSurfaceBelow(level(), getX(), getY(), getZ(), 4, true);
            if (surface.isPresent()) {
                var error = surface.getAsDouble() + HOVER_HEIGHT - getY();
                vertical = Mth.clamp(error * 0.2D + vertical * 0.6D,
                        -MAX_UNMOUNTED_FALL_SPEED, MAX_UNMOUNTED_RISE_SPEED);
                if (Math.abs(error) < 0.01D && Math.abs(vertical) < 0.01D) {
                    vertical = 0.0D;
                }
            } else {
                vertical = Math.max(-MAX_UNMOUNTED_FALL_SPEED, vertical - 0.02D);
            }
        }

        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        turnSpeed = turnSpeed * (float)COAST_HORIZONTAL_DAMPING;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }

        setHurtDirection(-getHurtDirection());
        setHurtTime(10);
        setDamage(getDamage() + amount * 10.0F);
        markHurt();
        gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());

        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            breakBroom(false);
        } else if (getDamage() > 40.0F) {
            breakBroom(true);
        }
        return true;
    }

    private void breakBroom(boolean dropItem) {
        if (breaking || isRemoved()) {
            return;
        }
        breaking = true;
        ejectPassengers();
        if (dropItem && level() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        }
        discard();
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (isVehicle()) {
                return InteractionResult.PASS;
            }
            if (!level().isClientSide) {
                recoverAsItem(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (isVehicle() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            if (!canMountWithCurrentMana(player)) {
                return InteractionResult.CONSUME;
            }
            resetFlightState();
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    private void recoverAsItem(Player player) {
        var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        discard();
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && !isVehicle()) {
            resetFlightState();
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        clampPassengerRotation(passenger);
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity passenger,
                                                         @NotNull EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, RIDER_ATTACHMENT_Y, 0.0D);
    }

    private void clampPassengerRotation(Entity passenger) {
        var center = getYRot() - 90.0F;
        passenger.setYRot(center + Mth.clamp(Mth.wrapDegrees(passenger.getYRot() - center), -105.0F, 105.0F));
        passenger.setYHeadRot(passenger.getYRot());
        passenger.setYBodyRot(center);
    }

    @Override
    public void onPassengerTurned(@NotNull Entity passenger) {
        clampPassengerRotation(passenger);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull net.minecraft.world.entity.LivingEntity passenger) {
        var yaw = getYRot() * Mth.DEG_TO_RAD;
        var left = new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw)).scale(0.5D);
        var candidate = position().add(left);
        for (var pose : passenger.getDismountPoses()) {
            var dismount = new Vec3(candidate.x, getY(), candidate.z);
            if (net.minecraft.world.entity.vehicle.DismountHelper.canDismountTo(level(), dismount, passenger, pose)) {
                passenger.setPose(pose);
                return dismount;
            }
        }
        var fallback = super.getDismountLocationForPassenger(passenger);
        return level().noCollision(passenger, passenger.getBoundingBox().move(fallback.subtract(passenger.position())))
                ? fallback
                : new Vec3(getX(), getBoundingBox().maxY, getZ());
    }

    public boolean isDangerousDismount() {
        if (isEmergencyLanding() || isInLava()) {
            return true;
        }
        var surface = FloatmountBroomSurfaceScanner.findSurfaceBelow(level(), getX(), getY(), getZ(), 3, false);
        return surface.isEmpty() || getY() - surface.getAsDouble() >= DANGEROUS_HEIGHT;
    }

    public void setLocalInput(float strafe, float forward, boolean ascending, boolean descending) {
        localStrafeInput = Mth.clamp(strafe, -1.0F, 1.0F);
        localForwardInput = Mth.clamp(forward, -1.0F, 1.0F);
        localAscending = ascending;
        descendingInput = descending;
    }

    public void acceptServerInput(
            Player player,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending
    ) {
        if (player.getVehicle() != this || getControllingPassenger() != player) {
            return;
        }

        // 箒は操作感を優先してvanillaの乗り物と同様にclient予測を維持する。
        // serverは所有者を検証した入力でマナと緊急状態を管理するが、移動そのもののserver権威化は意図的に別課題とする。
        localStrafeInput = sanitizeInput(strafe);
        serverForwardInput = sanitizeInput(forward);
        serverAscending = ascending;
        descendingInput = descending;
        lastServerInputGameTime = level().getGameTime();
    }

    private boolean canMountWithCurrentMana(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        var requiredMana = ApprenticeCodexServerConfig.floatmountBroomConfig().normalFlightManaThreshold();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getMana() >= requiredMana) {
            return true;
        }
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.floatmount_broom.insufficient_mana",
                requiredMana
        ).withStyle(ChatFormatting.RED), true);
        return false;
    }

    private void tickServerManaState(Player player) {
        if (player.getAbilities().instabuild) {
            resetFlightState();
            return;
        }

        var now = level().getGameTime();
        if (lastServerInputGameTime == Long.MIN_VALUE
                || now - lastServerInputGameTime > SERVER_INPUT_TIMEOUT_TICKS) {
            clearServerInput();
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var config = ApprenticeCodexServerConfig.floatmountBroomConfig();
        var mana = magicData == null ? 0.0F : magicData.getMana();
        if (isEmergencyLanding()) {
            if (mana >= config.normalFlightManaThreshold()) {
                setEmergencyLanding(false);
                lowManaWarningShown = false;
                player.displayClientMessage(Component.translatable(
                        "ui.apprenticecodex.floatmount_broom.recover_emergency_landing"
                ).withStyle(ChatFormatting.GREEN), true);
            }
            return;
        }

        var manaCost = movementManaCost(config);
        if (manaCost > 0.0F) {
            var updatedMana = Math.max(0.0F, mana - manaCost);
            if (magicData != null && updatedMana != mana) {
                magicData.setMana(updatedMana);
                syncMana(player, magicData);
            }
            if (updatedMana <= 0.0F) {
                setEmergencyLanding(true);
                player.displayClientMessage(Component.translatable(
                        "ui.apprenticecodex.floatmount_broom.warning_emergency_landing"
                ).withStyle(ChatFormatting.RED), true);
                return;
            }
            mana = updatedMana;
        }

        if (!lowManaWarningShown
                && mana <= config.lowManaWarningThreshold()
                && mana < config.normalFlightManaThreshold()) {
            lowManaWarningShown = true;
            player.displayClientMessage(Component.translatable(
                    "ui.apprenticecodex.floatmount_broom.warning_low_mana"
            ).withStyle(ChatFormatting.YELLOW), true);
        } else if (lowManaWarningShown && mana >= config.normalFlightManaThreshold()) {
            lowManaWarningShown = false;
        }
    }

    private float movementManaCost(FloatmountBroomServerConfig.Values config) {
        var horizontal = Math.abs(serverForwardInput) > INPUT_EPSILON;
        if (horizontal && serverAscending) {
            return Math.max(0.0F, (float)config.horizontalAscendingManaCostPerTick());
        }
        if (horizontal) {
            return Math.max(0.0F, (float)config.horizontalManaCostPerTick());
        }
        return serverAscending ? Math.max(0.0F, (float)config.ascendingManaCostPerTick()) : 0.0F;
    }

    private static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    private static void syncMana(Player player, MagicData magicData) {
        if (player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    private void resetFlightState() {
        setEmergencyLanding(false);
        lowManaWarningShown = false;
        clearServerInput();
    }

    private void clearServerInput() {
        localStrafeInput = 0.0F;
        serverForwardInput = 0.0F;
        serverAscending = false;
        descendingInput = false;
        lastServerInputGameTime = Long.MIN_VALUE;
    }

    public boolean isEmergencyLanding() {
        return entityData.get(EMERGENCY_LANDING);
    }

    private void setEmergencyLanding(boolean value) {
        entityData.set(EMERGENCY_LANDING, value);
    }

    public boolean isBreaking() {
        return breaking;
    }

    public int getHurtTime() {
        return entityData.get(HURT_TIME);
    }

    public void setHurtTime(int value) {
        entityData.set(HURT_TIME, value);
    }

    public int getHurtDirection() {
        return entityData.get(HURT_DIRECTION);
    }

    public void setHurtDirection(int value) {
        entityData.set(HURT_DIRECTION, value);
    }

    public float getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setDamage(float value) {
        entityData.set(DAMAGE, value);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(@NotNull Entity other) {
        return (other.canBeCollidedWith() || other.isPushable()) && !isPassengerOfSameVehicle(other);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public void animateHurt(float yaw) {
        setHurtDirection(-getHurtDirection());
        setHurtTime(10);
        setDamage(getDamage() * 11.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(isVehicle() ? MOUNT : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
