package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        if (hasDetailsKeyDown()) {
            return false;
        }

        lines.add(Component.translatable(TOOLTIP_PREFIX + "hint").withStyle(ChatFormatting.YELLOW));
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

    public static Component createAmmoTooltipLine(Item item, @Nullable String conditionTranslationKey) {
        var line = Component.literal("- ").append(item.getDescription());
        if (conditionTranslationKey != null) {
            line = line.append(Component.literal(" ("))
                    .append(Component.translatable(conditionTranslationKey))
                    .append(Component.literal(")"));
        }
        return line.withStyle(ChatFormatting.GRAY);
    }

    public static Component getAttackKeyName() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ImbueTooltipClientHelper.getAttackKeyName();
        }
        return Component.translatable("key.attack");
    }

    public static Component getUseKeyName() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ImbueTooltipClientHelper.getUseKeyName();
        }
        return Component.translatable("key.use");
    }

    public static Component getJumpKeyName() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ImbueTooltipClientHelper.getJumpKeyName();
        }
        return Component.translatable("key.jump");
    }

    public static Component translatableGray(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withStyle(ChatFormatting.GRAY);
    }

    public static String formatTooltipSeconds(int ticks) {
        // 小数点1位まで表示して切り捨てる.
        return BigDecimal.valueOf(ticks)
                .divide(BigDecimal.valueOf(20L), 1, RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString();
    }

    public static int resolveClientCooldownReductionAdjustedTicks(int baseCooldownTicks) {
        return FMLEnvironment.dist == Dist.CLIENT
                ? ImbueCooldownTooltipClientHelper.resolveCooldownReductionAdjustedTicks(baseCooldownTicks)
                : Math.max(0, baseCooldownTicks);
    }

    public static boolean hasDetailsKeyDown() {
        return FMLEnvironment.dist == Dist.CLIENT && ImbueTooltipClientHelper.hasDetailsKeyDown();
    }
}
