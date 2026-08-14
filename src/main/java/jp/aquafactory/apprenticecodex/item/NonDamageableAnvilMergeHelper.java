package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

public final class NonDamageableAnvilMergeHelper {
    private NonDamageableAnvilMergeHelper() {
    }

    public static @Nullable MergeResult tryMergeSameItem(
            ItemStack leftStack,
            ItemStack rightStack,
            @Nullable String itemName,
            Player player
    ) {
        if (leftStack.isEmpty() || rightStack.isEmpty()) {
            return null;
        }
        if (leftStack.getItem() != rightStack.getItem()) {
            return null;
        }
        if (leftStack.isDamageableItem()) {
            return null;
        }
        if (!(leftStack.getItem() instanceof NonDamageableAnvilMergeItem mergeItem)) {
            return null;
        }
        // 右アイテムはスロット内アイテムを全て失うためガードをかける.
        if (hasStoredContents(rightStack)) {
            return null;
        }
        if (!mergeItem.supportsSameItemAnvilMerge(leftStack, rightStack)) {
            return null;
        }

        // 左入力の独自 NBT を優先し、非耐久アイテム固有の状態を金床で落とさないようにする。
        var resultStack = leftStack.copy();
        var mergedEnchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(resultStack));
        var rightEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(rightStack);
        var baseCost = leftStack.getOrDefault(DataComponents.REPAIR_COST, 0)
                + rightStack.getOrDefault(DataComponents.REPAIR_COST, 0);
        var addedCost = 0;
        var renameCost = 0;
        var appliedAnyEnchantment = false;
        var rejectedAnyEnchantment = false;

        for (var entry : rightEnchantments.entrySet()) {
            var enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            var currentLevel = mergedEnchantments.getLevel(enchantment);
            var targetLevel = entry.getValue();
            targetLevel = currentLevel == targetLevel ? targetLevel + 1 : Math.max(targetLevel, currentLevel);

            var canApply = mergeItem.isAnvilMergeEnchantmentAllowed(leftStack, enchantment);
            for (Holder<Enchantment> existingEnchantment : mergedEnchantments.keySet()) {
                if (!existingEnchantment.equals(enchantment) && !Enchantment.areCompatible(enchantment, existingEnchantment)) {
                    canApply = false;
                    ++addedCost;
                }
            }

            if (!canApply) {
                rejectedAnyEnchantment = true;
                continue;
            }

            appliedAnyEnchantment = true;
            if (targetLevel > enchantment.value().getMaxLevel()) {
                targetLevel = enchantment.value().getMaxLevel();
            }

            mergedEnchantments.set(enchantment, targetLevel);
            addedCost += getRarityCost(enchantment) * targetLevel;
        }

        if (rejectedAnyEnchantment && !appliedAnyEnchantment) {
            return null;
        }

        if (itemName != null && !itemName.isBlank()) {
            if (!itemName.equals(leftStack.getHoverName().getString())) {
                renameCost = 1;
                addedCost += renameCost;
                resultStack.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
            }
        } else if (leftStack.get(DataComponents.CUSTOM_NAME) != null) {
            renameCost = 1;
            addedCost += renameCost;
            resultStack.remove(DataComponents.CUSTOM_NAME);
        }

        if (addedCost <= 0) {
            return null;
        }

        var totalCost = baseCost + addedCost;
        if (renameCost == addedCost && renameCost > 0 && totalCost >= 40) {
            totalCost = 39;
        }
        if (totalCost >= 40 && !player.getAbilities().instabuild) {
            return null;
        }

        var updatedRepairCost = resultStack.getOrDefault(DataComponents.REPAIR_COST, 0);
        var rightRepairCost = rightStack.getOrDefault(DataComponents.REPAIR_COST, 0);
        if (updatedRepairCost < rightRepairCost) {
            updatedRepairCost = rightRepairCost;
        }
        if (renameCost != addedCost || renameCost == 0) {
            updatedRepairCost = AnvilMenu.calculateIncreasedRepairCost(updatedRepairCost);
        }

        resultStack.set(DataComponents.REPAIR_COST, updatedRepairCost);
        EnchantmentHelper.setEnchantments(resultStack, mergedEnchantments.toImmutable());
        return new MergeResult(resultStack, totalCost, 1);
    }

    private static boolean hasStoredContents(ItemStack stack) {
        if (stack.getItem() instanceof SpellCalibrationAdjustmentTarget adjustmentTarget) {
            for (var slot = 0; slot < adjustmentTarget.getCalibrationAdjustmentSlotCount(stack); ++slot) {
                if (!adjustmentTarget.getCalibrationAdjustment(stack, slot).isEmpty()) {
                    return true;
                }
            }
        }

        return stack.getItem() instanceof StoredSpellCalibrationImbueTarget storedTarget
                && storedTarget.hasAnyStoredCalibrationScroll(stack);
    }

    private static int getRarityCost(Holder<Enchantment> enchantment) {
        return enchantment.value().getAnvilCost();
    }

    public record MergeResult(ItemStack output, int cost, int materialCost) {
    }
}
