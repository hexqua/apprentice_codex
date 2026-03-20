package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ExplorersCaneTrackingEvent {
    private ExplorersCaneTrackingEvent() {
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || serverPlayer.serverLevel().dimension() != Level.NETHER) {
            return;
        }

        // ポータル由来で取得できる位置を優先し、取れないケースだけ近傍探索へフォールバックする.
        ExplorersCane.captureNetherPortalDestination(
                serverPlayer,
                serverPlayer.portalProcess == null ? null : serverPlayer.portalProcess.getEntryPosition()
        );
    }
}
