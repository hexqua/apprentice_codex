package jp.aquafactory.apprenticecodex.entity.broom;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.HoverrideBroomReleaseResultPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Optional;

public final class HoverrideBroomEntity extends AbstractBroomEntity {
    private static final EntityDataAccessor<Boolean> MANA_DEPLETED =
            SynchedEntityData.defineId(HoverrideBroomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float INPUT_EPSILON = 1.0e-4F;
    private static final float MANA_EPSILON = 1.0e-4F;
    private static final int SERVER_INPUT_TIMEOUT_TICKS = 30;
    private static final int RELEASE_RESPONSE_TIMEOUT_TICKS = 20;
    private static final int AIRBORNE_GRACE_TICKS = 40;
    private static final double HOVER_HEIGHT = 1.2D;
    private static final double VERTICAL_ACCELERATION = 0.03D;
    private static final double MAX_MOUNTED_VERTICAL_SPEED = 0.15D;
    private static final double MAX_UNMOUNTED_VERTICAL_SPEED = 0.10D;
    private static final double UNMOUNTED_HORIZONTAL_DAMPING = 0.85D;
    private static final float TURN_ACCELERATION = 1.0F;
    private static final float MAX_TURN_SPEED = 10.0F;
    private static final float TURN_DAMPING = 0.9F;

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
    private boolean localGlideActive;
    private Vec3 localInertia = Vec3.ZERO;
    private long pendingReleaseSequence = Long.MIN_VALUE;
    private int pendingReleaseTicks;
    private boolean localReleaseImpulsePending;
    private int localAirborneTicks;
    private float localTurnSpeed;

    private float serverForwardInput;
    private boolean serverGlideRequested;
    private boolean serverGlideActive;
    private int serverSuccessfulGlideTicks;
    private long lastServerInputGameTime = Long.MIN_VALUE;
    private long lastServerActionSequence;
    private int serverAirborneTicks;

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
    }

    @Override
    public void setLocalInput(float strafe, float forward, boolean ascending, boolean descending) {
        super.setLocalInput(strafe, forward, ascending, descending);
        localStrafeInput = sanitizeInput(strafe);
        localForwardInput = sanitizeInput(forward);
        if (ascending && !isManaDepleted() && !isDamaged()
                && !localGlideActive && pendingReleaseSequence == Long.MIN_VALUE) {
            localGlideActive = true;
            localInertia = HoverrideBroomMovement.horizontal(getDeltaMovement());
        }
    }

    @Override
    public void handleLocalInputTransition(BroomInputTransition transition, long actionSequence) {
        if (transition == BroomInputTransition.CANCEL) {
            cancelLocalGlide();
        } else if (transition == BroomInputTransition.RELEASE && localGlideActive) {
            localGlideActive = false;
            pendingReleaseSequence = actionSequence;
            pendingReleaseTicks = 0;
        }
    }

    public void acceptLocalReleaseResult(long sequence, boolean accepted, double minimumHorizontalSpeed) {
        if (sequence != pendingReleaseSequence) {
            return;
        }
        pendingReleaseSequence = Long.MIN_VALUE;
        pendingReleaseTicks = 0;
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
        } else if (isManaDepleted() && localGlideActive) {
            localGlideActive = false;
            localInertia = Vec3.ZERO;
        }
        if (pendingReleaseSequence != Long.MIN_VALUE
                && ++pendingReleaseTicks > RELEASE_RESPONSE_TIMEOUT_TICKS) {
            pendingReleaseSequence = Long.MIN_VALUE;
            pendingReleaseTicks = 0;
            localInertia = Vec3.ZERO;
        }

        var surface = BroomSurfaceScanner.findSurfaceBelow(level(), getX(), getY(), getZ(), 4, false);
        localAirborneTicks = surface.isPresent() ? 0 : Math.min(AIRBORNE_GRACE_TICKS, localAirborneTicks + 1);

        var movement = getDeltaMovement();
        var gliding = localGlideActive || pendingReleaseSequence != Long.MIN_VALUE;
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
        var vertical = mountedVertical(movement.y, surface.orElse(Double.NaN));
        if (isDamaged()) {
            vertical = -MAX_MOUNTED_VERTICAL_SPEED;
        }

