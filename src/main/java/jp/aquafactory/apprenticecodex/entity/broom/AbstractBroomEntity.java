package jp.aquafactory.apprenticecodex.entity.broom;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.item.broom.BroomDeploymentState;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractBroomEntity extends Entity implements GeoEntity {
    public static final float WIDTH = 0.8F;
    public static final float HEIGHT = 0.5F;
    public static final int DISMOUNT_CONFIRM_TICKS = 30;
    public static final double DANGEROUS_HEIGHT = 2.0D;
    public static final int DAMAGE_SCALE = 50;
    public static final int DURABILITY_STEPS = 20;

    private static final double HOVER_HEIGHT = 0.5D;
    private static final double VERTICAL_ACCELERATION = 0.05D;
    private static final float TURN_ACCELERATION = 1.0F;
    private static final float MAX_TURN_SPEED = 10.0F;
    private static final double POWERED_HORIZONTAL_DAMPING = 0.9D;
    private static final double COAST_HORIZONTAL_DAMPING = 0.85D;
    private static final float TURN_DAMPING = 0.9F;
    private static final float INPUT_EPSILON = 1.0e-4F;
    private static final int SERVER_INPUT_TIMEOUT_TICKS = 30;
    private static final int SERVER_RIDING_MOVEMENT_GRACE_TICKS = 2;
    private static final double MOVEMENT_EPSILON_SQR = 1.0e-8D;
    private static final int DAMAGE_RECOVERY_INTERVAL_TICKS = 10;
    private static final int RIDER_VIBRATION_INTERVAL_TICKS = 10;
    private static final int RETRIEVE_HELP_COOLDOWN_TICKS = 40;
    private static final double RETRIEVE_HELP_DISTANCE_SQR = 16.0D;
    private static final int DEFAULT_MAX_DAMAGE = 1000;
    private static final double REAR_PARTICLE_DISTANCE = 1.2D;
    private static final double REAR_PARTICLE_Y_OFFSET = 0.1D;
    private static final double FLIGHT_PARTICLE_POSITION_SPREAD = 0.25D;
    private static final double FLIGHT_PARTICLE_VERTICAL_SPREAD = 0.25D;
    private static final int NORMAL_SPARK_INTERVAL_TICKS = 2;
    private static final int NORMAL_RHOMBUS_INTERVAL_TICKS = 5;
    private static final int FORCED_LANDING_SPARK_INTERVAL_TICKS = 4;
    private static final int FORCED_LANDING_RHOMBUS_INTERVAL_TICKS = 10;
    private static final int FORWARD_SPARK_COUNT = 2;
    private static final int FORWARD_RHOMBUS_INTERVAL_TICKS = 2;
    private static final String EMERGENCY_LANDING_TAG = "EmergencyLanding";
    private static final String LOW_MANA_WARNING_SHOWN_TAG = "LowManaWarningShown";
    private static final String DAMAGE_TAG = "Damage";
    private static final String DAMAGED_TAG = "Damaged";
    private static final String CALLED_OWNER_TAG = "CallBroomOwner";
    private static final String BROOM_ITEM_STACK_TAG = "BroomItemStack";
    /**
     * 箒のEntity原点から乗員のvehicle attachmentまでの基準高さ。
     * 箒ごとの姿勢差は{@link #passengerAttachmentYOffset()}で追加補正する。
     */
    public static final float RIDER_ATTACHMENT_Y = 0.05F;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation MOUNT = RawAnimation.begin().thenLoop("mount");

    private static final EntityDataAccessor<Integer> DAMAGE =
            SynchedEntityData.defineId(AbstractBroomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_DAMAGE =
            SynchedEntityData.defineId(AbstractBroomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MANA_EMERGENCY_LANDING =
            SynchedEntityData.defineId(AbstractBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DAMAGED =
            SynchedEntityData.defineId(AbstractBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> BROOM_ITEM_STACK =
            SynchedEntityData.defineId(AbstractBroomEntity.class, EntityDataSerializers.ITEM_STACK);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private float localForwardInput;
    private float localStrafeInput;
    private boolean localAscending;
    private boolean descendingInput;
    private float serverForwardInput;
    private boolean serverAscending;
    private long lastServerInputGameTime = Long.MIN_VALUE;
    private long lastRetrieveHelpGameTime = Long.MIN_VALUE;
    private long lastAcceptedDamageGameTime = Long.MIN_VALUE;
    private long lastRiderVibrationGameTime = Long.MIN_VALUE;
    private boolean lowManaWarningShown;
    private float turnSpeed;
    private boolean breaking;
    private @Nullable UUID calledOwnerUuid;
    private @Nullable ServerPlayer cachedCalledOwner;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private Vec3 lastServerRidingPosition = Vec3.ZERO;
    private Vec3 lastServerRidingMovement = Vec3.ZERO;
    private long lastServerRidingPositionGameTime = Long.MIN_VALUE;
    private long lastServerRidingMovementGameTime = Long.MIN_VALUE;
    private double bubbleColumnVerticalInfluence;

    protected AbstractBroomEntity(EntityType<?> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    protected abstract Item getRecoveryItem();

    protected abstract BroomMessageKeys messageKeys();

    protected boolean requiresMountMana() {
        return true;
    }

    protected boolean isWaterMovementPenaltyActive() {
        return false;
    }

    protected boolean managesBubbleColumnMovement() {
        return false;
    }

    protected boolean ignoresBubbleColumnMovement() {
        return false;
    }

    protected void onBroomItemStackChanged(ItemStack stack) {
    }

    public abstract Component createControlHelpMessage();

    protected record BroomMessageKeys(
            String warningDismount,
            String retrieveHelp,
            String warningDamage,
            String cannotMountDamaged,
            Optional<String> insufficientMana,
            String warningLowMana,
            String manaDepleted,
            String manaRecovered
    ) {
        public BroomMessageKeys {
            Objects.requireNonNull(warningDismount);
            Objects.requireNonNull(retrieveHelp);
            Objects.requireNonNull(warningDamage);
            Objects.requireNonNull(cannotMountDamaged);
            Objects.requireNonNull(insufficientMana);
            Objects.requireNonNull(warningLowMana);
            Objects.requireNonNull(manaDepleted);
            Objects.requireNonNull(manaRecovered);
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DAMAGE, 0);
        entityData.define(MAX_DAMAGE, DEFAULT_MAX_DAMAGE);
        entityData.define(MANA_EMERGENCY_LANDING, false);
        entityData.define(DAMAGED, false);
        entityData.define(BROOM_ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        var maxDamage = ApprenticeCodexServerConfig.floatmountBroomConfig().maxDamage();
        setMaxDamage(maxDamage);
        setDamage(Mth.clamp(tag.getInt(DAMAGE_TAG), 0, maxDamage));
        setDamaged(tag.getBoolean(DAMAGED_TAG) || getDamage() >= maxDamage);
        setManaEmergencyLanding(tag.getBoolean(EMERGENCY_LANDING_TAG));
        lowManaWarningShown = tag.getBoolean(LOW_MANA_WARNING_SHOWN_TAG);
        calledOwnerUuid = tag.hasUUID(CALLED_OWNER_TAG) ? tag.getUUID(CALLED_OWNER_TAG) : null;
        cachedCalledOwner = null;
        setBroomItemStack(tag.contains(BROOM_ITEM_STACK_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(BROOM_ITEM_STACK_TAG))
                : ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putInt(DAMAGE_TAG, getDamage());
        tag.putBoolean(DAMAGED_TAG, isDamaged());
        tag.putBoolean(EMERGENCY_LANDING_TAG, isManaEmergencyLanding());
        tag.putBoolean(LOW_MANA_WARNING_SHOWN_TAG, lowManaWarningShown);
        if (calledOwnerUuid != null) {
            tag.putUUID(CALLED_OWNER_TAG, calledOwnerUuid);
        }
        tag.put(BROOM_ITEM_STACK_TAG, getBroomItemStack().save(new CompoundTag()));
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        if (level().isClientSide) {
            spawnDamageParticles();
            spawnFlightParticles();
        } else {
            tickServerDamageState();
            if (isRemoved()) {
                return;
            }
            if (calledOwnerUuid != null) {
                var owner = getCalledOwner();
                if (owner == null || CallBroomDeploymentManager.shouldRecall(this, owner)) {
                    recallWithoutItem();
                    return;
                }
            }
            tickRiderVibration();
        }

        var movedThisTick = false;
        if (isVehicle()) {
            if (level().isClientSide && isControlledByLocalInstance()) {
                applyControlledMovement();
                movedThisTick = true;
            } else if (!level().isClientSide && getControllingPassenger() instanceof Player player) {
                observeServerRidingMovement();
                tickControlledServer(player);
                setDeltaMovement(Vec3.ZERO);
            }
        } else {
            if (!level().isClientSide) {
                resetRidingState();
            }
            applyUnoccupiedMovement();
            movedThisTick = true;
        }

        // move()自身がblock内判定を行うため、移動しないsideだけ明示的に補う。
        // これにより操作client・server・追跡clientのcallback回数を1tickにつき1回へそろえる。
        if (!movedThisTick) {
            checkInsideBlocks();
        }
        if (!level().isClientSide && getRemainingFireTicks() > 0) {
            // 接触中の火炎・溶岩ダメージは受けるが、離れた後まで続く炎上は飛行視界を妨げるため残さない。
            clearFire();
        }
    }

    private void tickRiderVibration() {
        if (!(getControllingPassenger() instanceof Player player)) {
            lastRiderVibrationGameTime = Long.MIN_VALUE;
            return;
        }

        var gameTime = level().getGameTime();
        if (lastRiderVibrationGameTime != Long.MIN_VALUE
                && gameTime - lastRiderVibrationGameTime < RIDER_VIBRATION_INTERVAL_TICKS) {
            return;
        }

        // Deep Sensorを含むplayer由来の無音化規則へ統一するため、箒ではなく騎乗者を振動源にする。
        player.gameEvent(GameEvent.ELYTRA_GLIDE);
        lastRiderVibrationGameTime = gameTime;
    }

    private void tickServerDamageState() {
        var config = ApprenticeCodexServerConfig.floatmountBroomConfig();
        setMaxDamage(config.maxDamage());
        if (!isDamaged() && getDamage() >= getMaxDamage()) {
            enterDamagedState();
        }
        if (isDamaged()) {
            // 設定変更や旧保存データでも、復帰不能な損傷状態は常に現在の最大値へ固定する。
            setDamage(getMaxDamage());
            tryItemizeBelowWorld();
            return;
        }
        if (tickCount % DAMAGE_RECOVERY_INTERVAL_TICKS == 0 && getDamage() > 0) {
            setDamage(Math.max(0, getDamage() - config.damageRecoveryAmount()));
        }
    }

    private void spawnDamageParticles() {
        var stage = getDamageStage();
        if (getDamageRatio() <= 0.5F) {
            return;
        }

        var smokeInterval = stage >= 4 ? 2 : stage == 3 ? 4 : 8;
        var rear = getRearParticlePosition();
        if (tickCount % smokeInterval == 0) {
            level().addParticle(
                    ParticleTypes.SMOKE,
                    rear.x + (random.nextDouble() - 0.5D) * 0.2D,
                    rear.y + random.nextDouble() * 0.15D,
                    rear.z + (random.nextDouble() - 0.5D) * 0.2D,
                    0.0D,
                    0.015D,
                    0.0D
            );
        }
        if (stage >= 4 && tickCount % 10 == 0) {
            level().addParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    rear.x,
                    rear.y,
                    rear.z,
                    0.0D,
                    0.025D,
                    0.0D
            );
        }
    }

    private Vec3 getRearParticlePosition() {
        return position().subtract(getForwardDirection().scale(REAR_PARTICLE_DISTANCE))
                .add(0.0D, REAR_PARTICLE_Y_OFFSET, 0.0D);
    }

    protected void spawnFlightParticles() {
        var forcedLanding = isForcedLanding();
        // FloatmountのW増量は見た目だけのため、serverや他clientへ入力状態を同期してまで再現しない。
        var forwardBoost = !forcedLanding && isControlledByLocalInstance() && localForwardInput > INPUT_EPSILON;
        spawnDefaultFlightParticles(forwardBoost);
    }

    protected final void spawnDefaultFlightParticles(boolean forwardBoost) {
        var forcedLanding = isForcedLanding();
        if (forwardBoost) {
            spawnFlightParticles(ParticleRegistry.ADDITIVE_SPARK.get(), FORWARD_SPARK_COUNT);
            if (tickCount % FORWARD_RHOMBUS_INTERVAL_TICKS == 0) {
                spawnFlightParticles(ParticleRegistry.ADDITIVE_RHOMBUS.get(), 1);
            }
            return;
        }

        var sparkInterval = forcedLanding ? FORCED_LANDING_SPARK_INTERVAL_TICKS : NORMAL_SPARK_INTERVAL_TICKS;
        var rhombusInterval = forcedLanding ? FORCED_LANDING_RHOMBUS_INTERVAL_TICKS : NORMAL_RHOMBUS_INTERVAL_TICKS;
        if (tickCount % sparkInterval == 0) {
            spawnFlightParticles(ParticleRegistry.ADDITIVE_SPARK.get(), 1);
        }
        if (tickCount % rhombusInterval == 0) {
            spawnFlightParticles(ParticleRegistry.ADDITIVE_RHOMBUS.get(), 1);
        }
    }

    private void spawnFlightParticles(ParticleType<AdditiveGlowParticleOptions> particleType, int count) {
        var base = getRearParticlePosition();
        var backward = getForwardDirection().scale(-1.0D);
        for (var i = 0; i < count; ++i) {
            var color = randomManaJetColor(random);
            var speed = 0.03D + random.nextDouble() * 0.02D;
            var velocity = backward.scale(speed).add(
                    (random.nextDouble() - 0.5D) * 0.012D,
                    (random.nextDouble() - 0.5D) * 0.008D,
                    (random.nextDouble() - 0.5D) * 0.012D
            );
            level().addParticle(
                    createFlightParticleOptions(particleType, color, random),
                    base.x + (random.nextDouble() - 0.5D) * FLIGHT_PARTICLE_POSITION_SPREAD,
                    base.y + (random.nextDouble() - 0.5D) * FLIGHT_PARTICLE_VERTICAL_SPREAD,
                    base.z + (random.nextDouble() - 0.5D) * FLIGHT_PARTICLE_POSITION_SPREAD,
                    velocity.x,
                    velocity.y,
                    velocity.z
            );
        }
    }

    private static AdditiveGlowParticleOptions createFlightParticleOptions(
            ParticleType<AdditiveGlowParticleOptions> particleType,
            Vector3f color,
            RandomSource random
    ) {
        var spark = particleType == ParticleRegistry.ADDITIVE_SPARK.get();
        return new AdditiveGlowParticleOptions(
                particleType,
                spark ? 0.18F + random.nextFloat() * 0.08F : 0.14F + random.nextFloat() * 0.06F,
                color.x(),
                color.y(),
                color.z(),
                2,
                spark ? 8 : 10,
                3,
                0.75F,
                spark ? 1.25F : 1.15F,
                spark ? 0.7F : 0.55F,
                spark ? 1.0F : 0.9F,
                spark ? 0.02F : 0.04F,
                spark ? 0.58F : 0.62F,
                spark ? 0.45F : 0.35F,
                true
        );
    }

    private static Vector3f randomManaJetColor(RandomSource random) {
        var t = random.nextFloat();
        return new Vector3f(
                Mth.lerp(t, 0.28F, 0.62F),
                Mth.lerp(t, 0.78F, 0.36F),
                Mth.lerp(t, 1.0F, 0.95F)
        );
    }

    protected final Vec3 getForwardDirection() {
        var yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            var step = 1.0D / lerpSteps;
            setPos(
                    Mth.lerp(step, getX(), lerpX),
                    Mth.lerp(step, getY(), lerpY),
                    Mth.lerp(step, getZ(), lerpZ)
            );
            setYRot(Mth.rotLerp((float) step, getYRot(), (float) lerpYRot));
            setXRot(Mth.lerp((float) step, getXRot(), (float) lerpXRot));
            lerpSteps--;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 10;
    }

    protected void applyControlledMovement() {
        turnSpeed = Mth.clamp((turnSpeed + localStrafeInput * TURN_ACCELERATION) * TURN_DAMPING,
                -MAX_TURN_SPEED, MAX_TURN_SPEED);
        setYRot(getYRot() + turnSpeed);

        var movement = getDeltaMovement();
        var bubbleInfluence = currentBubbleColumnVerticalInfluence();
        var controlledVerticalMovement = movement.y - bubbleInfluence;
        var waterPenaltyActive = isWaterMovementPenaltyActive();
        var overdriveEnabled = isOverdriveEnabled();
        var yaw = getYRot() * Mth.DEG_TO_RAD;
        var forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        // 高所作業で停止しやすくしつつ、入力中はボート相当の滑らかな加速を維持する。
        var horizontalDamping = Math.abs(localForwardInput) > INPUT_EPSILON
                ? POWERED_HORIZONTAL_DAMPING
                : COAST_HORIZONTAL_DAMPING;
        var horizontal = new Vec3(movement.x, 0.0D, movement.z)
                .scale(horizontalDamping)
                .add(forward.scale(localForwardInput
                        * FloatmountBroomMovement.horizontalAcceleration(waterPenaltyActive, overdriveEnabled)));
        var maxHorizontalSpeed = FloatmountBroomMovement.maximumHorizontalSpeed(
                isForcedLanding(), waterPenaltyActive, overdriveEnabled
        );
        if (horizontal.length() > maxHorizontalSpeed) {
            horizontal = horizontal.normalize().scale(maxHorizontalSpeed);
        }

        var forcedLanding = isForcedLanding();
        var maxVerticalSpeed = FloatmountBroomMovement.maximumVerticalSpeed(waterPenaltyActive);
        var verticalTarget = localAscending ? maxVerticalSpeed : descendingInput ? -maxVerticalSpeed : 0.0D;
        var vertical = forcedLanding
                ? -maxVerticalSpeed
                : Mth.clamp(controlledVerticalMovement + Mth.clamp(verticalTarget - controlledVerticalMovement,
                        -VERTICAL_ACCELERATION, VERTICAL_ACCELERATION), -maxVerticalSpeed, maxVerticalSpeed);
        vertical += bubbleInfluence;
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
    }

    protected void applyUnoccupiedMovement() {
        var movement = getDeltaMovement();
        var bubbleInfluence = currentBubbleColumnVerticalInfluence();
        var controlledVerticalMovement = movement.y - bubbleInfluence;
        var waterPenaltyActive = isWaterMovementPenaltyActive();
        var overdriveEnabled = isOverdriveEnabled();
        var horizontal = new Vec3(movement.x * COAST_HORIZONTAL_DAMPING, 0.0D,
                movement.z * COAST_HORIZONTAL_DAMPING);
        if (waterPenaltyActive) {
            var maxHorizontalSpeed = FloatmountBroomMovement.maximumHorizontalSpeed(
                    false, true, overdriveEnabled
            );
            if (horizontal.length() > maxHorizontalSpeed) {
                horizontal = horizontal.normalize().scale(maxHorizontalSpeed);
            }
        }
        var vertical = controlledVerticalMovement;

        // 損傷は有人飛行を制限する状態とし、無人時は溶岩などから回収できるよう通常の浮遊を維持する。
        if (isInWaterOrBubble() || isInLava() || !level().noCollision(this, getBoundingBox().deflate(0.01D))) {
            var maximumVerticalSpeed = FloatmountBroomMovement.maximumUnoccupiedVerticalSpeed(waterPenaltyActive);
            vertical = waterPenaltyActive
                    ? Mth.clamp(vertical + 0.03D, -maximumVerticalSpeed, maximumVerticalSpeed)
                    : Math.min(maximumVerticalSpeed, vertical + 0.03D);
        } else {
            var surface = BroomSurfaceScanner.findSurfaceBelow(level(), getX(), getY(), getZ(), 4, true);
            if (surface.isPresent()) {
                var error = surface.get().y() + HOVER_HEIGHT - getY();
                vertical = Mth.clamp(error * 0.2D + vertical * 0.6D,
                        -FloatmountBroomMovement.MAX_UNMOUNTED_FALL_SPEED,
                        FloatmountBroomMovement.MAX_UNMOUNTED_RISE_SPEED);
                if (Math.abs(error) < 0.01D && Math.abs(vertical) < 0.01D) {
                    vertical = 0.0D;
                }
            } else {
                vertical = Math.max(-FloatmountBroomMovement.MAX_UNMOUNTED_FALL_SPEED, vertical - 0.02D);
            }
        }
        vertical += bubbleInfluence;

        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        turnSpeed = turnSpeed * (float)COAST_HORIZONTAL_DAMPING;
    }

    protected double maximumInheritedDismountHorizontalSpeed() {
        return FloatmountBroomMovement.maximumHorizontalSpeed(
                false, isWaterMovementPenaltyActive(), isOverdriveEnabled()
        );
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (AbstractBroomItem.isFirewardEnabled(getBroomItemStack()) && source.is(DamageTypeTags.IS_FIRE)) {
            // Fireward Ring本来の耐性に合わせ、溶岩を含む火炎ダメージと付随する炎上を箒だけに無効化する。
            clearFire();
            return false;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }

        maybeShowRetrieveHelp(source);
        var config = ApprenticeCodexServerConfig.floatmountBroomConfig();
        var ignoresDamageIFrame = isIgnoredByDamageIFrame(source, config);
        if (!ignoresDamageIFrame && isDamageIFrameActive(config)) {
            return false;
        }

        var scaledDamage = Math.max(0.0D, amount) * DAMAGE_SCALE;
        var addedDamage = scaledDamage >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Mth.floor(scaledDamage);
        if (addedDamage > 0 && !isDamaged()) {
            var updatedDamage = Math.min((long)getMaxDamage(), (long)getDamage() + addedDamage);
            setDamage((int)updatedDamage);
            if (!ignoresDamageIFrame && config.damageIFrameTicks() > 0) {
                lastAcceptedDamageGameTime = level().getGameTime();
            }
            if (getDamage() >= getMaxDamage()) {
                enterDamagedState();
            } else {
                playBroomSound(SoundRegistry.VANILLA_BROOM_DAMAGE.get());
            }
        }
        gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        tryItemizeBelowWorld();
        return true;
    }

    private boolean isDamageIFrameActive(FloatmountBroomServerConfig.Values config) {
        if (config.damageIFrameTicks() <= 0 || lastAcceptedDamageGameTime == Long.MIN_VALUE) {
            return false;
        }
        var elapsedTicks = level().getGameTime() - lastAcceptedDamageGameTime;
        return elapsedTicks >= 0L && elapsedTicks < config.damageIFrameTicks();
    }

    private static boolean isIgnoredByDamageIFrame(
            DamageSource source,
            FloatmountBroomServerConfig.Values config
    ) {
        if (source.is(DamageTypeTagGenerator.IGNORES_FLOATMOUNT_BROOM_IFRAME)) {
            return true;
        }
        return source.typeHolder().unwrapKey()
                .map(key -> config.iframeIgnoredDamageTypes().contains(key.location()))
                .orElse(false);
    }

    private void maybeShowRetrieveHelp(DamageSource source) {
        if (isVehicle()
                || !(source.getEntity() instanceof Player player)
                || source.getDirectEntity() != player
                || player.distanceToSqr(this) > RETRIEVE_HELP_DISTANCE_SQR) {
            return;
        }
        var now = level().getGameTime();
        if (lastRetrieveHelpGameTime != Long.MIN_VALUE
                && now - lastRetrieveHelpGameTime < RETRIEVE_HELP_COOLDOWN_TICKS) {
            return;
        }
        lastRetrieveHelpGameTime = now;
        player.displayClientMessage(Component.translatable(
                messageKeys().retrieveHelp(),
                Component.keybind("key.use")
        ).withStyle(ChatFormatting.YELLOW), true);
    }

    private void enterDamagedState() {
        if (isDamaged()) {
            return;
        }
        setDamage(getMaxDamage());
        setDamaged(true);
        clearRidingInput();
        playBroomSound(SoundRegistry.VANILLA_BROOM_CRITICAL_DAMAGE.get());
        playBroomSound(SoundRegistry.VANILLA_BROOM_EMERGENCY.get());
        if (getControllingPassenger() instanceof Player player) {
            player.displayClientMessage(Component.translatable(
                    messageKeys().warningDamage()
            ).withStyle(ChatFormatting.RED), true);
        }
    }

    private void tryItemizeBelowWorld() {
        if (!isDamaged() || getY() >= level().getMinBuildHeight()) {
            return;
        }
        itemizeBroom();
    }

    private void itemizeBroom() {
        if (breaking || isRemoved()) {
            return;
        }
        if (calledOwnerUuid != null) {
            recallWithoutItem();
            return;
        }
        breaking = true;
        ejectPassengers();
        if (level() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(createRecoveredStack());
        }
        discard();
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        if (calledOwnerUuid != null && !isOwnedBy(player)) {
            // 魔法で展開した箒は元アイテムが所有者の背中に残るため、第三者には操作させない。
            return InteractionResult.CONSUME;
        }
        if (player.isSecondaryUseActive()) {
            if (isVehicle()) {
                return InteractionResult.PASS;
            }
            if (!level().isClientSide) {
                if (calledOwnerUuid != null) {
                    recallWithoutItem();
                } else {
                    // 通常設置の箒はボート型の共有物として所有者を保存しないため、空席なら誰でも回収できる。
                    recoverAsItem(player);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (isVehicle() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            if (isDamaged()) {
                player.displayClientMessage(Component.translatable(
                        messageKeys().cannotMountDamaged()
                ).withStyle(ChatFormatting.RED), true);
                playPlayerNotification(player, SoundRegistry.VANILLA_BROOM_MOUNT_REJECT.get());
                return InteractionResult.CONSUME;
            }
            if (!canMountWithCurrentMana(player)) {
                return InteractionResult.CONSUME;
            }
            resetRidingState();
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    private void recoverAsItem(Player player) {
        var stack = createRecoveredStack();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        playBroomSound(SoundRegistry.VANILLA_BROOM_RECONSTRUCT.get());
        discard();
    }

    private ItemStack createRecoveredStack() {
        // 回収はEntity側の損傷だけを修復し、展開前の調整内容とスクロールはItemStackごと引き継ぐ。
        var stack = getBroomItemStack();
        var customName = getCustomName();
        if (customName != null) {
            stack.setHoverName(customName);
        } else {
            stack.resetHoverName();
        }
        BroomDeploymentState.clear(stack);
        return stack;
    }

    public final void setBroomItemStack(ItemStack stack) {
        var storedStack = stack.is(getRecoveryItem())
                ? stack.copyWithCount(1)
                : new ItemStack(getRecoveryItem());
        // CallBroomの一時的なEntity UUIDは回収物や同期用copyへ持ち込まない。
        BroomDeploymentState.clear(storedStack);
        entityData.set(BROOM_ITEM_STACK, storedStack);
        onBroomItemStackChanged(storedStack);
    }

    public final ItemStack getBroomItemStack() {
        var stack = entityData.get(BROOM_ITEM_STACK);
        return stack.is(getRecoveryItem()) ? stack.copyWithCount(1) : new ItemStack(getRecoveryItem());
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return !isDamaged()
                && passenger instanceof Player player
                && (calledOwnerUuid == null || isOwnedBy(player))
                && getPassengers().isEmpty();
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && !isVehicle()) {
            lastRiderVibrationGameTime = Long.MIN_VALUE;
            var inheritedMovement = takeServerRidingMovement();
            resetRidingState();
            setDeltaMovement(inheritedMovement);
            hasImpulse = true;
            if (calledOwnerUuid != null && isDamaged() && !breaking) {
                recallWithoutItem();
            }
        }
    }

    public final void setCalledOwner(ServerPlayer owner) {
        calledOwnerUuid = owner.getUUID();
        cachedCalledOwner = owner;
    }

    public final boolean isCalledBroom() {
        return calledOwnerUuid != null;
    }

    public final boolean isOwnedBy(Player player) {
        return isOwnedBy(player.getUUID());
    }

    public final boolean isOwnedBy(UUID ownerId) {
        return calledOwnerUuid != null && calledOwnerUuid.equals(ownerId);
    }

    public final @Nullable ServerPlayer getCalledOwner() {
        if (calledOwnerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (cachedCalledOwner != null && !cachedCalledOwner.isRemoved()
                && calledOwnerUuid.equals(cachedCalledOwner.getUUID())) {
            return cachedCalledOwner;
        }
        cachedCalledOwner = serverLevel.getServer().getPlayerList().getPlayer(calledOwnerUuid);
        return cachedCalledOwner;
    }

    public final boolean matchesBroomItem(ItemStack stack) {
        return stack.is(getRecoveryItem());
    }

    public final void recallWithoutItem() {
        if (calledOwnerUuid == null || breaking || isRemoved()) {
            return;
        }
        breaking = true;
        ejectPassengers();
        playBroomSound(SoundRegistry.VANILLA_BROOM_RECONSTRUCT.get());
        discard();
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (calledOwnerUuid != null
                && reason != RemovalReason.UNLOADED_WITH_PLAYER
                && reason != RemovalReason.UNLOADED_TO_CHUNK) {
            // 明示撤去では装備側を再表示する。RootVehicleとchunk保存ではUUIDを維持する。
            breaking = true;
            CallBroomDeploymentManager.onBroomRemoved(this);
        }
        super.remove(reason);
    }

    private void observeServerRidingMovement() {
        var now = level().getGameTime();
        var currentPosition = position();
        if (lastServerRidingPositionGameTime == Long.MIN_VALUE) {
            lastServerRidingPosition = currentPosition;
            lastServerRidingPositionGameTime = now;
            return;
        }

        var displacement = currentPosition.subtract(lastServerRidingPosition);
        if (displacement.lengthSqr() > MOVEMENT_EPSILON_SQR) {
            var elapsedTicks = Math.max(1L, now - lastServerRidingPositionGameTime);
            lastServerRidingMovement = clampInheritedDismountMovement(displacement.scale(1.0D / elapsedTicks));
            lastServerRidingMovementGameTime = now;
            lastServerRidingPosition = currentPosition;
            lastServerRidingPositionGameTime = now;
        } else if (lastServerRidingMovementGameTime != Long.MIN_VALUE
                && now - lastServerRidingMovementGameTime > SERVER_RIDING_MOVEMENT_GRACE_TICKS) {
            lastServerRidingMovement = Vec3.ZERO;
        }
    }

    private Vec3 takeServerRidingMovement() {
        observeServerRidingMovement();
        var now = level().getGameTime();
        var movement = lastServerRidingMovementGameTime != Long.MIN_VALUE
                && now - lastServerRidingMovementGameTime <= SERVER_RIDING_MOVEMENT_GRACE_TICKS
                ? lastServerRidingMovement
                : Vec3.ZERO;
        lastServerRidingPosition = Vec3.ZERO;
        lastServerRidingMovement = Vec3.ZERO;
        lastServerRidingPositionGameTime = Long.MIN_VALUE;
        lastServerRidingMovementGameTime = Long.MIN_VALUE;
        return movement;
    }

    private Vec3 clampInheritedDismountMovement(Vec3 movement) {
        if (!Double.isFinite(movement.x) || !Double.isFinite(movement.y) || !Double.isFinite(movement.z)) {
            return Vec3.ZERO;
        }
        var horizontal = new Vec3(movement.x, 0.0D, movement.z);
        var maximumHorizontalSpeed = maximumInheritedDismountHorizontalSpeed();
        if (horizontal.length() > maximumHorizontalSpeed) {
            horizontal = horizontal.normalize().scale(maximumHorizontalSpeed);
        }
        return new Vec3(
                horizontal.x,
                Mth.clamp(
                        movement.y,
                        -FloatmountBroomMovement.maximumVerticalSpeed(isWaterMovementPenaltyActive()),
                        FloatmountBroomMovement.maximumVerticalSpeed(isWaterMovementPenaltyActive())
                ),
                horizontal.z
        );
    }

    @Override
    public void onAboveBubbleCol(boolean downwards) {
        var before = getDeltaMovement();
        super.onAboveBubbleCol(downwards);
        finishBubbleColumnMovement(before);
    }

    @Override
    public void onInsideBubbleColumn(boolean downwards) {
        var before = getDeltaMovement();
        super.onInsideBubbleColumn(downwards);
        finishBubbleColumnMovement(before);
    }

    private void finishBubbleColumnMovement(Vec3 before) {
        if (!managesBubbleColumnMovement()) {
            return;
        }
        if (ignoresBubbleColumnMovement()) {
            // super側のfall distance更新は維持し、調整効果が対象とする速度だけを戻す。
            setDeltaMovement(before);
            bubbleColumnVerticalInfluence = 0.0D;
            return;
        }
        if (!shouldTrackBubbleColumnInfluence()) {
            bubbleColumnVerticalInfluence = 0.0D;
            return;
        }
        bubbleColumnVerticalInfluence += getDeltaMovement().y - before.y;
    }

    private double currentBubbleColumnVerticalInfluence() {
        if (!managesBubbleColumnMovement()
                || ignoresBubbleColumnMovement()
                || !shouldTrackBubbleColumnInfluence()
                || !getFeetBlockState().is(Blocks.BUBBLE_COLUMN)) {
            bubbleColumnVerticalInfluence = 0.0D;
        }
        return bubbleColumnVerticalInfluence;
    }

    private boolean shouldTrackBubbleColumnInfluence() {
        return !isVehicle() || level().isClientSide && isControlledByLocalInstance();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction moveFunction) {
        if (hasPassenger(passenger)) {
            moveFunction.accept(passenger, getX(),
                    getY() + RIDER_ATTACHMENT_Y + passengerAttachmentYOffset(), getZ());
            updatePassengerRotation(passenger);
        }
    }

    protected double passengerAttachmentYOffset() {
        return 0.0D;
    }

    protected void updatePassengerRotation(Entity passenger) {
        clampPassengerRotation(passenger);
    }

    protected final void clampPassengerRotation(Entity passenger) {
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
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        if (!isForcedLanding() && !isInLava()) {
            var safeTarget = findSafeDismountTarget(passenger);
            if (safeTarget.isPresent()) {
                var target = safeTarget.get();
                passenger.setPose(target.pose());
                return target.position();
            }
        }

        var candidate = preferredDismountPosition(passenger);
        for (var pose : passenger.getDismountPoses()) {
            var dismount = new Vec3(candidate.x, getY(), candidate.z);
            if (DismountHelper.canDismountTo(level(), dismount, passenger, pose)) {
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
        if (isForcedLanding() || isInLava()) {
            return true;
        }

        var passenger = getControllingPassenger();
        if (passenger != null) {
            return findSafeDismountTarget(passenger).isEmpty();
        }

        var surface = BroomSurfaceScanner.findSurfaceBelow(
                level(), getX(), getY(), getZ(), (int) DANGEROUS_HEIGHT, false
        );
        return surface.isEmpty()
                || isDangerousDismountSurface(surface.get())
                || getY() - surface.get().y() >= DANGEROUS_HEIGHT;
    }

    private Vec3 preferredDismountPosition(LivingEntity passenger) {
        // 乗り物yawはclient予測由来のため偽装できるが、server側で距離・高度・地表・衝突を再検証する。
        // 影響は箒の近傍で降りる側を選べる程度に限られ、危険高度から安全地表への移動には使えないため、専用のserver権威yawは持たない。
        var left = getCollisionHorizontalEscapeVector(
                getBbWidth() * Mth.SQRT_OF_TWO,
                passenger.getBbWidth(),
                getYRot() - 90.0F
        );
        return position().add(left);
    }

    private Optional<DismountTarget> findSafeDismountTarget(LivingEntity passenger) {
        var candidate = preferredDismountPosition(passenger);
        var surface = BroomSurfaceScanner.findSurfaceBelow(
                level(), candidate.x, getY(), candidate.z, (int) DANGEROUS_HEIGHT, false
        );
        if (surface.isEmpty()
                || isDangerousDismountSurface(surface.get())
                || getY() - surface.get().y() >= DANGEROUS_HEIGHT) {
            return Optional.empty();
        }

        var position = new Vec3(candidate.x, surface.get().y(), candidate.z);
        for (var pose : passenger.getDismountPoses()) {
            if (DismountHelper.canDismountTo(level(), position, passenger, pose)) {
                return Optional.of(new DismountTarget(position, pose));
            }
        }
        return Optional.empty();
    }

    private boolean isDangerousDismountSurface(BroomSurfaceScanner.Surface surface) {
        return level().getBlockState(surface.blockPos())
                .is(TagRegistry.Blocks.BROOM_DANGEROUS_DISMOUNT_SURFACES);
    }

    private record DismountTarget(Vec3 position, Pose pose) {
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
        acceptServerInput(player, strafe, forward, ascending, descending, BroomInputTransition.NONE, 0L);
    }

    public void acceptServerInput(
            Player player,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending,
            BroomInputTransition transition,
            long actionSequence
    ) {
        if (player.getVehicle() != this || getControllingPassenger() != player) {
            return;
        }

        // 箒の有人移動は操作clientが予測し、vanillaのvehicle位置更新をserverが採用する。
        // serverはsender・操縦者・入力範囲を検証したうえで、申告された動力入力だけを課金根拠にする。
        // 実座標差では惰性・server再配置・他MODの外力と偽装移動を区別できず、座標差課金は正規playerへの
        // 誤課金や意図しない緊急着陸を起こすため採用しない。この設計では改造clientによる入力の過少申告を防げず、
        // マナ消費と緊急着陸は対不正clientの保証境界には含めない。
        localStrafeInput = sanitizeInput(strafe);
        serverForwardInput = sanitizeInput(forward);
        serverAscending = ascending;
        descendingInput = descending;
        lastServerInputGameTime = level().getGameTime();
    }

    private boolean canMountWithCurrentMana(Player player) {
        if (!requiresMountMana() || player.getAbilities().instabuild) {
            return true;
        }
        var requiredMana = ApprenticeCodexServerConfig.floatmountBroomConfig().normalFlightManaThreshold();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.getMana() >= requiredMana) {
            return true;
        }
        messageKeys().insufficientMana().ifPresent(key -> player.displayClientMessage(Component.translatable(
                key,
                requiredMana
        ).withStyle(ChatFormatting.RED), true));
        playPlayerNotification(player, SoundRegistry.VANILLA_BROOM_MOUNT_REJECT.get());
        return false;
    }

    protected void tickControlledServer(Player player) {
        if (player.getAbilities().instabuild) {
            resetRidingState();
            return;
        }

        var now = level().getGameTime();
        if (lastServerInputGameTime == Long.MIN_VALUE
                || now - lastServerInputGameTime > SERVER_INPUT_TIMEOUT_TICKS) {
            clearRidingInput();
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var config = ApprenticeCodexServerConfig.floatmountBroomConfig();
        var mana = magicData == null ? 0.0F : magicData.getMana();
        if (isDamaged()) {
            return;
        }
        if (isManaEmergencyLanding()) {
            if (mana >= config.normalFlightManaThreshold()) {
                setManaEmergencyLanding(false);
                lowManaWarningShown = false;
                player.displayClientMessage(Component.translatable(
                        messageKeys().manaRecovered()
                ).withStyle(ChatFormatting.GREEN), true);
                playBroomSound(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
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
                setManaEmergencyLanding(true);
                player.displayClientMessage(Component.translatable(
                        messageKeys().manaDepleted()
                ).withStyle(ChatFormatting.RED), true);
                playBroomSound(SoundRegistry.VANILLA_BROOM_EMERGENCY.get());
                return;
            }
            mana = updatedMana;
        }

        if (!lowManaWarningShown
                && mana <= config.lowManaWarningThreshold()
                && mana < config.normalFlightManaThreshold()) {
            lowManaWarningShown = true;
            player.displayClientMessage(Component.translatable(
                    messageKeys().warningLowMana()
            ).withStyle(ChatFormatting.YELLOW), true);
            playPlayerNotification(player, SoundRegistry.VANILLA_BROOM_WARNING.get());
        } else if (lowManaWarningShown && mana >= config.normalFlightManaThreshold()) {
            lowManaWarningShown = false;
        }
    }

    private float movementManaCost(FloatmountBroomServerConfig.Values config) {
        var horizontal = Math.abs(serverForwardInput) > INPUT_EPSILON;
        var overdriveEnabled = isOverdriveEnabled();
        if (horizontal && serverAscending) {
            return Math.max(0.0F, (float)(overdriveEnabled
                    ? config.overdriveHorizontalAscendingManaCostPerTick()
                    : config.horizontalAscendingManaCostPerTick()));
        }
        if (horizontal) {
            return Math.max(0.0F, (float)(overdriveEnabled
                    ? config.overdriveHorizontalManaCostPerTick()
                    : config.horizontalManaCostPerTick()));
        }
        return serverAscending
                ? Math.max(0.0F, (float)(overdriveEnabled
                        ? config.overdriveAscendingManaCostPerTick()
                        : config.ascendingManaCostPerTick()))
                : 0.0F;
    }

    protected final boolean isOverdriveEnabled() {
        return AbstractBroomItem.isOverdriveEnabled(getBroomItemStack());
    }

    protected static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    protected static void syncMana(Player player, MagicData magicData) {
        if (player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }

    protected final void playBroomSound(SoundEvent sound) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    static void playPlayerNotification(Player player, SoundEvent sound) {
        if (player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)) {
            serverPlayer.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    protected void resetRidingState() {
        setManaEmergencyLanding(false);
        lowManaWarningShown = false;
        clearRidingInput();
    }

    protected void clearRidingInput() {
        localStrafeInput = 0.0F;
        serverForwardInput = 0.0F;
        serverAscending = false;
        descendingInput = false;
        lastServerInputGameTime = Long.MIN_VALUE;
    }

    public void handleLocalInputTransition(BroomInputTransition transition, long actionSequence) {
    }

    public boolean isManaEmergencyLanding() {
        return entityData.get(MANA_EMERGENCY_LANDING);
    }

    private void setManaEmergencyLanding(boolean value) {
        entityData.set(MANA_EMERGENCY_LANDING, value);
    }

    public boolean isDamaged() {
        return entityData.get(DAMAGED);
    }

    private void setDamaged(boolean value) {
        entityData.set(DAMAGED, value);
    }

    public boolean isForcedLanding() {
        return isManaEmergencyLanding() || isDamaged();
    }

    public BroomCoreWarningState getCoreWarningState() {
        return isForcedLanding() ? BroomCoreWarningState.CRITICAL : BroomCoreWarningState.NONE;
    }

    public final boolean isLowManaWarningShown() {
        return lowManaWarningShown;
    }

    protected final void setLowManaWarningShown(boolean value) {
        lowManaWarningShown = value;
    }

    public boolean isBreaking() {
        return breaking;
    }

    public int getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setDamage(int value) {
        entityData.set(DAMAGE, Mth.clamp(value, 0, getMaxDamage()));
    }

    public int getMaxDamage() {
        return Math.max(1, entityData.get(MAX_DAMAGE));
    }

    private void setMaxDamage(int value) {
        entityData.set(MAX_DAMAGE, Math.max(1, value));
    }

    public float getDamageRatio() {
        return Mth.clamp(getDamage() / (float)getMaxDamage(), 0.0F, 1.0F);
    }

    public int getDamageStage() {
        return Mth.clamp((int)((long)getDamage() * 4L / getMaxDamage()), 0, 4);
    }

    public int getRemainingDurabilitySteps() {
        var remaining = Math.max(0, getMaxDamage() - getDamage());
        var roundedUpSteps = ((long)remaining * DURABILITY_STEPS + getMaxDamage() - 1L) / getMaxDamage();
        return Mth.clamp((int)roundedUpSteps, 0, DURABILITY_STEPS);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            state.setAnimation(isVehicle() ? MOUNT : IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
