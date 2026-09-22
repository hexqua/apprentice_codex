package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class StoredScrollClientTooltip {
    private StoredScrollClientTooltip() {}

    public static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        // 除去待ちの投影に対するIron'sの表示とは重ねない。
        if (player == null || ISpellContainer.isSpellContainer(stack)) return;
        var tooltip = stack.getItem() instanceof ScrollcasterGauntlet
                ? ScrollcasterGauntlet.getScrollTooltipData(stack) : ChargecastCatalystbook.getScrollTooltipData(stack);
        lines.add(Component.empty());
        if (Screen.hasControlDown() && !tooltip.entries().isEmpty()) {
            ScrollSlotTooltipClientHelper.appendList(lines, tooltip, player);
            return;
        }
        if (tooltip.selectedSpell() == SpellData.EMPTY) {
            lines.add(Component.translatable("item.apprenticecodex.common.scroll_slots.no_selected_spell")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            var details = TooltipsUtils.formatActiveSpellTooltip(stack, tooltip.selectedSpell(), CastSource.SWORD, player);
            if (!details.isEmpty()) details.removeFirst();
            lines.addAll(details);
        }
        ScrollSlotTooltipClientHelper.appendHint(lines, tooltip.entries().size());
    }
}
