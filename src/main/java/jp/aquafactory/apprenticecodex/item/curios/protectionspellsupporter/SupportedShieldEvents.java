package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
