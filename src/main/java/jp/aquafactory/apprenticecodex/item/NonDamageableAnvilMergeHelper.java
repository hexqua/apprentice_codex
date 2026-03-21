package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.Holder;
import net.minecraft.Util;
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
        if (!mergeItem.supportsSameItemAnvilMerge(leftStack, rightStack)) {
            return null;
        }

        // 左入力の独自 NBT を優先し、非耐久アイテム固有の状態を金床で落とさないようにする。
        var resultStack = leftStack.copy();
        var mergedEnchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(resultStack));
        var rightEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(rightStack);
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
        EnchantmentHelper.setEnchantments(resultStack, mergedEnchantments.toImmutable());
        return new MergeResult(resultStack, totalCost, 1);
    }

    private static int getRarityCost(Holder<Enchantment> enchantment) {
        return enchantment.value().getAnvilCost();
    }

    public record MergeResult(ItemStack output, int cost, int materialCost) {
    }
}
