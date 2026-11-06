package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class InventoryTestSupport {
    private InventoryTestSupport() {}

    public static boolean containsItem(ChestBlockEntity chest, Item item, int count) {
        for (var slot = 0; slot < chest.getContainerSize(); ++slot) {
            var stack = chest.getItem(slot);
            if (stack.is(item) && stack.getCount() == count) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    public static CraftingInput createCraftingInput(ItemStack... stacks) {
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; ++i) {
            items.set(i, stacks[i]);
        }
        return CraftingInput.of(3, 3, items);
    }
}
