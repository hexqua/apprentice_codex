package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class QuickcastCartridgeClientTooltip {
    private static final String PREFIX = "item.apprenticecodex.quickcast_scroll_cartridge.tooltip.";

    private QuickcastCartridgeClientTooltip() {}

    static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var selected = QuickcastScrollCartridge.getSelectedSpellData(stack);
        lines.add(Component.empty());
        if (selected == SpellData.EMPTY) {
            lines.add(Component.translatable(PREFIX + "empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.add(Component.translatable(PREFIX + "selected").withStyle(ChatFormatting.GRAY));
        // スペルコンテナ化せず、保存された選択魔法を通常詠唱と同じマナ・CD条件で表示する。
        var details = TooltipsUtils.formatActiveSpellTooltip(stack, selected, CastSource.SPELLBOOK, player);
        if (!details.isEmpty()) {
            details.remove(0);
        }
        lines.addAll(details);

        int selectedIndex = QuickcastScrollCartridge.getSelectedScrollIndex(stack);
        boolean hasOtherSpells = false;
        for (int slot = 0; slot < QuickcastScrollCartridge.getEnabledCalibrationScrollSlotCount(stack); slot++) {
            if (slot == selectedIndex) {
                continue;
            }
            var data = QuickcastScrollCartridge.readSpell(stack, slot);
            if (data == SpellData.EMPTY) {
                continue;
            }
            if (!hasOtherSpells) {
                lines.add(Component.empty());
                lines.add(Component.translatable(PREFIX + "others").withStyle(ChatFormatting.GRAY));
                hasOtherSpells = true;
            }
            lines.add(Component.literal("  ").append(TooltipsUtils.getTitleComponent(data, player)));
        }
    }
}
