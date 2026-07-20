package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolActions;

public final class OffhandUsePriorityHelper {
    private static final ResourceLocation FORGE_SHIELDS_TAG_ID = ResourceLocation.fromNamespaceAndPath("forge", "shields");
    private static final ResourceLocation FORGE_TOOLS_SHIELDS_TAG_ID =
            ResourceLocation.fromNamespaceAndPath("forge", "tools/shields");

    private OffhandUsePriorityHelper() {
    }

    public static boolean isPriorityOffhandUseItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        // 使用可能状態ではなく装備種別で優先する。
        // 弾切れ・盾無効化・クールダウンでメインハンド詠唱へ戻すと、戦闘中に右クリックの意味が突然変わるため。
        var item = stack.getItem();
        return item instanceof AbstractSpellGunItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof ChargecastCatalystbook
                || isShieldLikeItem(stack);
    }

    public static boolean isShieldLikeItem(ItemStack stack) {
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
