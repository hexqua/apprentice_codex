package jp.aquafactory.apprenticecodex.item;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

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
        var mergedEnchantments = new HashMap<>(EnchantmentHelper.getEnchantments(resultStack));
        var rightEnchantments = EnchantmentHelper.getEnchantments(rightStack);
        var baseCost = leftStack.getBaseRepairCost() + rightStack.getBaseRepairCost();
        var addedCost = 0;
        var renameCost = 0;
        var appliedAnyEnchantment = false;
        var rejectedAnyEnchantment = false;

        for (var entry : rightEnchantments.entrySet()) {
            var enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            var currentLevel = mergedEnchantments.getOrDefault(enchantment, 0);
            var targetLevel = entry.getValue();
            targetLevel = currentLevel == targetLevel ? targetLevel + 1 : Math.max(targetLevel, currentLevel);

            var canApply = mergeItem.isAnvilMergeEnchantmentAllowed(leftStack, enchantment);
            for (Enchantment existingEnchantment : mergedEnchantments.keySet()) {
                if (existingEnchantment != enchantment && !enchantment.isCompatibleWith(existingEnchantment)) {
                    canApply = false;
                    ++addedCost;
                }
            }

            if (!canApply) {
                rejectedAnyEnchantment = true;
                continue;
            }

            appliedAnyEnchantment = true;
            if (targetLevel > enchantment.getMaxLevel()) {
                targetLevel = enchantment.getMaxLevel();
            }

            mergedEnchantments.put(enchantment, targetLevel);
            addedCost += getRarityCost(enchantment) * targetLevel;
        }

        if (rejectedAnyEnchantment && !appliedAnyEnchantment) {
            return null;
        }

        if (itemName != null && !Util.isBlank(itemName)) {
            if (!itemName.equals(leftStack.getHoverName().getString())) {
                renameCost = 1;
                addedCost += renameCost;
                resultStack.setHoverName(Component.literal(itemName));
            }
        } else if (leftStack.hasCustomHoverName()) {
            renameCost = 1;
            addedCost += renameCost;
            resultStack.resetHoverName();
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

        var updatedRepairCost = resultStack.getBaseRepairCost();
        if (updatedRepairCost < rightStack.getBaseRepairCost()) {
            updatedRepairCost = rightStack.getBaseRepairCost();
        }
        if (renameCost != addedCost || renameCost == 0) {
            updatedRepairCost = AnvilMenu.calculateIncreasedRepairCost(updatedRepairCost);
        }

        resultStack.setRepairCost(updatedRepairCost);
        EnchantmentHelper.setEnchantments(mergedEnchantments, resultStack);
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

    private static int getRarityCost(Enchantment enchantment) {
        return switch (enchantment.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    public record MergeResult(ItemStack output, int cost, int materialCost) {
    }
}
