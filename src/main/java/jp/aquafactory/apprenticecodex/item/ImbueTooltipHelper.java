package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ImbueTooltipHelper {
    private static final String TOOLTIP_PREFIX = "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.";

    private ImbueTooltipHelper() {
    }

    public static void appendBlankLineIfNeeded(List<Component> lines) {
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
    }

    public static boolean appendHintIfDetailsHidden(List<Component> lines) {
        if (Screen.hasShiftDown()) {
            return false;
        }

        lines.add(translatableGray(TOOLTIP_PREFIX + "hint"));
        return true;
    }

    public static void appendTooltipSection(
            List<Component> lines,
            List<Component> sectionLines,
            String titleTranslationKey,
            @Nullable String emptyTranslationKey
    ) {
        lines.add(Component.translatable(titleTranslationKey).withStyle(ChatFormatting.GOLD));
        if (sectionLines.isEmpty()) {
            if (emptyTranslationKey != null) {
                lines.add(Component.translatable(emptyTranslationKey).withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        lines.addAll(sectionLines);
    }

    public static List<Component> collectCastTypeRestrictionLines(Set<SpellGunCastType> supportedCastTypes) {
        var translatedLines = new ArrayList<Component>();
        if (supportedCastTypes.size() == 2
                && supportedCastTypes.contains(SpellGunCastType.INSTANT)
                && supportedCastTypes.contains(SpellGunCastType.LONG)) {
            translatedLines.add(translatableGray(TOOLTIP_PREFIX + "restrict_restrict_not_continuous"));
        } else if (supportedCastTypes.size() == 1) {
            if (supportedCastTypes.contains(SpellGunCastType.INSTANT)) {
                translatedLines.add(translatableGray(TOOLTIP_PREFIX + "restrict_restrict_instant_only"));
            }
            if (supportedCastTypes.contains(SpellGunCastType.LONG)) {
                translatedLines.add(translatableGray(TOOLTIP_PREFIX + "restrict_restrict_long_only"));
            }
        }
        return translatedLines;
    }

    public static void appendMaxCooldownRestrictionLine(List<Component> lines, @Nullable Integer maxCooldownTicks) {
        if (maxCooldownTicks == null) {
            return;
        }

        lines.add(translatableGray(
                TOOLTIP_PREFIX + "restrict_restrict_cooldown",
                formatTooltipSeconds(maxCooldownTicks)
        ));
    }

    public static void appendNoRecastRestrictionLine(List<Component> lines, boolean requireZeroRecast) {
        if (!requireZeroRecast) {
            return;
        }

        lines.add(translatableGray(TOOLTIP_PREFIX + "restrict_restrict_no_recast"));
    }

    public static Component translatableGray(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withStyle(ChatFormatting.GRAY);
    }

    public static String formatTooltipSeconds(int ticks) {
        return BigDecimal.valueOf(ticks)
                .divide(BigDecimal.valueOf(20L))
                .stripTrailingZeros()
                .toPlainString();
    }
}
