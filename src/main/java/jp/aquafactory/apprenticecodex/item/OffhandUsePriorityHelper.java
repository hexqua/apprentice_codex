package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.Tags;

public final class OffhandUsePriorityHelper {
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

        // 継承元ではなく NeoForge の盾契約で判定し、ShieldItem 非継承の MOD 盾にも追従する。
        return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK)
                || stack.is(Tags.Items.TOOLS_SHIELD);
    }
}
