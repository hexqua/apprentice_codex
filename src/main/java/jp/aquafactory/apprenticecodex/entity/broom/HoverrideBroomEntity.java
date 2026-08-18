package jp.aquafactory.apprenticecodex.entity.broom;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomImpulseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomReleaseResultPacket;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.OptionalDouble;

public final class HoverrideBroomEntity extends AbstractBroomEntity {
    private static final EntityDataAccessor<Boolean> MANA_DEPLETED =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AIRBORNE_ACCELERATION_LOCKED =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PRESENTATION_STATE =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.INT);
    private static final float INPUT_EPSILON = 1.0e-4F;
    private static final float MANA_EPSILON = 1.0e-4F;
    private static final int SERVER_INPUT_TIMEOUT_TICKS = 30;
    private static final int AIRBORNE_GRACE_TICKS = 40;
    private static final int RIDE_SURFACE_SCAN_BLOCKS = 4;
    private static final double ASSIST_WINGS_LANDING_DISTANCE = 1.5D;
    private static final double ASSIST_WINGS_GRAVITY = 0.08D;
    private static final double ASSIST_WINGS_VERTICAL_DRAG = 0.98D;
    private static final double ASSIST_WINGS_MAX_JUMP_HEIGHT = 4.0D;
    private static final double HOVER_HEIGHT = 1.2D;
    private static final double VERTICAL_ACCELERATION = 0.03D;
    private static final double MAX_MOUNTED_VERTICAL_SPEED = 0.15D;
    private static final double MAX_UNMOUNTED_VERTICAL_SPEED = 0.10D;
    private static final double UNMOUNTED_HORIZONTAL_DAMPING = 0.85D;
    private static final float TURN_ACCELERATION = 1.0F;
    private static final float MAX_TURN_SPEED = 10.0F;
    private static final float TURN_DAMPING = 0.9F;
    private static final float PASSENGER_YAW_RESPONSE = 0.4F;
    private static final int ACCELERATION_SOUND_INTERVAL_TICKS = 10;
    private static final double BRAKE_PARTICLE_MINIMUM_SPEED = 0.08D;
    private static final double EFFECT_AXIS_RANGE = 1.0D;
    private static final double EFFECT_VERTICAL_SPREAD = 0.1D;
    private static final double EFFECT_Y_OFFSET = 0.1D;
    // 座り脚から立ち脚へ変わる約10ピクセル分、乗員全体を持ち上げて足元を箒へ合わせる。
    private static final double STANDING_RIDER_Y_OFFSET = 0.625D;

    private static final BroomMessageKeys MESSAGE_KEYS = new BroomMessageKeys(
            "ui.apprenticecodex.hoverride_broom.warning_dismount",
            "ui.apprenticecodex.hoverride_broom.retrieve_help",
            "ui.apprenticecodex.hoverride_broom.warning_damage",
            "ui.apprenticecodex.hoverride_broom.cannot_mount_damaged",
            Optional.empty(),
            "ui.apprenticecodex.hoverride_broom.warning_low_mana",
            "ui.apprenticecodex.hoverride_broom.warning_depleted_mana",
            "ui.apprenticecodex.hoverride_broom.recover_mana"
    );

    public HoverrideBroomEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    private float localStrafeInput;
    private float localForwardInput;
    private LocalReleaseState localReleaseState = LocalReleaseState.IDLE;
    private Vec3 localInertia = Vec3.ZERO;
    private long pendingReleaseSequence = Long.MIN_VALUE;
    private boolean localReleaseImpulsePending;
    private int localAirborneTicks;
    private boolean localAssistWingsJumpActive;
    private double localAssistWingsVerticalVelocity;
    private float localTurnSpeed;
    private int followedPassengerId = -1;
    private float lastFollowedBroomYaw;
    private float pendingPassengerYaw;

    private float serverForwardInput;
    private boolean serverGlideRequested;
    private boolean serverGlideActive;
    private int serverSuccessfulGlideTicks;
    private long lastServerInputGameTime = Long.MIN_VALUE;
    private long lastServerActionSequence;
    private int serverAirborneTicks;
    private long lastAccelerationSoundGameTime = Long.MIN_VALUE;

    private Vec3 lastClientEffectPosition;

    // serverの解除確定と課金は応答送信より先に完了するため、経過tickだけでAWAITING_RESULTを破棄しない。
    private enum LocalReleaseState {
        IDLE,
        GLIDING,
        AWAITING_RESULT;

        private boolean preservesInertia() {
            return this != IDLE;
        }
    }

    @Override
    protected Item getRecoveryItem() {
        return ItemRegistry.HOVERRIDE_BROOM.get();
    }

    @Override
    protected BroomMessageKeys messageKeys() {
        return MESSAGE_KEYS;
    }

    @Override
    protected boolean requiresMountMana() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MANA_DEPLETED, false);
        entityData.define(AIRBORNE_ACCELERATION_LOCKED, false);
        entityData.define(PRESENTATION_STATE, HoverrideBroomPresentation.NORMAL.ordinal());
    }

    @Override
    protected void spawnFlightParticles() {
        if (!isVehicle() || isDamaged()) {
            lastClientEffectPosition = position();
            super.spawnFlightParticles();
            return;
        }

        var movement = sampleClientEffectMovement();
        switch (effectivePresentationState()) {
            case GLIDING -> spawnGlideParticles();
            case BRAKING -> {
                if (movement.length() >= BRAKE_PARTICLE_MINIMUM_SPEED) {
                    spawnBrakeParticles(movement);
                } else if (!isManaDepleted()) {
                    spawnDefaultFlightParticles(false);
                }
            }
            case ACCELERATING -> spawnDefaultFlightParticles(true);
            case NORMAL -> {
                if (!isManaDepleted()) {
                    spawnDefaultFlightParticles(false);
                }
            }
        }
    }

    private HoverrideBroomPresentation effectivePresentationState() {
        if (!isControlledByLocalInstance()) {
            return getPresentationState();
        }
        var gliding = localReleaseState.preservesInertia();
        var accelerationAllowed = !isManaDepleted()
                && !isDamaged()
                && localAirborneTicks < AIRBORNE_GRACE_TICKS;
        return HoverrideBroomPresentation.resolve(localForwardInput, gliding, accelerationAllowed);
    }

    private Vec3 sampleClientEffectMovement() {
        var current = position();
        var sampled = lastClientEffectPosition == null
                ? HoverrideBroomMovement.horizontal(getDeltaMovement())
                : HoverrideBroomMovement.horizontal(current.subtract(lastClientEffectPosition));
        lastClientEffectPosition = current;
        return sampled.lengthSqr() > 1.0e-8D
                ? sampled
                : HoverrideBroomMovement.horizontal(getDeltaMovement());
    }

    private void spawnGlideParticles() {
        spawnBodyParticle(
                ParticleRegistry.ADDITIVE_RHOMBUS.get(), randomGlideColor(),
                0.14F + random.nextFloat() * 0.08F, 10, 4, Vec3.ZERO
        );
        if (tickCount % 2 == 0) {
            spawnBodyParticle(
                    ParticleRegistry.ADDITIVE_SPARK.get(), randomGlideColor(),
                    0.14F + random.nextFloat() * 0.08F, 10, 4, Vec3.ZERO
            );
        }
    }

    private void spawnBrakeParticles(Vec3 movement) {
        var direction = movement.normalize();
        var count = 1 + random.nextInt(2);
        for (var i = 0; i < count; ++i) {
            var speed = 0.04D + random.nextDouble() * 0.04D;
            spawnBodyParticle(
                    ParticleRegistry.ADDITIVE_SPARK.get(), randomBrakeColor(),
                    0.14F + random.nextFloat() * 0.08F, 6, 3, direction.scale(speed)
            );
        }
    }

    private void spawnBodyParticle(
            ParticleType<AdditiveGlowParticleOptions> type,
            Vector3f color,
            float size,
            int lifetime,
            int lifetimeVariance,
            Vec3 velocity
    ) {
        var axisOffset = (random.nextDouble() * 2.0D - 1.0D) * EFFECT_AXIS_RANGE;
        var base = position().add(getForwardDirection().scale(axisOffset)).add(
                0.0D,
                EFFECT_Y_OFFSET + (random.nextDouble() * 2.0D - 1.0D) * EFFECT_VERTICAL_SPREAD,
                0.0D
        );
        level().addParticle(
                new AdditiveGlowParticleOptions(
                        type,
                        size,
                        color.x(),
                        color.y(),
                        color.z(),
                        random.nextInt(2),
                        lifetime,
                        lifetimeVariance,
                        0.75F,
                        1.2F,
                        0.6F,
                        0.95F,
                        0.02F,
                        0.65F,
                        0.4F,
                        true
                ),
                base.x,
                base.y,
                base.z,
                velocity.x,
                velocity.y,
                velocity.z
        );
    }

    private Vector3f randomGlideColor() {
        var t = random.nextFloat();
        return new Vector3f(
                Mth.lerp(t, 0.20F, 0.22F),
                Mth.lerp(t, 1.0F, 0.82F),
                Mth.lerp(t, 0.42F, 0.72F)
        );
    }

    private Vector3f randomBrakeColor() {
        var t = random.nextFloat();
        return new Vector3f(
                1.0F,
                Mth.lerp(t, 0.18F, 0.55F),
                Mth.lerp(t, 0.05F, 0.08F)
        );
    }

    @Override
    public void setLocalInput(float strafe, float forward, boolean ascending, boolean descending) {
        super.setLocalInput(strafe, forward, ascending, descending);
        localStrafeInput = sanitizeInput(strafe);
        localForwardInput = sanitizeInput(forward);
        if (ascending && !isManaDepleted() && !isDamaged()
                && localReleaseState == LocalReleaseState.IDLE) {
            localReleaseState = LocalReleaseState.GLIDING;
            localInertia = HoverrideBroomMovement.horizontal(getDeltaMovement());
        }
    }

    @Override
    public void handleLocalInputTransition(BroomInputTransition transition, long actionSequence) {
        if (transition == BroomInputTransition.CANCEL) {
            cancelLocalGlide();
        } else if (transition == BroomInputTransition.RELEASE
                && localReleaseState == LocalReleaseState.GLIDING) {
            localReleaseState = LocalReleaseState.AWAITING_RESULT;
            pendingReleaseSequence = actionSequence;
        }
    }

    public void acceptLocalReleaseResult(long sequence, boolean accepted, double minimumHorizontalSpeed) {
        if (localReleaseState != LocalReleaseState.AWAITING_RESULT
                || sequence != pendingReleaseSequence) {
            return;
        }
        localReleaseState = LocalReleaseState.IDLE;
        pendingReleaseSequence = Long.MIN_VALUE;
        if (accepted) {
            var released = HoverrideBroomMovement.releaseHorizontal(
                    localInertia,
                    getForwardDirection(),
                    minimumHorizontalSpeed
            );
            setDeltaMovement(released.x, getDeltaMovement().y, released.z);
            localReleaseImpulsePending = true;
            localAirborneTicks = 0;
        }
        localInertia = Vec3.ZERO;
    }

    @Override
    protected void applyControlledMovement() {
        localTurnSpeed = Mth.clamp(
                (localTurnSpeed + localStrafeInput * TURN_ACCELERATION) * TURN_DAMPING,
                -MAX_TURN_SPEED,
                MAX_TURN_SPEED
        );
        setYRot(getYRot() + localTurnSpeed);

        if (isDamaged()) {
            cancelLocalGlide();
            cancelLocalAssistWingsJump();
        } else if (isManaDepleted() && localReleaseState == LocalReleaseState.GLIDING) {
            localReleaseState = LocalReleaseState.IDLE;
            localInertia = Vec3.ZERO;
        }

        var surface = findRideSurfaceBelow();
        localAirborneTicks = surface.isPresent() ? 0 : Math.min(AIRBORNE_GRACE_TICKS, localAirborneTicks + 1);

        var movement = getDeltaMovement();
        var gliding = localReleaseState.preservesInertia();
        var horizontal = gliding
                ? localInertia
                : localReleaseImpulsePending
                        ? HoverrideBroomMovement.horizontal(movement)
                        : HoverrideBroomMovement.normalHorizontal(
                        movement,
                        getForwardDirection(),
                        localForwardInput,
                        !isManaDepleted() && !isDamaged() && localAirborneTicks < AIRBORNE_GRACE_TICKS
                );
        localReleaseImpulsePending = false;
        if (gliding && (isInWaterOrBubble() || isInLava())) {
            // liquid接触は壁衝突と異なりmoveの実変位へ必ずしも減速が反映されないため、慣性値へ直接反映する。
            horizontal = horizontal.scale(isInLava() ? 0.5D : 0.8D);
        }
        if (localAssistWingsJumpActive
                && localAssistWingsVerticalVelocity <= 0.0D
                && isWithinAssistWingsLandingDistance(surface)) {
            cancelLocalAssistWingsJump();
        }
        var vertical = localAssistWingsJumpActive
                ? Math.max(-MAX_MOUNTED_VERTICAL_SPEED, localAssistWingsVerticalVelocity)
                : mountedVertical(movement.y, surface.orElse(Double.NaN));
        if (isDamaged()) {
            vertical = -MAX_MOUNTED_VERTICAL_SPEED;
        }

        var beforeMove = position();
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        if (localAssistWingsJumpActive) {
            var actualVertical = getY() - beforeMove.y;
            if (vertical > 0.0D && actualVertical + 1.0e-4D < vertical) {
                cancelLocalAssistWingsJump();
                var collidedMovement = getDeltaMovement();
                setDeltaMovement(collidedMovement.x, 0.0D, collidedMovement.z);
            } else if (vertical <= -MAX_MOUNTED_VERTICAL_SPEED) {
                cancelLocalAssistWingsJump();
            } else {
                localAssistWingsVerticalVelocity = Math.max(
                        -MAX_MOUNTED_VERTICAL_SPEED,
                        (vertical - ASSIST_WINGS_GRAVITY) * ASSIST_WINGS_VERTICAL_DRAG
                );
            }
        }
        if (gliding) {
            var actualMovement = position().subtract(beforeMove);
            localInertia = HoverrideBroomMovement.horizontal(actualMovement);
            setDeltaMovement(localInertia.x, getDeltaMovement().y, localInertia.z);
        }
    }

    @Override
    protected void applyUnoccupiedMovement() {
        resetPassengerYawFollow();
        var movement = getDeltaMovement();
        var horizontal = HoverrideBroomMovement.horizontal(movement).scale(UNMOUNTED_HORIZONTAL_DAMPING);
        var surface = findRideSurfaceBelow();
        var vertical = surface.isPresent()
                ? hoverVertical(movement.y, surface.getAsDouble(), MAX_UNMOUNTED_VERTICAL_SPEED)
                : Math.max(-MAX_UNMOUNTED_VERTICAL_SPEED, movement.y - VERTICAL_ACCELERATION);
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    protected double maximumInheritedDismountHorizontalSpeed() {
        return HoverrideBroomMovement.MAX_HORIZONTAL_SPEED;
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide && !isVehicle()) {
            // 応答待機中に降車した場合、後から届く解除結果を次の騎乗へ持ち越さない。
            cancelLocalGlide();
            cancelLocalAssistWingsJump();
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected double passengerAttachmentYOffset() {
        return STANDING_RIDER_Y_OFFSET;
    }

    @Override
    protected void updatePassengerRotation(Entity passenger) {
        if (followedPassengerId != passenger.getId()) {
            followedPassengerId = passenger.getId();
            lastFollowedBroomYaw = getYRot();
            pendingPassengerYaw = 0.0F;
        } else {
            var broomYawChange = Mth.wrapDegrees(getYRot() - lastFollowedBroomYaw);
            pendingPassengerYaw = Mth.wrapDegrees(pendingPassengerYaw + broomYawChange);
            var appliedYaw = pendingPassengerYaw * PASSENGER_YAW_RESPONSE;
            passenger.setYRot(passenger.getYRot() + appliedYaw);
            pendingPassengerYaw -= appliedYaw;
            lastFollowedBroomYaw = getYRot();
        }
        clampPassengerRotation(passenger);
    }

    private double mountedVertical(double currentVertical, double surfaceY) {
        if (!Double.isFinite(surfaceY)) {
            return Math.max(-MAX_MOUNTED_VERTICAL_SPEED, currentVertical - VERTICAL_ACCELERATION);
        }
        return hoverVertical(currentVertical, surfaceY, MAX_MOUNTED_VERTICAL_SPEED);
    }

    private double hoverVertical(double currentVertical, double surfaceY, double maxSpeed) {
        var error = surfaceY + HOVER_HEIGHT - getY();
        return Mth.clamp(error * 0.2D + currentVertical * 0.6D, -maxSpeed, maxSpeed);
    }

    @Override
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

        serverForwardInput = sanitizeInput(forward);
        lastServerInputGameTime = level().getGameTime();
        if (transition != BroomInputTransition.NONE
                && (actionSequence <= 0L || actionSequence <= lastServerActionSequence)) {
            if (transition == BroomInputTransition.RELEASE) {
                sendReleaseResult(player, actionSequence, false);
            }
            return;
        }
        if (transition != BroomInputTransition.NONE) {
            lastServerActionSequence = actionSequence;
        }
        if (transition == BroomInputTransition.CANCEL) {
            cancelServerGlide();
            return;
        }
        if (transition == BroomInputTransition.RELEASE) {
            handleServerRelease(player, actionSequence);
            return;
        }

        serverGlideRequested = ascending;
        if (!ascending) {
            cancelServerGlide();
        }
    }

    @Override
    protected void tickControlledServer(Player player) {
        updateServerSurfaceState();
        if (isDamaged()) {
            cancelServerGlide();
            setPresentationState(HoverrideBroomPresentation.NORMAL);
            return;
        }

        var now = level().getGameTime();
        if (lastServerInputGameTime == Long.MIN_VALUE
                || now - lastServerInputGameTime > SERVER_INPUT_TIMEOUT_TICKS) {
            clearRidingInput();
        }

        if (player.getAbilities().instabuild) {
            setManaDepleted(false);
            if (serverGlideRequested) {
                serverGlideActive = true;
                serverSuccessfulGlideTicks++;
            }
            updateServerPresentation();
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var mana = magicData == null ? 0.0F : magicData.getMana();
        var config = ApprenticeCodexServerConfig.hoverrideBroomConfig();
        if (isManaDepleted()) {
            if (mana + MANA_EPSILON >= config.inertiaReleaseManaCost()) {
                setManaDepleted(false);
                setLowManaWarningShown(false);
                player.displayClientMessage(Component.translatable(
                        messageKeys().manaRecovered()
                ).withStyle(ChatFormatting.GREEN), true);
                playBroomSound(SoundRegistry.VANILLA_BROOM_PROPULSION_RECOVERED.get());
            }
            updateServerPresentation();
            return;
        }
        if (mana <= MANA_EPSILON) {
            enterManaDepleted(player);
            updateServerPresentation();
            return;
        }

        if (serverGlideRequested) {
            serverGlideActive = true;
            serverSuccessfulGlideTicks++;
            consumeMana(player, magicData, (float)config.inertiaGlideManaCostPerTick());
            if (isManaDepleted()) {
                cancelServerGlide();
            } else {
                updateLowManaWarning(player, manaAfterConsumption(magicData), config);
            }
            updateServerPresentation();
            return;
        }

        if (serverForwardInput > INPUT_EPSILON && serverAirborneTicks < AIRBORNE_GRACE_TICKS) {
            consumeMana(player, magicData, (float)config.forwardManaCostPerTick());
        }
        if (!isManaDepleted()) {
            updateLowManaWarning(player, manaAfterConsumption(magicData), config);
        }
        updateServerPresentation();
    }

    private float manaAfterConsumption(MagicData magicData) {
        return magicData == null ? 0.0F : magicData.getMana();
    }

    private void updateLowManaWarning(
            Player player,
            float mana,
            HoverrideBroomServerConfig.Values config
    ) {
        var recoveryThreshold = config.inertiaReleaseManaCost();
        if (!isLowManaWarningShown()
                && mana <= config.lowManaWarningThreshold()
                && mana < recoveryThreshold) {
            setLowManaWarningShown(true);
            player.displayClientMessage(Component.translatable(
                    messageKeys().warningLowMana()
            ).withStyle(ChatFormatting.YELLOW), true);
            playPlayerNotification(player, SoundRegistry.VANILLA_BROOM_WARNING.get());
        } else if (isLowManaWarningShown() && mana + MANA_EPSILON >= recoveryThreshold) {
            setLowManaWarningShown(false);
        }
    }

    private void updateServerPresentation() {
        var accelerationAllowed = !isManaDepleted()
                && !isDamaged()
                && serverAirborneTicks < AIRBORNE_GRACE_TICKS;
        var presentation = HoverrideBroomPresentation.resolve(
                serverForwardInput,
                serverGlideActive,
                accelerationAllowed
        );
        setPresentationState(presentation);
        if (presentation == HoverrideBroomPresentation.ACCELERATING) {
            var now = level().getGameTime();
            if (lastAccelerationSoundGameTime == Long.MIN_VALUE
                    || now - lastAccelerationSoundGameTime >= ACCELERATION_SOUND_INTERVAL_TICKS) {
                level().playSound(null, getX(), getY(), getZ(),
                        SoundRegistry.BROOM_ACCELERATE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.0F);
                lastAccelerationSoundGameTime = now;
            }
        }
    }

    private void handleServerRelease(Player player, long actionSequence) {
        var accepted = serverGlideActive && serverSuccessfulGlideTicks > 0 && !isManaDepleted() && !isDamaged();
        var config = ApprenticeCodexServerConfig.hoverrideBroomConfig();
        if (accepted && !player.getAbilities().instabuild) {
            var magicData = MagicData.getPlayerMagicData(player);
            consumeMana(player, magicData, (float)config.inertiaReleaseManaCost());
            if (!isManaDepleted()) {
                updateLowManaWarning(player, manaAfterConsumption(magicData), config);
            }
        }
        if (accepted) {
            serverAirborneTicks = 0;
            setAirborneAccelerationLocked(false);
            playBroomSound(SoundRegistry.VANILLA_BROOM_IMPULSE.get());
            // 解除音の直後に周期加速音を重ねず、強い一回音を操作フィードバックとして残す。
            lastAccelerationSoundGameTime = level().getGameTime();
            sendReleaseEffect();
        }
        cancelServerGlide();
        updateServerPresentation();

        sendReleaseResult(player, actionSequence, accepted);
    }

    private void sendReleaseEffect() {
        var forward = getForwardDirection();
        var center = position().subtract(forward.scale(0.8D)).add(0.0D, EFFECT_Y_OFFSET, 0.0D);
        Networks.sendToTrackingEntityAndSelf(this, new HoverrideBroomImpulseEffectPacket(center, forward));
    }

    private void sendReleaseResult(Player player, long actionSequence, boolean accepted) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer instanceof FakePlayer) {
            return;
        }
        var config = ApprenticeCodexServerConfig.hoverrideBroomConfig();
        Networks.sendToPlayer(serverPlayer, new HoverrideBroomReleaseResultPacket(
                getId(),
                actionSequence,
                accepted,
                HoverrideBroomMovement.MAX_HORIZONTAL_SPEED * config.inertiaReleaseMinimumSpeedRatio()
        ));
    }

    private void consumeMana(Player player, MagicData magicData, float cost) {
        if (cost <= 0.0F) {
            return;
        }
        var mana = magicData == null ? 0.0F : magicData.getMana();
        var updatedMana = Math.max(0.0F, mana - cost);
        if (magicData != null && updatedMana != mana) {
            magicData.setMana(updatedMana);
            syncMana(player, magicData);
        }
        if (updatedMana <= MANA_EPSILON) {
            enterManaDepleted(player);
        }
    }

    private void enterManaDepleted(Player player) {
        if (isManaDepleted()) {
            return;
        }
        setManaDepleted(true);
        cancelServerGlide();
        playBroomSound(SoundRegistry.VANILLA_BROOM_PROPULSION_LOST.get());
        player.displayClientMessage(Component.translatable(
                messageKeys().manaDepleted()
        ).withStyle(ChatFormatting.RED), true);
    }

    private void updateServerSurfaceState() {
        var surface = findRideSurfaceBelow();
        serverAirborneTicks = surface.isPresent()
                ? 0
                : Math.min(AIRBORNE_GRACE_TICKS, serverAirborneTicks + 1);
        setAirborneAccelerationLocked(serverAirborneTicks >= AIRBORNE_GRACE_TICKS);
    }

    private OptionalDouble findRideSurfaceBelow() {
        // Floatmountと同系統の箒として、溶岩面も走行・浮遊を維持できる地表として扱う。
        // プレイヤーの安全な降車地点はAbstractBroomEntity側で引き続き溶岩を除外する。
        return BroomSurfaceScanner.findSurfaceBelow(
                level(), getX(), getY(), getZ(), RIDE_SURFACE_SCAN_BLOCKS, true
        );
    }

    public boolean isWithinAssistWingsLandingDistance() {
        return isWithinAssistWingsLandingDistance(findRideSurfaceBelow());
    }

    private boolean isWithinAssistWingsLandingDistance(OptionalDouble surface) {
        return surface.isPresent() && getY() - surface.getAsDouble() <= ASSIST_WINGS_LANDING_DISTANCE;
    }

    public boolean canUseAssistWings(Player player) {
        return player.getVehicle() == this
                && getControllingPassenger() == player
                && !isDamaged()
                && !isManaDepleted();
    }

    public boolean acceptServerAssistWingsJump(Player player, float jumpHeight) {
        if (level().isClientSide || !Float.isFinite(jumpHeight) || jumpHeight <= 0.0F
                || !canUseAssistWings(player)) {
            return false;
        }

        serverAirborneTicks = 0;
        setAirborneAccelerationLocked(false);
        // 詠唱音の直後に周期加速音を重ねず、Assist Wingsの操作フィードバックを残す。
        lastAccelerationSoundGameTime = level().getGameTime();
        updateServerPresentation();
        if (player instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)) {
            Networks.sendToPlayer(serverPlayer, new HoverrideBroomAssistWingsJumpPacket(getId(), jumpHeight));
        }
        return true;
    }

    public void acceptLocalAssistWingsJump(float jumpHeight) {
        if (!Float.isFinite(jumpHeight) || jumpHeight <= 0.0F || isDamaged()) {
            return;
        }

        localAssistWingsJumpActive = true;
        localAssistWingsVerticalVelocity = Mth.clamp(jumpHeight, 0.0D, ASSIST_WINGS_MAX_JUMP_HEIGHT);
        localAirborneTicks = 0;
        var movement = getDeltaMovement();
        setDeltaMovement(movement.x, localAssistWingsVerticalVelocity, movement.z);
    }

    private void cancelLocalAssistWingsJump() {
        localAssistWingsJumpActive = false;
        localAssistWingsVerticalVelocity = 0.0D;
    }

    private void cancelLocalGlide() {
        localReleaseState = LocalReleaseState.IDLE;
        localInertia = Vec3.ZERO;
        pendingReleaseSequence = Long.MIN_VALUE;
        localReleaseImpulsePending = false;
    }

    private void cancelServerGlide() {
        serverGlideRequested = false;
        serverGlideActive = false;
        serverSuccessfulGlideTicks = 0;
    }

    @Override
    protected void clearRidingInput() {
        super.clearRidingInput();
        serverForwardInput = 0.0F;
        lastServerInputGameTime = Long.MIN_VALUE;
        cancelServerGlide();
    }

    @Override
    protected void resetRidingState() {
        super.resetRidingState();
        setManaDepleted(false);
        setAirborneAccelerationLocked(false);
        lastServerActionSequence = 0L;
        serverAirborneTicks = 0;
        clearRidingInput();
        cancelLocalGlide();
        cancelLocalAssistWingsJump();
        localAirborneTicks = 0;
        lastClientEffectPosition = null;
        lastAccelerationSoundGameTime = Long.MIN_VALUE;
        setPresentationState(HoverrideBroomPresentation.NORMAL);
        resetPassengerYawFollow();
    }

    private void resetPassengerYawFollow() {
        followedPassengerId = -1;
        lastFollowedBroomYaw = 0.0F;
        pendingPassengerYaw = 0.0F;
    }

    public boolean isManaDepleted() {
        return entityData.get(MANA_DEPLETED);
    }

    @Override
    public BroomCoreWarningState getCoreWarningState() {
        var baseState = super.getCoreWarningState();
        if (baseState == BroomCoreWarningState.CRITICAL || isManaDepleted()) {
            return BroomCoreWarningState.CRITICAL;
        }
        return isAirborneAccelerationLocked()
                ? BroomCoreWarningState.CAUTION
                : BroomCoreWarningState.NONE;
    }

    private void setManaDepleted(boolean value) {
        entityData.set(MANA_DEPLETED, value);
    }

    public boolean isAirborneAccelerationLocked() {
        return entityData.get(AIRBORNE_ACCELERATION_LOCKED);
    }

    private void setAirborneAccelerationLocked(boolean value) {
        entityData.set(AIRBORNE_ACCELERATION_LOCKED, value);
    }

    public HoverrideBroomPresentation getPresentationState() {
        return HoverrideBroomPresentation.fromId(entityData.get(PRESENTATION_STATE));
    }

    private void setPresentationState(HoverrideBroomPresentation state) {
        entityData.set(PRESENTATION_STATE, state.ordinal());
    }

    public boolean isServerInertiaGlideActive() {
        return serverGlideActive;
    }

    public int getServerSuccessfulGlideTicks() {
        return serverSuccessfulGlideTicks;
    }

    public boolean isLocalAssistWingsJumpActive() {
        return localAssistWingsJumpActive;
    }

    @Override
    public Component createControlHelpMessage() {
        return Component.translatable(
                "ui.apprenticecodex.hoverride_broom.control_help",
                Component.keybind("key.jump"),
                Component.keybind("key.jump"),
                Component.keybind("key.sneak")
        );
    }
}
