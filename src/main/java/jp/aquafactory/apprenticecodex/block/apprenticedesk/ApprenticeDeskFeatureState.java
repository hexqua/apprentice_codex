package jp.aquafactory.apprenticecodex.block.apprenticedesk;

public final class ApprenticeDeskFeatureState {
    private static boolean disableNonJobSiteFeatures;

    private ApprenticeDeskFeatureState() {
    }

    public static boolean areNonJobSiteFeaturesDisabled() {
        return disableNonJobSiteFeatures;
    }

    public static void setDisableNonJobSiteFeatures(boolean value) {
        disableNonJobSiteFeatures = value;
    }

    public static void reset() {
        disableNonJobSiteFeatures = false;
    }
}
