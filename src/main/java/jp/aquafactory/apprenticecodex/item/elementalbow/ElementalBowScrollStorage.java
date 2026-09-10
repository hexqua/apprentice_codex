package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * スクロールの現物を保持し、弓に派生 SpellContainer を作らない。
 */
public final class ElementalBowScrollStorage {
    private static final String ROOT = "ElementalBowCalibration";
    public static final int ADJUSTMENT_SLOTS = 3;
    public static final int SCROLL_SLOTS = 4;

    private ElementalBowScrollStorage() {
    }

    public static void migrate(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.getCompound(ROOT).getInt("Version") >= 1) return;
        // 旧コンテナは自動生成された固定魔法。現物や無関係な component は保持する。
        ISpellContainer.remove(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var calibration = root.getCompound(ROOT);
            calibration.putInt("Version", 1);
            root.put(ROOT, calibration);
            if (root.getString("ElementalBowShotMode").equals("magic")) root.remove("ElementalBowShotMode");
            root.remove("ElementalBowMode");
        });
    }

    public static int enabledSlots(ItemStack stack, HolderLookup.Provider lookup) {
        int count = 1;
        for (int i = 0; i < ADJUSTMENT_SLOTS; i++) {
            if (CalibrationAdjustmentStorage.get(stack, i, ADJUSTMENT_SLOTS, lookup)
                    .is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES)) count++;
        }
        return count;
    }

    public static ItemStack get(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        if (slot < 0 || slot >= SCROLL_SLOTS) return ItemStack.EMPTY;
        var list = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompound(ROOT).getList("Scrolls", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            if (entry.getInt("Slot") == slot) return ItemStack.parseOptional(lookup, entry.getCompound("Item"));
        }
        return ItemStack.EMPTY;
    }

    public static void set(ItemStack stack, int slot, ItemStack scroll, HolderLookup.Provider lookup) {
        if (slot < 0 || slot >= SCROLL_SLOTS) return;
        migrate(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var calibration = root.getCompound(ROOT);
            var list = calibration.getList("Scrolls", Tag.TAG_COMPOUND);
            for (int i = list.size() - 1; i >= 0; i--) if (list.getCompound(i).getInt("Slot") == slot) list.remove(i);
            if (!scroll.isEmpty()) {
                var entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Item", scroll.copyWithCount(1).saveOptional(lookup));
                list.add(entry);
            }
            calibration.put("Scrolls", list);
            root.put(ROOT, calibration);
        });
    }

    public static SpellData readSpell(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        var scroll = get(stack, slot, lookup);
        if (!(scroll.getItem() instanceof Scroll)) return SpellData.EMPTY;
        var container = ISpellContainer.get(scroll);
        return container == null ? SpellData.EMPTY : container.getSpellAtIndex(0);
    }
}
