package jp.aquafactory.apprenticecodex.item.curios.autocastamulet;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            appendSpellStatusSection(player, additionalLines, imbuedSpells);
        } else {
            imbuedSpells.forEach(spellData -> {
                var spellText = TooltipsUtils.getTitleComponent(spellData, player).setStyle(Style.EMPTY);
                additionalLines.add(Component.literal(" ").append(spellText.withStyle(Style.EMPTY.withColor(0x8888fe))));
            });
            appendSpellStatusSection(player, additionalLines, imbuedSpells);
            additionalLines.add(Component.literal(" ").append(
                    Component.translatable("item.apprenticecodex.autocast_amulet.tooltip.hint").withStyle(ChatFormatting.DARK_GRAY)
            ));
        }

        lines.addAll(tooltipInjectIndex < 0 ? lines.size() : tooltipInjectIndex, additionalLines);
    }

    private static void appendSpellStatusSection(LocalPlayer player, List<Component> lines, List<SpellData> imbuedSpells) {
        if (imbuedSpells.isEmpty()) {
            return;
        }

        var activeSpellCount = imbuedSpells.size();
        lines.add(Component.empty());
        lines.add(Component.translatable("item.apprenticecodex.autocast_amulet.tooltip.status_title").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "item.apprenticecodex.autocast_amulet.tooltip.mana_multiplier",
                formatManaMultiplier(activeSpellCount)
        ).withStyle(ChatFormatting.DARK_GRAY));

        // tooltip はクライアントが保持している cooldown 観測値をそのまま表示し、HUD 通知と食い違わないようにする。
        var spellCooldowns = ClientMagicData.getCooldowns().getSpellCooldowns();
        imbuedSpells.forEach(spellData -> {
            var spell = spellData.getSpell();
            if (spell == null) {
                return;
            }

            var cooldown = spellCooldowns.get(spell.getSpellId());
            if (cooldown != null && cooldown.getCooldownRemaining() > 0.0F) {
                lines.add(Component.translatable(
                        "item.apprenticecodex.autocast_amulet.tooltip.cooldown_line",
                        spell.getDisplayName(player),
                        AutocastAmuletNotificationController.toDisplayCooldownSeconds(cooldown.getCooldownRemaining())
                ).withStyle(ChatFormatting.DARK_AQUA));
                return;
            }

            lines.add(Component.translatable(
                    "item.apprenticecodex.autocast_amulet.tooltip.ready_line",
                    spell.getDisplayName(player),
                    AutocastAmulet.getScaledManaCost(spell, spellData.getLevel(), activeSpellCount)
            ).withStyle(ChatFormatting.GREEN));
        });
    }

    private static String formatManaMultiplier(int activeSpellCount) {
        return String.format(Locale.ROOT, "%.1fx", AutocastAmulet.getManaMultiplier(activeSpellCount));
    }
}
