package jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook;

public final class IsekaiTravelGuidebookTooltipState {
    private static boolean showTooltip = true;

    private IsekaiTravelGuidebookTooltipState() {
    }

    public static boolean shouldShowTooltip() {
        return showTooltip;
    }

    public static void setShowTooltip(boolean value) {
        showTooltip = value;
    }

    public static void reset() {
        showTooltip = true;
    }
}
