package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
        var selected = ElementalBow.getDisplayedSpellProfile(stack);
        lines.add(Component.empty());
        if (selected != null) {
            var data = new SpellData(selected.spell(), selected.spellLevel());
            // Cartridge と同様に通常の魔法情報を使い、一時的な過熱ペナルティは表示へ反映しない。
            var details = TooltipsUtils.formatActiveSpellTooltip(stack, data, CastSource.SPELLBOOK, player);
            if (!details.isEmpty()) {
                details.removeFirst();
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
            lines.add(Component.translatable("item.apprenticecodex.elemental_bow.tooltip.no_selected_spell")
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("item.apprenticecodex.elemental_bow.tooltip.scrolls")
                .withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < 4; i++) {
            var scroll = ElementalBowScrollStorage.get(stack, i, lookup());
            if (scroll.isEmpty()) continue;
            var data = ElementalBowScrollStorage.readSpell(stack, i, lookup());
            var mode = data == SpellData.EMPTY ? null : ElementalBowModeManager.getResolvedDefinition(data.getSpell().getSpellResource());
            if (mode != null && data.getSpell().isEnabled() && i < ElementalBow.getEnabledCalibrationScrollSlotCount(stack)) {
                lines.add(Component.literal((i + 1) + ": ").append(TooltipsUtils.getTitleComponent(
                        new SpellData(mode.spell(), mode.resolveSpellLevel(stack, data.getLevel())), player)));
            } else {
                lines.add(Component.translatable("item.apprenticecodex.elemental_bow.tooltip.inactive", i + 1, scroll.getHoverName())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
