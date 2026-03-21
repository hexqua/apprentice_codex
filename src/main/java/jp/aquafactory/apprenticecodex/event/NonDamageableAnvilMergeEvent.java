package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeHelper;
import jp.aquafactory.apprenticecodex.item.NonDamageableAnvilMergeItem;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class NonDamageableAnvilMergeEvent {
    private NonDamageableAnvilMergeEvent() {
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        var leftStack = event.getLeft();
        var rightStack = event.getRight();
        if (leftStack.isEmpty() || rightStack.isEmpty()) {
            return;
        }
        if (leftStack.getItem() != rightStack.getItem()) {
            return;
        }
        if (!(leftStack.getItem() instanceof NonDamageableAnvilMergeItem)) {
            return;
        }

        var result = NonDamageableAnvilMergeHelper.tryMergeSameItem(
                leftStack,
                rightStack,
                event.getName(),
                event.getPlayer()
        );
        if (result == null) {
            return;
        }

        event.setOutput(result.output());
        event.setCost(result.cost());
        event.setMaterialCost(result.materialCost());
    }
}
