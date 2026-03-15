package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CrystalBladedStaffRightClickItemEvent {
    private CrystalBladedStaffRightClickItemEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        var player = event.getEntity();
        if (!CrystalBladedStaff.isCrystalBladedStaff(player.getMainHandItem())) {
            return;
        }

        if (!CrystalBladedStaff.shouldPrioritizeOffhandUse(player)) {
            return;
        }

        // ISS の CASTING_IMPLEMENT は RightClickItem で先に詠唱を確定するため、
        // Crystal 側だけ先に PASS 返却してオフハンド盾・spell gun の後続処理へ流す。
        event.setCancellationResult(InteractionResult.PASS);
        event.setCanceled(true);
    }
}
