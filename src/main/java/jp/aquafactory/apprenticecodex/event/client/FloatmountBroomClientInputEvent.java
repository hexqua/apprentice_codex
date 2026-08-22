package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientFloatmountBroomInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientFloatmountBroomDismountInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class FloatmountBroomClientInputEvent {
    private static final int HEARTBEAT_TICKS = 10;
    private static final int DISMOUNT_TRACKING_GRACE_TICKS = FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS;
    private static ClientFloatmountBroomInputPacket lastSentInput = ClientFloatmountBroomInputPacket.inactive();
    private static boolean inputActive;
    private static int heartbeat;
    private static int trackedBroomId = -1;
    private static boolean lastSentSneaking;
    private static int missingTrackedBroomTicks;

    private FloatmountBroomClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTickPre(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetDismountInput();
            return;
        }

        if (player.getVehicle() instanceof FloatmountBroomEntity broom
                && broom.getControllingPassenger() == player) {
            if (trackedBroomId != broom.getId()) {
                resetInput();
                trackedBroomId = broom.getId();
                lastSentSneaking = false;
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
            Networks.sendToServer(new ClientFloatmountBroomDismountInputPacket(trackedBroomId, sneaking));
            lastSentSneaking = sneaking;
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null
                || !(player.getVehicle() instanceof FloatmountBroomEntity broom)
                || broom.getControllingPassenger() != player) {
            resetInput();
            return;
        }
        if (minecraft.screen != null) {
            // serverへの停止通知だけではclient予測に使う直前の入力が箒へ残るため、同じtickで解除する。
            broom.setLocalInput(0.0F, 0.0F, false, false);
            resetInput();
            return;
        }

        var options = minecraft.options;
        var strafe = (options.keyRight.isDown() ? 1.0F : 0.0F) - (options.keyLeft.isDown() ? 1.0F : 0.0F);
        var forward = (options.keyUp.isDown() ? 1.0F : 0.0F) - (options.keyDown.isDown() ? 1.0F : 0.0F);
        var ascending = options.keyJump.isDown();
        var descending = options.keySprint.isDown();
        broom.setLocalInput(strafe, forward, ascending, descending);

        var input = new ClientFloatmountBroomInputPacket(strafe, forward, ascending, descending);
        heartbeat++;
        if (!input.equals(lastSentInput) || heartbeat >= HEARTBEAT_TICKS) {
            Networks.sendToServer(input);
            lastSentInput = input;
            inputActive = true;
            heartbeat = 0;
        }
    }

    private static void resetInput() {
        if (inputActive && !lastSentInput.equals(ClientFloatmountBroomInputPacket.inactive())) {
            Networks.sendToServer(ClientFloatmountBroomInputPacket.inactive());
        }
        lastSentInput = ClientFloatmountBroomInputPacket.inactive();
        inputActive = false;
        heartbeat = 0;
    }

    private static void resetDismountInput() {
        trackedBroomId = -1;
        lastSentSneaking = false;
        missingTrackedBroomTicks = 0;
    }
}
