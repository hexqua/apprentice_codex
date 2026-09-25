package jp.aquafactory.apprenticecodex.spell.quickblink;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientQuickBlinkInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncQuickBlinkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class QuickBlinkClient {
    private static float lastForward = Float.NaN;
    private static float lastStrafe = Float.NaN;
    private static long lastSent = Long.MIN_VALUE;

    private QuickBlinkClient() { }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.isPaused()) return;
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        long time = player.level().getGameTime();
        if (forward != lastForward || strafe != lastStrafe || time - lastSent >= 5) {
            Networks.sendToServer(new ClientQuickBlinkInputPacket(forward, strafe));
            lastForward = forward;
            lastStrafe = strafe;
            lastSent = time;
        }
        QuickBlinkRuntime.state(player).blink.update(player);
    }

    public static void accept(SyncQuickBlinkPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(packet.entityId()) instanceof Player player) {
            QuickBlinkRuntime.accept(player, packet);
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        QuickBlinkRuntime.clearClient();
        lastForward = Float.NaN;
        lastStrafe = Float.NaN;
        lastSent = Long.MIN_VALUE;
    }
}
