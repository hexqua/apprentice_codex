package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CircuitHeatStaffRightClickItemEvent {
    private CircuitHeatStaffRightClickItemEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof CircuitHeatStaff)) {
            return;
        }

        // 1.21.1 の ISS は CASTING_IMPLEMENT を RightClickItem で処理するため、
        // Circuit Heat Staff の cooldown bypass は Iron's より先に独自 use() を通す。
        var result = stack.getItem().use(event.getLevel(), event.getEntity(), event.getHand());
        if (result.getResult() == net.minecraft.world.InteractionResult.PASS) {
            return;
        }

        event.setCancellationResult(result.getResult());
        event.setCanceled(true);
    }
}
