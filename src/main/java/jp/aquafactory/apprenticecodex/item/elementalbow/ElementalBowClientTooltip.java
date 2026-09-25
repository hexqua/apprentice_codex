package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.item.ScrollSlotTooltipClientHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class ElementalBowClientTooltip {
    private ElementalBowClientTooltip() {
    }

    static HolderLookup.Provider lookup() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        // 引き絞り時間などの既存取得処理も、元のstackを正規化しないようコピーで表示する。
        stack = stack.copy();
        var tooltip = ElementalBow.getScrollTooltipData(stack, lookup());
        var selected = ElementalBow.getDisplayedSpellProfile(stack);
        lines.add(Component.empty());
        if (Screen.hasControlDown() && !tooltip.entries().isEmpty()) {
            ScrollSlotTooltipClientHelper.appendList(lines, tooltip, player);
            return;
        }
        if (selected != null) {
            var data = new SpellData(selected.spell(), selected.spellLevel());
            // 常設ルーンの系統とマナを一括反映し、一時的な過熱ペナルティは表示へ反映しない。
            List<MutableComponent> details;
            try (var ignored = ElementalBowSpellPowerContext.open(player, selected.spell(), stack)) {
                details = TooltipsUtils.formatActiveSpellTooltip(stack, data, CastSource.SPELLBOOK, player);
                var mana = TooltipsUtils.getManaCostComponent(selected.spell().getCastType(),
                        ElementalBowRunes.baseManaCost(stack, player,
                                selected.spell().getManaCost(selected.spell().getLevelFor(selected.spellLevel(), player))))
                        .withStyle(ChatFormatting.BLUE);
                details.replaceAll(line -> line.getContents() instanceof TranslatableContents text
                        && (text.getKey().equals("tooltip.irons_spellbooks.mana_cost")
                        || text.getKey().equals("tooltip.irons_spellbooks.mana_cost_per_second")) ? mana : line);
            }
            if (!details.isEmpty()) {
                details.remove(0);
            }
            var drawTime = Component.literal(" ").append(Component.translatable(
                    "item.apprenticecodex.elemental_bow.tooltip.draw",
                    Utils.timeFromTicks(ElementalBow.resolveMagicRequiredDrawTicks(stack), 2))
                    .withStyle(ChatFormatting.BLUE));
            // Iron's の詠唱時間行は空白の親 component と翻訳された子 component で構成される。
            details.replaceAll(line -> line.getSiblings().stream().anyMatch(child ->
                    child.getContents() instanceof TranslatableContents text
                            && (text.getKey().equals("tooltip.irons_spellbooks.cast_long")
                            || text.getKey().equals("tooltip.irons_spellbooks.cast_continuous")))
                    ? drawTime : line);
            if (selected.spell().getCastType() == CastType.INSTANT) {
                details.add(drawTime);
            }
            lines.addAll(details);
        } else {
            lines.add(Component.translatable("item.apprenticecodex.common.scroll_slots.no_selected_spell")
                    .withStyle(ChatFormatting.GRAY));
        }
        ScrollSlotTooltipClientHelper.appendHint(lines, tooltip.entries().size());
    }
}
