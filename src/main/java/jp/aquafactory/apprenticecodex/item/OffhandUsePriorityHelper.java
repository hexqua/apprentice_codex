package jp.aquafactory.apprenticecodex.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;

final class OffhandUsePriorityHelper {
    private static final ResourceLocation FORGE_SHIELDS_TAG_ID = ResourceLocation.fromNamespaceAndPath("forge", "shields");
    private static final ResourceLocation FORGE_TOOLS_SHIELDS_TAG_ID =
            ResourceLocation.fromNamespaceAndPath("forge", "tools/shields");

    private OffhandUsePriorityHelper() {
    }

    static boolean isPriorityOffhandUseItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        var item = stack.getItem();
        return item instanceof AbstractSpellGunItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || isShieldLikeItem(stack);
    }

    static boolean isShieldLikeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 継承元ではなく Forge の盾契約で判定し、ShieldItem 非継承の MOD 盾や
        // Shield Expansion のタグ拡張にも追従する。
        return stack.canPerformAction(ToolActions.SHIELD_BLOCK)
                || stack.is(ItemTags.create(FORGE_SHIELDS_TAG_ID))
                || stack.is(ItemTags.create(FORGE_TOOLS_SHIELDS_TAG_ID));
    }
}
