package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SearchBeaconLifecycleEvents {
    private SearchBeaconLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        recoverPendingInstantBrazier(event);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        recoverPendingInstantBrazier(event);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        recoverPendingInstantBrazier(event);
    }

    private static void recoverPendingInstantBrazier(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Search Beacon 自体は保存しないため、world 境界をまたいだ未確定分はプレイヤーへ戻す。
            SearchBeaconRefundManager.recoverPending(player);
        }
    }
}
