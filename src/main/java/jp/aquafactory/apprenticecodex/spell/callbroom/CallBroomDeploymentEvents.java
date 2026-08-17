package jp.aquafactory.apprenticecodex.spell.callbroom;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CallBroomDeploymentEvents {
    private CallBroomDeploymentEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CallBroomDeploymentManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CallBroomDeploymentManager.onLogout(player);
        }
    }
}
