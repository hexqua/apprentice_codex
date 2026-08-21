package jp.aquafactory.apprenticecodex.gametest.malum;

import com.sammy.malum.common.item.curiosities.pouch.MalumPouchItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MalumPouchGameTestHelper {
    private MalumPouchGameTestHelper() {
    }

    public static ItemStack createPouch(String pouchId, List<ItemStack> storedItems) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", pouchId));
        if (!(item instanceof MalumPouchItem pouchItem)) {
            throw new IllegalStateException("Missing Malum pouch item: " + pouchId);
        }

        var pouchStack = new ItemStack(item);
        var mutable = pouchItem.getContents(pouchStack).mutable();
        mutable.clearItems();
        for (var index = storedItems.size() - 1; index >= 0; --index) {
            var stack = storedItems.get(index).copy();
            var expectedCount = stack.getCount();
            if (mutable.tryInsert(stack) != expectedCount || !stack.isEmpty()) {
                throw new IllegalStateException("Could not fill Malum pouch for GameTest: " + pouchId);
            }
        }
        pouchItem.setContents(pouchStack, mutable.immutable());
        return pouchStack;
    }

    public static List<ItemStack> getItemsCopy(ItemStack pouchStack) {
        if (!(pouchStack.getItem() instanceof MalumPouchItem pouchItem)) {
            return List.of();
        }
        return new ArrayList<>(pouchItem.getContents(pouchStack).getItemsCopy());
    }

    public static int getStoredCount(ItemStack pouchStack, ItemStack template) {
        var total = 0;
        for (var stack : getItemsCopy(pouchStack)) {
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static double getWeight(ItemStack pouchStack) {
        if (!(pouchStack.getItem() instanceof MalumPouchItem pouchItem)) {
            return 0.0D;
        }
        return pouchItem.getContents(pouchStack).weight().doubleValue();
    }
}
