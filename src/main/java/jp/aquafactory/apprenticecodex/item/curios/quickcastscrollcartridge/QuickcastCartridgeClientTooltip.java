package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.spells.CastSource;
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
final class QuickcastCartridgeClientTooltip {
    private QuickcastCartridgeClientTooltip() {}

    static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var tooltip = QuickcastScrollCartridge.getScrollTooltipData(stack);
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
            // 通常詠唱と同じマナ・CD条件を維持し、一覧だけ共通の折り畳み表示へ移す。
            var details = TooltipsUtils.formatActiveSpellTooltip(stack, selected, CastSource.SPELLBOOK, player);
            if (!details.isEmpty()) details.removeFirst();
            lines.addAll(details);
        }
        ScrollSlotTooltipClientHelper.appendHint(lines, tooltip.entries().size());
    }
}
