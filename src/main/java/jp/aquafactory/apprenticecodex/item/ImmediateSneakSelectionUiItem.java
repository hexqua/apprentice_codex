package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * ホイール操作のたびに選択をItemStackへ即時反映する、共通スニーク選択UIの契約。
 */
public interface ImmediateSneakSelectionUiItem extends SneakSelectionUiItem {
    List<SneakSelectionView> getSneakSelectionViews(ItemStack stack);

    int getSneakSelectionIndex(ItemStack stack);

    boolean isSneakSelectionIndexSelectable(ItemStack stack, int selectionIndex);

    void setSneakSelectionIndex(ItemStack stack, int selectionIndex);

    default HandStackResolver.OffhandResolution getSneakSelectionOffhandResolution() {
        return HandStackResolver.OffhandResolution.LOGICAL;
    }

    default ItemStack resolveSneakSelectionStack(Player player, InteractionHand hand) {
        return HandStackResolver.resolve(player, hand, getSneakSelectionOffhandResolution());
    }
}
