package jp.aquafactory.apprenticecodex.spell.quickblink;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class QuickBlinkEvents {
    private QuickBlinkEvents() { }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickBlinkRuntime.tick(player);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer target
                && QuickBlinkRuntime.active(target)) PacketDistributor.sendToPlayer(observer, QuickBlinkRuntime.packet(target));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event); }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event); }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { clear(event); }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickBlinkRuntime.clear(player);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { QuickBlinkRuntime.clearServer(); }

    private static void clear(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickBlinkRuntime.clear(player);
    }
}
