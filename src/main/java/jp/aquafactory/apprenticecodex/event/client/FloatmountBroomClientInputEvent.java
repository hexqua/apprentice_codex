package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientFloatmountBroomDescentPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientFloatmountBroomDismountInputPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class FloatmountBroomClientInputEvent {
    private static final int HEARTBEAT_TICKS = 10;
    private static final int DISMOUNT_TRACKING_GRACE_TICKS = FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS;
    private static boolean lastSentDescending;
    private static int heartbeat;
    private static int trackedBroomId = -1;
    private static boolean lastSentSneaking;
    private static int missingTrackedBroomTicks;

    private FloatmountBroomClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetDismountInput();
            return;
        }

        if (player.getVehicle() instanceof FloatmountBroomEntity broom
                && broom.getControllingPassenger() == player) {
            if (trackedBroomId != broom.getId()) {
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
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null
                || minecraft.screen != null
                || !(player.getVehicle() instanceof FloatmountBroomEntity broom)
                || broom.getControllingPassenger() != player) {
            resetInput();
            return;
        }

        var options = minecraft.options;
        var strafe = (options.keyRight.isDown() ? 1.0F : 0.0F) - (options.keyLeft.isDown() ? 1.0F : 0.0F);
        var forward = (options.keyUp.isDown() ? 1.0F : 0.0F) - (options.keyDown.isDown() ? 1.0F : 0.0F);
        var ascending = options.keyJump.isDown();
        var descending = options.keySprint.isDown();
        broom.setLocalInput(strafe, forward, ascending, descending);

        heartbeat++;
        if (descending != lastSentDescending || heartbeat >= HEARTBEAT_TICKS) {
            Networks.sendToServer(new ClientFloatmountBroomDescentPacket(descending));
            lastSentDescending = descending;
            heartbeat = 0;
        }
    }

    private static void resetInput() {
        if (lastSentDescending) {
            Networks.sendToServer(new ClientFloatmountBroomDescentPacket(false));
        }
        lastSentDescending = false;
        heartbeat = 0;
    }

    private static void resetDismountInput() {
        trackedBroomId = -1;
        lastSentSneaking = false;
        missingTrackedBroomTicks = 0;
    }
}
