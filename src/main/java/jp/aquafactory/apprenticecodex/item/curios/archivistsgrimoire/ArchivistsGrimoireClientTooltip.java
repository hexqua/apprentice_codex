package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
final class ArchivistsGrimoireClientTooltip {
    private ArchivistsGrimoireClientTooltip() {
    }

    static void append(
            ItemStack itemStack,
            List<Component> lines,
            List<ArchivistsGrimoire.VisibleSpell> visibleSpells) {
        var player = MinecraftInstanceHelper.getPlayer();
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }

        lines.add(Component.empty());
        lines.add(Component.translatable(
                        "tooltip.irons_spellbooks.press_to_cast",
                        Component.keybind("key.irons_spellbooks.spellbook_cast"))
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.irons_spellbooks.spellbook_tooltip").withStyle(ChatFormatting.GRAY));
        appendVisibleSpellTooltips(itemStack, lines, visibleSpells, localPlayer);
    }

    private static void appendVisibleSpellTooltips(
            ItemStack itemStack,
            List<Component> lines,
            List<ArchivistsGrimoire.VisibleSpell> visibleSpells,
            LocalPlayer player) {
        SpellSelectionManager spellSelectionManager = ClientMagicData.getSpellSelectionManager();
        var selectedOption = spellSelectionManager == null
                ? null
                : spellSelectionManager.getSpellSlot(spellSelectionManager.getSelectionIndex());

        for (var visibleSpell : visibleSpells) {
            var spellText = TooltipsUtils.getTitleComponent(visibleSpell.spellData(), player).setStyle(Style.EMPTY);
            if (Utils.getPlayerSpellbookStack(player) == itemStack
                    && selectedOption != null
                    && selectedOption.slot.equals(Curios.SPELLBOOK_SLOT)
                    && selectedOption.slotIndex == visibleSpell.visibleSlot()) {
                var shiftMessage = TooltipsUtils.formatActiveSpellTooltip(
                        itemStack,
                        spellSelectionManager.getSelectedSpellData(),
                        CastSource.SPELLBOOK,
                        player);
                shiftMessage.remove(0);
                TooltipsUtils.addShiftTooltip(
                        lines,
                        Component.literal("> ").append(spellText).withStyle(ChatFormatting.YELLOW),
                        shiftMessage.stream().map(component -> Component.literal(" ").append(component)).collect(Collectors.toList())
                );
            } else {
                lines.add(Component.literal(" ").append(spellText.withStyle(Style.EMPTY.withColor(0x8888fe))));
            }
        }
    }
}
