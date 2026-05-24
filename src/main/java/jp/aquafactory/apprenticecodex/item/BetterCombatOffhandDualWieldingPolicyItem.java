package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;

public interface BetterCombatOffhandDualWieldingPolicyItem {
    default boolean suppressesBetterCombatOffhandDualWielding(
            ItemStack offhandStack,
            ItemStack mainHandStack
    ) {
        return !allowsBetterCombatDualWieldingWithMainHand(offhandStack, mainHandStack);
    }

    default boolean allowsBetterCombatDualWieldingWithMainHand(
            ItemStack offhandStack,
            ItemStack mainHandStack
    ) {
        return mainHandStack.getItem() instanceof BetterCombatOffhandDualWieldingPolicyItem mainHandPolicyItem
                && betterCombatOffhandDualWieldingPolicyGroup(offhandStack)
                == mainHandPolicyItem.betterCombatOffhandDualWieldingPolicyGroup(mainHandStack);
    }

    default Class<?> betterCombatOffhandDualWieldingPolicyGroup(ItemStack stack) {
        return getClass();
    }
}
