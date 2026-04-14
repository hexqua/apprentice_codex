package jp.aquafactory.apprenticecodex.compat.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.ITooltip;
import snownee.jade.api.ui.IElementHelper;

final class JadeTooltipHelper {
    private JadeTooltipHelper() {
    }

    static int toDisplaySeconds(long ticks) {
        return Math.max(1, Mth.ceil(Math.max(0L, ticks) / 20.0D));
    }

    static void appendItemLine(ITooltip tooltip, ItemStack stack) {
        appendItemLine(tooltip, stack, stack.getHoverName().copy());
    }

    static void appendItemLine(ITooltip tooltip, ItemStack stack, Component label) {
        var icon = IElementHelper.get().item(stack.copyWithCount(1), 0.5f);
        icon.message(null);
        tooltip.add(icon);
        tooltip.append(label);
    }

    static void appendItemCountLine(ITooltip tooltip, ItemStack stack, int count) {
        appendItemLine(
                tooltip,
                stack,
                Component.translatable("jade.apprenticecodex.item_count", count, stack.getHoverName().copy())
        );
    }
}
