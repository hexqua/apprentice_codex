package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Optional;

public final class ScrollSlotTooltipClientHelper {
    private static final String PREFIX = "item.apprenticecodex.common.scroll_slots.";

    private ScrollSlotTooltipClientHelper() {}

    public static void appendHint(List<Component> lines, int count) {
        if (count > 0) lines.add(Component.translatable(PREFIX + "foldout_hint", count).withStyle(ChatFormatting.GRAY));
    }

    public static void appendLabel(List<Component> lines) {
        lines.add(Component.translatable(PREFIX + "foldout_label").withStyle(ChatFormatting.GRAY));
    }

    public static void appendSpell(List<Component> lines, SpellData data, Component fallback,
                                   boolean usable, boolean selected, LocalPlayer player) {
        MutableComponent name;
        if (SpellCalibrationImbueTarget.isValidCalibrationSpell(data)) {
            var spell = data.getSpell();
            name = spell.getDisplayName(player).copy().append(" ")
                    .append(TooltipsUtils.getLevelComponenet(data, player));
            if (usable) name.withStyle(spell.getSchoolType().getDisplayName().getStyle());
        } else {
            name = fallback.copy();
        }
        if (!usable) name = darkGray(name);
        String state = !usable ? "inactive" : selected ? "selected" : "not_selected";
        lines.add(Component.translatable(PREFIX + "spell_line." + state, name)
                .withStyle(!usable ? ChatFormatting.DARK_GRAY : selected ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }

    private static MutableComponent darkGray(Component component) {
        // 翻訳引数や子componentに明示されたschool色も無効行では残さない。
        var result = Component.empty();
        component.visit((style, text) -> {
            result.append(Component.literal(text).setStyle(style.withColor(ChatFormatting.DARK_GRAY)));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
