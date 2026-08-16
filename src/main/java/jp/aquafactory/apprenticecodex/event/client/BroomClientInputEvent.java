package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.BroomInputTransition;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientBroomInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBroomDismountInputPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class BroomClientInputEvent {
    private static final int HEARTBEAT_TICKS = 10;
    private static final int DISMOUNT_TRACKING_GRACE_TICKS = AbstractBroomEntity.DISMOUNT_CONFIRM_TICKS;
    private static ClientBroomInputPacket lastSentInput = ClientBroomInputPacket.inactive();
    private static boolean inputActive;
    private static int heartbeat;
    private static int trackedBroomId = -1;
    private static boolean lastSentSneaking;
    private static int missingTrackedBroomTicks;
    private static boolean lastGlideHeld;
    private static boolean lastBroomWasHoverride;
    private static long nextActionSequence = 1L;

    private BroomClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetDismountInput();
            return;
        }

        if (player.getVehicle() instanceof AbstractBroomEntity broom
                && broom.getControllingPassenger() == player) {
            if (trackedBroomId != broom.getId()) {
                resetInput(true);
                trackedBroomId = broom.getId();
                lastSentSneaking = false;
                lastBroomWasHoverride = broom instanceof HoverrideBroomEntity;
            }
            missingTrackedBroomTicks = 0;
        } else if (trackedBroomId != -1
                && ++missingTrackedBroomTicks > DISMOUNT_TRACKING_GRACE_TICKS) {
            resetDismountInput();
            return;
        }

        if (trackedBroomId == -1) {
            return;
        }

        var sneaking = minecraft.options.keyShift.isDown();
        if (sneaking != lastSentSneaking) {
            Networks.sendToServer(new ClientBroomDismountInputPacket(trackedBroomId, sneaking));
            lastSentSneaking = sneaking;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null
                || !(player.getVehicle() instanceof AbstractBroomEntity broom)
                || broom.getControllingPassenger() != player) {
            resetInput(true);
            return;
        }
        if (minecraft.screen != null) {
            // serverへの停止通知だけではclient予測に使う直前の入力が箒へ残るため、同じtickで解除する。
            broom.setLocalInput(0.0F, 0.0F, false, false);
            if (broom instanceof HoverrideBroomEntity && lastGlideHeld) {
                var sequence = nextSequence();
                broom.handleLocalInputTransition(BroomInputTransition.CANCEL, sequence);
            }
            resetInput(true);
            return;
        }

        var options = minecraft.options;
        var strafe = (options.keyRight.isDown() ? 1.0F : 0.0F) - (options.keyLeft.isDown() ? 1.0F : 0.0F);
        var forward = broom instanceof HoverrideBroomEntity
                ? options.keyDown.isDown() ? -1.0F : options.keyUp.isDown() ? 1.0F : 0.0F
                : (options.keyUp.isDown() ? 1.0F : 0.0F) - (options.keyDown.isDown() ? 1.0F : 0.0F);
        var ascending = options.keyJump.isDown();
        var descending = options.keySprint.isDown();
        broom.setLocalInput(strafe, forward, ascending, descending);

        var transition = broom instanceof HoverrideBroomEntity && lastGlideHeld && !ascending
                ? BroomInputTransition.RELEASE
                : BroomInputTransition.NONE;
        var sequence = transition == BroomInputTransition.NONE ? 0L : nextSequence();
        if (transition != BroomInputTransition.NONE) {
            broom.handleLocalInputTransition(transition, sequence);
        }
        var input = new ClientBroomInputPacket(
                strafe,
                forward,
                ascending,
                descending,
                transition,
                sequence
        );
        heartbeat++;
        if (transition != BroomInputTransition.NONE
                || !input.withoutTransition().equals(lastSentInput)
                || heartbeat >= HEARTBEAT_TICKS) {
            Networks.sendToServer(input);
            lastSentInput = input.withoutTransition();
            inputActive = true;
            heartbeat = 0;
        }
        lastGlideHeld = broom instanceof HoverrideBroomEntity && ascending;
        lastBroomWasHoverride = broom instanceof HoverrideBroomEntity;
    }

    private static void resetInput(boolean cancelGlide) {
        if (inputActive && !lastSentInput.equals(ClientBroomInputPacket.inactive())) {
            var transition = cancelGlide && lastBroomWasHoverride && lastGlideHeld
                    ? BroomInputTransition.CANCEL
                    : BroomInputTransition.NONE;
            Networks.sendToServer(new ClientBroomInputPacket(
                    0.0F,
                    0.0F,
                    false,
                    false,
                    transition,
                    transition == BroomInputTransition.NONE ? 0L : nextSequence()
            ));
        }
        lastSentInput = ClientBroomInputPacket.inactive();
        inputActive = false;
        heartbeat = 0;
        lastGlideHeld = false;
        lastBroomWasHoverride = false;
    }

    private static void resetDismountInput() {
        trackedBroomId = -1;
        lastSentSneaking = false;
        missingTrackedBroomTicks = 0;
    }

    private static long nextSequence() {
        if (nextActionSequence == Long.MAX_VALUE) {
            nextActionSequence = 1L;
        }
        return nextActionSequence++;
    }
}
