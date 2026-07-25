package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.ether.EtherItem;
import com.sammy.malum.registry.common.item.MalumDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

final class MalumLuminousDeviceBridgeImpl {
    private MalumLuminousDeviceBridgeImpl() {
    }

    static boolean isSupportedEther(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof EtherItem;
    }

    static void normalizeForStorage(ItemStack stack) {
        if (!isSupportedEther(stack)) {
            return;
        }

        var etherItem = (EtherItem) stack.getItem();
        if (EtherItem.DEFAULT_FIRST_COLOR.equals(stack.get(DataComponents.DYED_COLOR))) {
            stack.remove(DataComponents.DYED_COLOR);
        }
        var secondaryColor = MalumDataComponents.SECONDARY_DYED_COLOR.get();
        if (!etherItem.isIridescent
                || EtherItem.DEFAULT_SECOND_COLOR.equals(stack.get(secondaryColor))) {
            // 非 iridescent では副色を参照しないため、同じ見た目のスタックが分裂しないよう除去する。
            stack.remove(secondaryColor);
        }
    }

    static boolean isSameEtherIgnoringColor(ItemStack first, ItemStack second) {
        if (!isSupportedEther(first)
                || !isSupportedEther(second)
                || first.getItem() != second.getItem()) {
            return false;
        }

        var normalizedFirst = first.copyWithCount(1);
        var normalizedSecond = second.copyWithCount(1);
        removeColors(normalizedFirst);
        removeColors(normalizedSecond);
        return ItemStack.isSameItemSameComponents(normalizedFirst, normalizedSecond);
    }

    private static void removeColors(ItemStack stack) {
        stack.remove(DataComponents.DYED_COLOR);
        stack.remove(MalumDataComponents.SECONDARY_DYED_COLOR.get());
    }
}
