package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SupportedShieldEvents {
    private SupportedShieldEvents() {}

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof SupportedShieldEntity shield) {
            shield.startTracking(player);
        }
    }

    @SubscribeEvent
    public static void stopTracking(PlayerEvent.StopTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof SupportedShieldEntity shield) {
            shield.stopTracking(player);
        }
    }
}
