package jp.aquafactory.apprenticecodex.spell.quickblink;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.SyncQuickBlinkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class QuickBlinkClient {
    private QuickBlinkClient() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.isPaused()) return;
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
    }
}
