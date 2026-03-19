package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.ExplorersCane;
import jp.aquafactory.apprenticecodex.mixin.EntityAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
                ((EntityAccessor) serverPlayer).apprenticecodex$getPortalEntrancePos()
        );
    }
}
