package jp.aquafactory.apprenticecodex.spell.fieldoverseer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FieldOverseerEvents {
    private FieldOverseerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FieldOverseerManager.cancel(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 固定設置物を別ディメンションへ移さず、手動解除と同じ再詠唱終了として扱う。
            FieldOverseerManager.cancel(player);
        }
    }
}
