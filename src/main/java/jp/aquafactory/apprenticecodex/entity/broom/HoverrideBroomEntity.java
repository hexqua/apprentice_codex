package jp.aquafactory.apprenticecodex.entity.broom;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.broom.HoverrideBroomItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomAssistWingsJumpPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomImpulseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomReleaseResultPacket;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Comparator;

public final class HoverrideBroomEntity extends AbstractBroomEntity {
    private static final EntityDataAccessor<Boolean> MANA_DEPLETED =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AIRBORNE_ACCELERATION_LOCKED =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PRESENTATION_STATE =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUSH_ATTACK_ACTIVE =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RUSH_DIRECTION_X =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RUSH_DIRECTION_Z =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.FLOAT);
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
    private int serverControlledTick;
    private Vec3 lastRushObservationPosition;
    private int lastRushObservationTick = Integer.MIN_VALUE;

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
        entityData.define(RUSH_ATTACK_ACTIVE, false);
        entityData.define(RUSH_DIRECTION_X, 0.0F);
        entityData.define(RUSH_DIRECTION_Z, 1.0F);
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
                    minimumHorizontalSpeed,
                    HoverrideBroomMovement.maximumHorizontalSpeed(isOverdriveEnabled())
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
                        !isManaDepleted() && !isDamaged() && localAirborneTicks < AIRBORNE_GRACE_TICKS,
                        isOverdriveEnabled()
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
        return HoverrideBroomMovement.maximumHorizontalSpeed(isOverdriveEnabled());
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
        serverControlledTick++;
        updateServerSurfaceState();
        var rushMovement = observeRushMovement();
        if (isDamaged()) {
            cancelServerGlide();
            setRushAttackActive(false);
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
            updateRushAttack(player, rushMovement, ApprenticeCodexServerConfig.hoverrideBroomConfig(), true);
            updateServerPresentation();
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var mana = magicData == null ? 0.0F : magicData.getMana();
        var config = ApprenticeCodexServerConfig.hoverrideBroomConfig();
        if (isManaDepleted()) {
            setRushAttackActive(false);
            if (mana + MANA_EPSILON >= inertiaReleaseManaCost(config)) {
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
            setRushAttackActive(false);
            enterManaDepleted(player);
            updateServerPresentation();
            return;
        }

        double movementManaCost = 0.0D;
        if (serverGlideRequested) {
            serverGlideActive = true;
            serverSuccessfulGlideTicks++;
            movementManaCost = inertiaGlideManaCostPerTick(config);
        } else if (serverForwardInput > INPUT_EPSILON && serverAirborneTicks < AIRBORNE_GRACE_TICKS) {
            movementManaCost = forwardManaCostPerTick(config);
        }

        var rushRequested = canStartRushAttack(rushMovement);
        var totalManaCost = movementManaCost + (rushRequested ? config.rushManaCostPerTick() : 0.0D);
        var rushPaid = rushRequested && mana + MANA_EPSILON >= totalManaCost;
        consumeMana(player, magicData, (float)totalManaCost);
        if (rushPaid) {
            updateRushAttack(player, rushMovement, config, true);
        } else {
            setRushAttackActive(false);
        }
        if (isManaDepleted()) {
            cancelServerGlide();
        }
        if (!isManaDepleted()) {
            updateLowManaWarning(player, manaAfterConsumption(magicData), config);
        }
        updateServerPresentation();
    }

    private Optional<RushMovement> observeRushMovement() {
        var now = serverControlledTick;
        var currentPosition = position();
        if (lastRushObservationPosition == null || lastRushObservationTick == Integer.MIN_VALUE) {
            lastRushObservationPosition = currentPosition;
            lastRushObservationTick = now;
            return Optional.empty();
        }

        var elapsedTicks = now - lastRushObservationTick;
        var movement = currentPosition.subtract(lastRushObservationPosition);
        if (movement.lengthSqr() <= 1.0e-8D) {
            if (elapsedTicks > HoverrideBroomRushAttack.MAX_OBSERVATION_TICKS) {
                lastRushObservationPosition = currentPosition;
                lastRushObservationTick = now;
            }
            return Optional.empty();
        }

        lastRushObservationPosition = currentPosition;
        lastRushObservationTick = now;
        if (elapsedTicks < 1L || elapsedTicks > HoverrideBroomRushAttack.MAX_OBSERVATION_TICKS
                || !Double.isFinite(movement.x) || !Double.isFinite(movement.y) || !Double.isFinite(movement.z)) {
            return Optional.empty();
        }

        var horizontal = HoverrideBroomMovement.horizontal(movement);
        var allowedHorizontal = (HoverrideBroomMovement.maximumHorizontalSpeed(isOverdriveEnabled())
                + HoverrideBroomRushAttack.MOVEMENT_TOLERANCE_PER_TICK) * elapsedTicks;
        var allowedVertical = (ASSIST_WINGS_MAX_JUMP_HEIGHT
                + HoverrideBroomRushAttack.MOVEMENT_TOLERANCE_PER_TICK) * elapsedTicks;
        if (horizontal.length() > allowedHorizontal || Math.abs(movement.y) > allowedVertical) {
            return Optional.empty();
        }

        var speed = horizontal.length() / elapsedTicks;
        if (speed < HoverrideBroomRushAttack.MINIMUM_SPEED || horizontal.lengthSqr() <= 1.0e-8D) {
            return Optional.empty();
        }
        return Optional.of(new RushMovement(movement, horizontal.normalize(), speed));
    }

    private boolean canStartRushAttack(Optional<RushMovement> movement) {
        return movement.isPresent()
                && isVehicle()
                && getControllingPassenger() instanceof Player
                && HoverrideBroomItem.isRushStyleEnabled(getBroomItemStack());
    }

    private void updateRushAttack(
            Player player,
            Optional<RushMovement> movement,
            HoverrideBroomServerConfig.Values config,
            boolean costPaid
    ) {
        if (!costPaid || !canStartRushAttack(movement)) {
            setRushAttackActive(false);
            return;
        }

        var rushMovement = movement.orElseThrow();
        setRushDirection(rushMovement.direction());
        setRushAttackActive(true);
        applyRushAttack(player, rushMovement, config);
    }

    private void applyRushAttack(
            Player player,
            RushMovement rushMovement,
            HoverrideBroomServerConfig.Values config
    ) {
        var currentBox = getBoundingBox();
        var startBox = currentBox.move(rushMovement.displacement().reverse());
        var searchBox = currentBox.minmax(startBox).inflate(HoverrideBroomRushAttack.CONTACT_PADDING);
        var targets = CombatTools.resolveUniqueCombatTargets(level().getEntities(this, searchBox)).stream()
                .filter(target -> CombatTools.isValidCombatTarget(target, player))
                .filter(target -> HoverrideBroomRushAttack.intersectsPath(
                        currentBox, rushMovement.displacement(), target.getBoundingBox()))
                .sorted(Comparator.comparingDouble(target -> HoverrideBroomRushAttack.pathEntryDistanceSqr(
                        currentBox, rushMovement.displacement(), target.getBoundingBox())))
                .limit(HoverrideBroomRushAttack.MAX_TARGETS_PER_TICK)
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        var lightningSchool = SchoolRegistry.LIGHTNING.get();
        var baseDamage = HoverrideBroomRushAttack.baseDamage(
                rushMovement.speed(), config.rushMinimumDamage(), config.rushMaximumDamage());
        var scaledDamage = baseDamage
                * (float)player.getAttributeValue(AttributeRegistry.SPELL_POWER)
                * (float)lightningSchool.getPowerFor(player);
        var source = CombatTools.getDamageSource(level(), this, player, DamageTypes.HOVERRIDE_BROOM);
        var knockback = HoverrideBroomRushAttack.knockbackStrength(rushMovement.speed());
        var hitSoundPlayed = false;
        for (var target : targets) {
            var applied = CombatTools.applyDamage(
                    target,
                    scaledDamage,
                    source,
                    lightningSchool,
                    CombatTools.KnockbackTypes.NO_KNOCKBACK
            );
            if (!applied) {
                continue;
            }
            spawnRushHitEffects(target, !hitSoundPlayed);
            hitSoundPlayed = true;
            if (target instanceof LivingEntity livingTarget) {
                // LivingEntity#knockbackは指定方向を減算するため、進行方向へ飛ばすには符号を反転する。
                livingTarget.knockback(knockback, -rushMovement.direction().x, -rushMovement.direction().z);
            }
        }
    }

    private void spawnRushHitEffects(Entity target, boolean playSound) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var impact = target.getBoundingBox().getCenter();
        MagicManager.spawnParticles(
                serverLevel,
                ParticleHelper.ELECTRIC_SPARKS,
                impact.x, impact.y, impact.z,
                14,
                0.18D, 0.25D, 0.18D,
                0.18D,
                true
        );
        MagicManager.spawnParticles(
                serverLevel,
                new AdditiveGlowParticleOptions(
                        ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                        0.18F,
                        0.48F, 0.72F, 1.0F,
                        2,
                        8,
                        3,
                        0.75F,
                        1.25F,
                        0.7F,
                        1.0F,
                        0.02F,
                        0.5F,
                        0.35F,
                        true
                ),
                impact.x, impact.y, impact.z,
                5,
                0.2D, 0.2D, 0.2D,
                0.08D,
                true
        );
        if (playSound) {
            serverLevel.playSound(
                    null,
                    impact.x, impact.y, impact.z,
                    io.redspace.ironsspellbooks.registries.SoundRegistry.SMALL_LIGHTNING_STRIKE.get(),
                    SoundSource.PLAYERS,
                    0.9F,
                    1.1F + serverLevel.random.nextFloat() * 0.2F
            );
        }
    }

    private record RushMovement(Vec3 displacement, Vec3 direction, double speed) {
    }

    private float manaAfterConsumption(MagicData magicData) {
        return magicData == null ? 0.0F : magicData.getMana();
    }

    private double forwardManaCostPerTick(HoverrideBroomServerConfig.Values config) {
        return isOverdriveEnabled()
                ? config.overdriveForwardManaCostPerTick()
                : config.forwardManaCostPerTick();
    }

    private double inertiaGlideManaCostPerTick(HoverrideBroomServerConfig.Values config) {
        return isOverdriveEnabled()
                ? config.overdriveInertiaGlideManaCostPerTick()
                : config.inertiaGlideManaCostPerTick();
    }

    private double inertiaReleaseManaCost(HoverrideBroomServerConfig.Values config) {
        return isOverdriveEnabled()
                ? config.overdriveInertiaReleaseManaCost()
                : config.inertiaReleaseManaCost();
    }

    private void updateLowManaWarning(
            Player player,
            float mana,
            HoverrideBroomServerConfig.Values config
    ) {
        var recoveryThreshold = inertiaReleaseManaCost(config);
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
            consumeMana(player, magicData, (float)inertiaReleaseManaCost(config));
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
        var overdriveEnabled = isOverdriveEnabled();
        var maximumSpeed = HoverrideBroomMovement.maximumHorizontalSpeed(overdriveEnabled);
        var minimumSpeedRatio = overdriveEnabled
                ? Math.max(
                        config.inertiaReleaseMinimumSpeedRatio(),
                        HoverrideBroomMovement.OVERDRIVE_INERTIA_RELEASE_MINIMUM_SPEED_RATIO
                )
                : config.inertiaReleaseMinimumSpeedRatio();
        Networks.sendToPlayer(serverPlayer, new HoverrideBroomReleaseResultPacket(
                getId(),
                actionSequence,
                accepted,
                maximumSpeed * minimumSpeedRatio
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
        setRushAttackActive(false);
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
        serverControlledTick = 0;
        lastRushObservationPosition = null;
        lastRushObservationTick = Integer.MIN_VALUE;
        setRushAttackActive(false);
        setRushDirection(new Vec3(0.0D, 0.0D, 1.0D));
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

    public boolean isRushAttackActive() {
        return entityData.get(RUSH_ATTACK_ACTIVE);
    }

    private void setRushAttackActive(boolean active) {
        entityData.set(RUSH_ATTACK_ACTIVE, active);
    }

    public Vec3 getRushAttackDirection() {
        return new Vec3(entityData.get(RUSH_DIRECTION_X), 0.0D, entityData.get(RUSH_DIRECTION_Z));
    }

    private void setRushDirection(Vec3 direction) {
        var horizontal = HoverrideBroomMovement.horizontal(direction);
        if (!Double.isFinite(horizontal.x) || !Double.isFinite(horizontal.z)
                || horizontal.lengthSqr() <= 1.0e-8D) {
            return;
        }
        var normalized = horizontal.normalize();
        entityData.set(RUSH_DIRECTION_X, (float)normalized.x);
        entityData.set(RUSH_DIRECTION_Z, (float)normalized.z);
    }

    @Override
    public boolean isPushable() {
        return !isRushAttackActive();
    }

    @Override
    public boolean canCollideWith(@NotNull Entity other) {
        // Entity#moveの衝突解決はisPushableとは別にcanCollideWithを参照するため、
        // 突進中だけモブ等のentity collision shapeを無視する。ブロック衝突には影響しない。
        return !isRushAttackActive() && super.canCollideWith(other);
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
                Component.keybind("key.sneak")
        );
    }
}
