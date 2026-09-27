package jp.aquafactory.apprenticecodex.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSpellMovementInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SpellMovementInputClient {
    private static float lastForward = Float.NaN;
    private static float lastStrafe = Float.NaN;
    private static long lastSent = Long.MIN_VALUE;
    private static UUID lastPlayerId;
    private static ResourceKey<Level> lastDimension;

    private SpellMovementInputClient() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        var dimension = player.level().dimension();
        if (!player.getUUID().equals(lastPlayerId) || !dimension.equals(lastDimension)) {
            reset();
            lastPlayerId = player.getUUID();
            lastDimension = dimension;
        }
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        long time = player.level().getGameTime();
        if (forward != lastForward || strafe != lastStrafe || time - lastSent >= 5) {
            Networks.sendToServer(new ClientSpellMovementInputPacket(forward, strafe));
            lastForward = forward;
            lastStrafe = strafe;
            lastSent = time;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        lastForward = Float.NaN;
        lastStrafe = Float.NaN;
        lastSent = Long.MIN_VALUE;
        lastPlayerId = null;
        lastDimension = null;
    }
}