        var beforeMove = position();
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
        if (gliding) {
            var actualMovement = position().subtract(beforeMove);
            localInertia = HoverrideBroomMovement.horizontal(actualMovement);
            setDeltaMovement(localInertia.x, getDeltaMovement().y, localInertia.z);
        }
    }

    @Override
    protected void applyUnoccupiedMovement() {
        var movement = getDeltaMovement();
        var horizontal = HoverrideBroomMovement.horizontal(movement).scale(UNMOUNTED_HORIZONTAL_DAMPING);
        var surface = BroomSurfaceScanner.findSurfaceBelow(level(), getX(), getY(), getZ(), 4, false);
        var vertical = surface.isPresent()
                ? hoverVertical(movement.y, surface.getAsDouble(), MAX_UNMOUNTED_VERTICAL_SPEED)
                : Math.max(-MAX_UNMOUNTED_VERTICAL_SPEED, movement.y - VERTICAL_ACCELERATION);
        setDeltaMovement(horizontal.x, vertical, horizontal.z);
        move(MoverType.SELF, getDeltaMovement());
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
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var mana = magicData == null ? 0.0F : magicData.getMana();
        var config = ApprenticeCodexServerConfig.hoverrideBroomConfig();
        if (isManaDepleted()) {
            if (mana + MANA_EPSILON >= config.inertiaReleaseManaCost()) {
                setManaDepleted(false);
                player.displayClientMessage(Component.translatable(
                        messageKeys().manaRecovered()
                ).withStyle(ChatFormatting.GREEN), true);
            }
            return;
        }
        if (mana <= MANA_EPSILON) {
            enterManaDepleted(player);
            return;
        }

        if (serverGlideRequested) {
            serverGlideActive = true;
            serverSuccessfulGlideTicks++;
            consumeMana(player, magicData, (float)config.inertiaGlideManaCostPerTick());
            if (isManaDepleted()) {
                cancelServerGlide();
            }
            return;
        }

        if (serverForwardInput > INPUT_EPSILON && serverAirborneTicks < AIRBORNE_GRACE_TICKS) {
            consumeMana(player, magicData, (float)config.forwardManaCostPerTick());
        }
    }

    private void handleServerRelease(Player player, long actionSequence) {
        var accepted = serverGlideActive && serverSuccessfulGlideTicks > 0 && !isManaDepleted() && !isDamaged();
        var config = ApprenticeCodexServerConfig.hoverrideBroomConfig();
        if (accepted && !player.getAbilities().instabuild) {
            var magicData = MagicData.getPlayerMagicData(player);
            consumeMana(player, magicData, (float)config.inertiaReleaseManaCost());
        }
        if (accepted) {
            serverAirborneTicks = 0;
        }
        cancelServerGlide();

        sendReleaseResult(player, actionSequence, accepted);
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
        if (isManaDepleted()
                || ApprenticeCodexServerConfig.hoverrideBroomConfig().inertiaReleaseManaCost() <= MANA_EPSILON) {
            return;
        }
        setManaDepleted(true);
        cancelServerGlide();
        player.displayClientMessage(Component.translatable(
                messageKeys().manaDepleted()
        ).withStyle(ChatFormatting.RED), true);
    }

    private void updateServerSurfaceState() {
        var surface = BroomSurfaceScanner.findSurfaceBelow(level(), getX(), getY(), getZ(), 4, false);
        serverAirborneTicks = surface.isPresent()
                ? 0
                : Math.min(AIRBORNE_GRACE_TICKS, serverAirborneTicks + 1);
    }

    private void cancelLocalGlide() {
        localGlideActive = false;
        localInertia = Vec3.ZERO;
        pendingReleaseSequence = Long.MIN_VALUE;
        pendingReleaseTicks = 0;
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
        lastServerActionSequence = 0L;
        serverAirborneTicks = 0;
        clearRidingInput();
        cancelLocalGlide();
        localAirborneTicks = 0;
    }

    public boolean isManaDepleted() {
        return entityData.get(MANA_DEPLETED);
    }

    private void setManaDepleted(boolean value) {
        entityData.set(MANA_DEPLETED, value);
    }

    public boolean isServerInertiaGlideActive() {
        return serverGlideActive;
    }

    public int getServerSuccessfulGlideTicks() {
        return serverSuccessfulGlideTicks;
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
