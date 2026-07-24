package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OtherworldLensEvents {
    private OtherworldLensEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OtherworldLensSessionManager.finish(player, OtherworldLensSessionManager.EndReason.DEATH);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OtherworldLensSessionManager.finish(player, OtherworldLensSessionManager.EndReason.CANCELLED);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OtherworldLensSessionManager.finish(player, OtherworldLensSessionManager.EndReason.LOGOUT);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OtherworldLensSessionManager.finish(player, OtherworldLensSessionManager.EndReason.DIMENSION_CHANGED);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        OtherworldLensSessionManager.finishAll(event.getServer(), OtherworldLensSessionManager.EndReason.SERVER_STOPPING);
    }
}
