package jp.aquafactory.apprenticecodex.spell.quickblink;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class QuickBlinkEvents {
    private QuickBlinkEvents() { }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) QuickBlinkRuntime.tick(player);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer target
                && QuickBlinkRuntime.active(target)) Networks.sendToPlayer(observer, QuickBlinkRuntime.packet(target));
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
