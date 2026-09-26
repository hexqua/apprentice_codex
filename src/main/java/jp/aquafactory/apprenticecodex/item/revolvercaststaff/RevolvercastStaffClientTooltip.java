package jp.aquafactory.apprenticecodex.item.revolvercaststaff;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.item.ScrollSlotTooltipClientHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class RevolvercastStaffClientTooltip {
    private RevolvercastStaffClientTooltip() {}

    static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        // 削除待ちの旧投影は Iron's が表示するため、二重に追加しない。
        if (player == null || ISpellContainer.isSpellContainer(stack)) return;
        var tooltip = RevolvercastStaff.getScrollTooltipData(stack);
        lines.add(Component.empty());
        if (Screen.hasControlDown() && !tooltip.entries().isEmpty()) {
            ScrollSlotTooltipClientHelper.appendList(lines, tooltip, player);
            return;
        }
        var selected = tooltip.selectedSpell();
        if (selected == SpellData.EMPTY) {
            lines.add(Component.translatable("item.apprenticecodex.common.scroll_slots.no_selected_spell")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            var details = TooltipsUtils.formatActiveSpellTooltip(stack, selected, CastSource.SWORD, player);
            if (!details.isEmpty()) details.remove(0);
            lines.addAll(details);
        }
        ScrollSlotTooltipClientHelper.appendHint(lines, tooltip.entries().size());
    }
}
