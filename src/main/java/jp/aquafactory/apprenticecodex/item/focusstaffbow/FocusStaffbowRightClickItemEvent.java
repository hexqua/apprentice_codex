package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FocusStaffbowRightClickItemEvent {
    private FocusStaffbowRightClickItemEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        var player = event.getEntity();
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof FocusStaffbow)) {
            return;
        }

        // 1.21.1 の ISS は CASTING_IMPLEMENT を RightClickItem で先に処理するため、
        // FocusStaffbow の独自 use() をここで先に通して通常詠唱へのフォールバックを防ぐ。
        var result = stack.getItem().use(player.level(), player, event.getHand());
        if (result.getResult() == net.minecraft.world.InteractionResult.PASS) {
            return;
        }

        event.setCancellationResult(result.getResult());
        event.setCanceled(true);
    }
}
