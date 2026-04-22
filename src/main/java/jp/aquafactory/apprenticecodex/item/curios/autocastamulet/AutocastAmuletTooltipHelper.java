package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AutocastAmuletTooltipHelper {
    private AutocastAmuletTooltipHelper() {
    }

    public static void appendImbuedSpellTooltip(ItemStack stack, LocalPlayer player, List<Component> lines, boolean advanced) {
        var imbuedSpells = AutocastAmulet.getImbuedSpells(stack);
        if (imbuedSpells.isEmpty()) {
            return;
        }

        var tooltipInjectIndex = advanced ? TooltipsUtils.indexOfAdvancedText(lines, stack) : lines.size();
        var additionalLines = new ArrayList<Component>();
        additionalLines.add(Component.empty());
        additionalLines.add(Component.translatable(
                imbuedSpells.size() > 1 ? "tooltip.irons_spellbooks.imbued_tooltip_plural" : "tooltip.irons_spellbooks.imbued_tooltip"
        ).withStyle(ChatFormatting.GRAY));

        if (ClientInputEvents.isShowExpandedTooltip()) {
            // AutocastAmulet だけは通常時の展開を抑え、Shift 時だけ Iron's 既存詳細をそのまま見せる。
            imbuedSpells.forEach(spellData -> {
                var spellTooltip = new ArrayList<Component>(TooltipsUtils.formatActiveSpellTooltip(stack, spellData, CastSource.SWORD, player));
                spellTooltip.set(1, Component.literal(" ").append(spellTooltip.get(1)));
                additionalLines.addAll(spellTooltip);
            });
        } else {
            imbuedSpells.forEach(spellData -> {
                var spellText = TooltipsUtils.getTitleComponent(spellData, player).setStyle(Style.EMPTY);
                additionalLines.add(Component.literal(" ").append(spellText.withStyle(Style.EMPTY.withColor(0x8888fe))));
            });
            additionalLines.add(Component.literal(" ").append(
                    Component.translatable("item.apprenticecodex.autocast_amulet.tooltip.hint").withStyle(ChatFormatting.DARK_GRAY)
            ));
        }

        lines.addAll(tooltipInjectIndex < 0 ? lines.size() : tooltipInjectIndex, additionalLines);
    }
}
