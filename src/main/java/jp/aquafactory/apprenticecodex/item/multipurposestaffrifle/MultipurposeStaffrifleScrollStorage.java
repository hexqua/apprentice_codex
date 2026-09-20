package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * スクロール現物を保持し、無効になった枠も取り出せるよう保存枠数を固定する。
 */
public final class MultipurposeStaffrifleScrollStorage {
    private static final String ROOT = "MultipurposeStaffrifleCalibration";
    public static final int MAX_SCROLL_SLOTS = 10;

    private MultipurposeStaffrifleScrollStorage() {
    }

    public static ItemStack get(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof MultipurposeStaffrifle) || slot < 0 || slot >= MAX_SCROLL_SLOTS) {
            return ItemStack.EMPTY;
        }
        var list = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompound(ROOT).getList("Scrolls", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            if (entry.getInt("Slot") == slot) return ItemStack.parseOptional(lookup, entry.getCompound("Item"));
        }
        return ItemStack.EMPTY;
    }

    public static void set(ItemStack stack, int slot, ItemStack scroll, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof MultipurposeStaffrifle rifle)
                || slot < 0 || slot >= MAX_SCROLL_SLOTS) return;
        if (!scroll.isEmpty() && !rifle.evaluateCalibrationImbue(stack, slot,
                readSpell(scroll), lookup).canInsert()) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var data = root.getCompound(ROOT);
            var list = data.getList("Scrolls", Tag.TAG_COMPOUND);
            for (var i = list.size() - 1; i >= 0; i--) {
                if (list.getCompound(i).getInt("Slot") == slot) list.remove(i);
            }
            if (!scroll.isEmpty()) {
                var entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Item", scroll.copyWithCount(1).saveOptional(lookup));
                list.add(entry);
            }
            data.put("Scrolls", list);
            root.put(ROOT, data);
        });
        rifle.normalizeSelectedScrollIndex(stack, lookup);
    }

    public static SpellData spell(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        return readSpell(get(stack, slot, lookup));
    }

    private static SpellData readSpell(ItemStack scroll) {
        if (!(scroll.getItem() instanceof Scroll)) return SpellData.EMPTY;
        var container = ISpellContainer.get(scroll);
        return container == null ? SpellData.EMPTY : container.getSpellAtIndex(0);
    }

    public static int selected(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT);
        return data.contains("Selected", Tag.TAG_INT) ? data.getInt("Selected") : -1;
    }

    public static void select(ItemStack stack, int index) {
        if (selected(stack) == index) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var data = root.getCompound(ROOT);
            data.putInt("Selected", index);
            root.put(ROOT, data);
        });
    }
}
