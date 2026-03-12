package jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record SpellcasterAmmoPouchTooltip(
        NonNullList<ItemStack> items,
        int highlightedIndex,
        boolean full
) implements TooltipComponent {
}
