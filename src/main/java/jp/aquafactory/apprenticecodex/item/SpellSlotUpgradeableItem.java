package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import net.minecraft.world.item.ItemStack;

public interface SpellSlotUpgradeableItem {
    ItemStack createSpellSlotUpgradeResult(ItemStack baseStack, SpellSlotUpgradeItem upgradeItem);
}
