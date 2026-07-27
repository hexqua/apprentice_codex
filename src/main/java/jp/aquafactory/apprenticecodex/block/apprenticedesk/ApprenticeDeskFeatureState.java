package jp.aquafactory.apprenticecodex.block.apprenticedesk;

import io.redspace.ironsspellbooks.api.spells.SpellRarity;

public final class ApprenticeDeskFeatureState {
    private static final int DEFAULT_COMMON_INK_MAX_USES = 5;
    private static final int DEFAULT_UNCOMMON_INK_MAX_USES = 4;
    private static final int DEFAULT_RARE_INK_MAX_USES = 3;
    private static final int DEFAULT_EPIC_INK_MAX_USES = 3;
    private static final int DEFAULT_LEGENDARY_INK_MAX_USES = 2;

    private static boolean disableNonJobSiteFeatures;
    private static int commonInkMaxUses = DEFAULT_COMMON_INK_MAX_USES;
    private static int uncommonInkMaxUses = DEFAULT_UNCOMMON_INK_MAX_USES;
    private static int rareInkMaxUses = DEFAULT_RARE_INK_MAX_USES;
    private static int epicInkMaxUses = DEFAULT_EPIC_INK_MAX_USES;
    private static int legendaryInkMaxUses = DEFAULT_LEGENDARY_INK_MAX_USES;

    private ApprenticeDeskFeatureState() {
    }

    public static boolean areNonJobSiteFeaturesDisabled() {
        return disableNonJobSiteFeatures;
    }

    public static void setDisableNonJobSiteFeatures(boolean value) {
        disableNonJobSiteFeatures = value;
    }

    public static int inkMaxUses(SpellRarity rarity) {
        return switch (rarity) {
            case COMMON -> commonInkMaxUses;
            case UNCOMMON -> uncommonInkMaxUses;
            case RARE -> rareInkMaxUses;
            case EPIC -> epicInkMaxUses;
            case LEGENDARY -> legendaryInkMaxUses;
        };
    }

    public static void setInkMaxUses(
            int common,
            int uncommon,
            int rare,
            int epic,
            int legendary
    ) {
        commonInkMaxUses = sanitizeUses(common);
        uncommonInkMaxUses = sanitizeUses(uncommon);
        rareInkMaxUses = sanitizeUses(rare);
        epicInkMaxUses = sanitizeUses(epic);
        legendaryInkMaxUses = sanitizeUses(legendary);
    }

    public static void reset() {
        disableNonJobSiteFeatures = false;
        commonInkMaxUses = DEFAULT_COMMON_INK_MAX_USES;
        uncommonInkMaxUses = DEFAULT_UNCOMMON_INK_MAX_USES;
        rareInkMaxUses = DEFAULT_RARE_INK_MAX_USES;
        epicInkMaxUses = DEFAULT_EPIC_INK_MAX_USES;
        legendaryInkMaxUses = DEFAULT_LEGENDARY_INK_MAX_USES;
    }

    private static int sanitizeUses(int value) {
        return Math.max(1, value);
    }
}
