package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeOffhandAttributeBridge;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellSideEdgeOffhandAttributeBridgeEvent {
    private SpellSideEdgeOffhandAttributeBridgeEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SpellSideEdgeOffhandAttributeBridge.sync(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SpellSideEdgeOffhandAttributeBridge.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        SpellSideEdgeOffhandAttributeBridge.clear(event.getOriginal());
    }
}
