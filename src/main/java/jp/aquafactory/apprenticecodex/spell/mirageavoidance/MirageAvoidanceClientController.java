package jp.aquafactory.apprenticecodex.spell.mirageavoidance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMirageAvoidanceCastPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class MirageAvoidanceClientController {
    private static final double RECENT_MOVEMENT_EPSILON_SQ = 1.0E-6D;
    private static final int RECENT_DIRECTION_MAX_AGE_TICKS = 200;
    private static final int DIRECTION_SYNC_INTERVAL_TICKS = 5;
    private static final float DIRECTION_SYNC_DOT_THRESHOLD = 0.98F;
    private static Vec3 previousPosition;
    private static UUID previousPlayerId;
    private static ResourceKey<Level> previousDimension;
    private static MirageAvoidanceInput.DirectionInput recentMovementInput;
    private static MirageAvoidanceInput.DirectionInput lastSyncedMovementInput;
    private static long recentMovementGameTime = Long.MIN_VALUE;
    private static long lastDirectionSyncGameTime = Long.MIN_VALUE;

    private MirageAvoidanceClientController() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            resetMovementTracking();
            return;
        }

        var currentPosition = player.position();
        if (!isSameTrackedPlayer(player.getUUID(), level.dimension())) {
            previousPosition = currentPosition;
            previousPlayerId = player.getUUID();
            previousDimension = level.dimension();
            recentMovementInput = null;
            recentMovementGameTime = Long.MIN_VALUE;
            return;
        }

        if (isActive()) {
            previousPosition = currentPosition;
            return;
        }

        rememberRecentMovement(player.getYRot(), currentPosition, level.getGameTime());
        previousPosition = currentPosition;
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!isActive()) {
            return;
        }

        var input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isActive()) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!isActive() || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (isActive()) {
            event.setCanceled(true);
        }
    }

    public static boolean isActive() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return false;
        }

        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return false;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        return spellData != null
                && MirageAvoidanceEvents.isActive(level, spellData.get(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE));
    }

    public static void showDuringEffectMessage() {
        Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("ui.apprenticecodex.during_effect").withStyle(ChatFormatting.RED),
                false
        );
    }

    public static MirageAvoidanceInput.DirectionInput captureCurrentInput() {
        var recentInput = captureRecentMovementInput();
        if (recentInput != null) {
            return recentInput;
        }

        return MirageAvoidanceInput.sanitize(0.0F, 0.0F);
    }

    private static MirageAvoidanceInput.DirectionInput captureRecentMovementInput() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null) {
            return null;
        }

        if (isSameTrackedPlayer(player.getUUID(), level.dimension()) && previousPosition != null) {
            var currentTickMovement = player.position().subtract(previousPosition);
            if (hasHorizontalMovement(currentTickMovement)) {
                return MirageAvoidanceInput.fromHorizontalMovement(currentTickMovement, player.getYRot());
            }
        }

        if (recentMovementInput != null
                && level.getGameTime() - recentMovementGameTime <= RECENT_DIRECTION_MAX_AGE_TICKS) {
            return recentMovementInput;
        }

        return null;
    }

    private static void rememberRecentMovement(float yRot, Vec3 currentPosition, long gameTime) {
        if (previousPosition == null) {
            return;
        }

        var movement = currentPosition.subtract(previousPosition);
        if (hasHorizontalMovement(movement)) {
            recentMovementInput = MirageAvoidanceInput.fromHorizontalMovement(movement, yRot);
            recentMovementGameTime = gameTime;
            syncRecentMovementInput(gameTime);
        }
    }

    private static void syncRecentMovementInput(long gameTime) {
        if (recentMovementInput == null) {
            return;
        }

        if (lastSyncedMovementInput != null
                && gameTime - lastDirectionSyncGameTime < DIRECTION_SYNC_INTERVAL_TICKS
                && isSimilarDirection(lastSyncedMovementInput, recentMovementInput)) {
            return;
        }

        Networks.sendToServer(ClientMirageAvoidanceCastPacket.rememberInput(
                recentMovementInput.forward(),
                recentMovementInput.strafe()
        ));
        lastSyncedMovementInput = recentMovementInput;
        lastDirectionSyncGameTime = gameTime;
    }

    private static boolean isSimilarDirection(MirageAvoidanceInput.DirectionInput first,
                                              MirageAvoidanceInput.DirectionInput second) {
        return first.forward() * second.forward() + first.strafe() * second.strafe() >= DIRECTION_SYNC_DOT_THRESHOLD;
    }

    private static boolean isSameTrackedPlayer(UUID playerId, ResourceKey<Level> dimension) {
        return playerId.equals(previousPlayerId) && dimension.equals(previousDimension);
    }

    private static void resetMovementTracking() {
        previousPosition = null;
        previousPlayerId = null;
        previousDimension = null;
        recentMovementInput = null;
        lastSyncedMovementInput = null;
        recentMovementGameTime = Long.MIN_VALUE;
        lastDirectionSyncGameTime = Long.MIN_VALUE;
    }

    private static boolean hasHorizontalMovement(Vec3 movement) {
        return movement.x * movement.x + movement.z * movement.z > RECENT_MOVEMENT_EPSILON_SQ;
    }

}
