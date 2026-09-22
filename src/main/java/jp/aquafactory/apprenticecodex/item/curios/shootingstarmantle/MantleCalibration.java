package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceHelper;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentHint;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentRule;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.ScrollSlotTooltipData;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueTarget;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Objects;

import static jp.aquafactory.apprenticecodex.registry.ItemRegistry.SCROLLWOVEN_PARCHMENT;

/** 外套の調整とスクロールは、消費で更新される魔力から独立して保存する。 */
public final class MantleCalibration {
    public static final int ADJUSTMENT_SLOTS = 3;
    public static final int SCROLL_SLOTS = 3;
    private static final String ROOT = "ShootingStarMantleCalibration";
    private static final HolderLookup.Provider FALLBACK_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    public static final CalibrationAdjustmentProfile PROFILE = CalibrationAdjustmentProfile.of(
            CalibrationAdjustmentRule.repeatable("scroll_slot", stack -> stack.is(SCROLLWOVEN_PARCHMENT.get()),
                    CalibrationAdjustmentHint.specificItem(SCROLLWOVEN_PARCHMENT))
                    .withEffectLines(CalibrationAdjustmentEffects.addScrollSlot(1)),
            CalibrationAdjustmentRule.unique("protection_rune", stack -> stack.is(ItemRegistry.PROTECTION_RUNE.get()),
                    CalibrationAdjustmentHint.specificItem(ItemRegistry.PROTECTION_RUNE))
                    .withEffectLines(CalibrationAdjustmentEffects.addSpellResist(0.1)),
            CalibrationAdjustmentRule.unique("recovery_rune", stack -> stack.is(ItemRegistry.COOLDOWN_RUNE.get()),
                    CalibrationAdjustmentHint.specificItem(ItemRegistry.COOLDOWN_RUNE))
                    .withEffectLines(CalibrationAdjustmentEffects.forceMantleRecovery()),
            CalibrationAdjustmentRule.unique("ice_rune", stack -> stack.is(ItemRegistry.ICE_RUNE.get()),
                    CalibrationAdjustmentHint.specificItem(ItemRegistry.ICE_RUNE))
                    .withEffectLines(CalibrationAdjustmentEffects.changeMantleDrift()));

    private MantleCalibration() { }

    static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_LOOKUP : server.registryAccess();
    }

    public static int enabledSlots(ItemStack stack, HolderLookup.Provider lookup) {
        int count = 0;
        for (int slot = 0; slot < ADJUSTMENT_SLOTS; slot++) {
            if (CalibrationAdjustmentStorage.get(stack, slot, ADJUSTMENT_SLOTS, lookup)
                    .is(SCROLLWOVEN_PARCHMENT.get())) count++;
        }
        return count;
    }

    public static boolean hasAdjustment(ItemStack stack, Item item) {
        if (!(stack.getItem() instanceof ShootingStarMantle mantle)) return false;
        for (int slot = 0; slot < ADJUSTMENT_SLOTS; slot++) {
            if (mantle.getCalibrationAdjustment(stack, slot).is(item)) return true;
        }
        return false;
    }

    public static boolean fastRecovery(ItemStack stack) {
        return hasAdjustment(stack, ItemRegistry.COOLDOWN_RUNE.get());
    }

    public static boolean retainsDrift(ItemStack stack) {
        return hasAdjustment(stack, ItemRegistry.ICE_RUNE.get());
    }

    public static ItemStack getScroll(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof ShootingStarMantle) || slot < 0 || slot >= SCROLL_SLOTS) return ItemStack.EMPTY;
        var list = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompound(ROOT).getList("Scrolls", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            if (entry.getInt("Slot") == slot) return ItemStack.parseOptional(lookup, entry.getCompound("Item"));
        }
        return ItemStack.EMPTY;
    }

    public static void setScroll(ItemStack stack, int slot, ItemStack scroll, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof ShootingStarMantle) || slot < 0 || slot >= SCROLL_SLOTS) return;
        // 有効枠数とは独立させ、羊皮紙を外しても現物と元の位置を保持する。
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var calibration = root.getCompound(ROOT);
            var list = calibration.getList("Scrolls", Tag.TAG_COMPOUND);
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.getCompound(i).getInt("Slot") == slot) list.remove(i);
            }
            if (!scroll.isEmpty()) {
                var entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Item", scroll.copyWithCount(1).saveOptional(lookup));
                list.add(entry);
            }
            if (list.isEmpty()) {
                root.remove(ROOT);
            } else {
                calibration.put("Scrolls", list);
                root.put(ROOT, calibration);
            }
        });
    }

    public static SpellData readSpell(ItemStack stack, int slot, HolderLookup.Provider lookup) {
        var scroll = getScroll(stack, slot, lookup);
        if (!(scroll.getItem() instanceof Scroll)) return SpellData.EMPTY;
        var container = ISpellContainer.get(scroll);
        var data = container == null ? SpellData.EMPTY : container.getSpellAtIndex(0);
        return SpellCalibrationImbueTarget.isValidCalibrationSpell(data) ? data : SpellData.EMPTY;
    }

    public static ScrollSlotTooltipData tooltipData(ItemStack stack, HolderLookup.Provider lookup) {
        var entries = new ArrayList<ScrollSlotTooltipData.Entry>();
        int enabled = enabledSlots(stack, lookup);
        for (int slot = 0; slot < SCROLL_SLOTS; slot++) {
            var scroll = getScroll(stack, slot, lookup);
            if (scroll.isEmpty()) continue;
            var spell = readSpell(stack, slot, lookup);
            entries.add(new ScrollSlotTooltipData.Entry(slot, scroll,
                    TranscendenceHelper.resolveScrollSpellData(stack, spell), slot < enabled && spell != SpellData.EMPTY));
        }
        return new ScrollSlotTooltipData(SpellData.EMPTY, -1, entries);
    }

    public static boolean hasSameScrollSelection(ItemStack before, ItemStack after, HolderLookup.Provider lookup) {
        if (before.getItem() != after.getItem()) return false;
        if (!(after.getItem() instanceof ShootingStarMantle)) return true;
        if (enabledSlots(before, lookup) != enabledSlots(after, lookup)
                || !Objects.equals(before.get(DataComponents.ENCHANTMENTS), after.get(DataComponents.ENCHANTMENTS))) return false;
        for (int slot = 0; slot < SCROLL_SLOTS; slot++) {
            if (!ItemStack.isSameItemSameComponents(getScroll(before, slot, lookup), getScroll(after, slot, lookup))) return false;
        }
        return true;
    }
}
