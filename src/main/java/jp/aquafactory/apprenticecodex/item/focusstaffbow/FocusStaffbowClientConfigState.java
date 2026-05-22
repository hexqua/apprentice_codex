package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class FocusStaffbowClientConfigState {
    private static final List<ResourceLocation> DEFAULT_ARROW_CATALYST_ITEM_IDS =
            List.of(ResourceLocation.fromNamespaceAndPath("minecraft", "arrow"));
    private static boolean continuousFocusedCastEnabled = true;
    private static boolean arrowCatalystRequired = true;
    private static List<ResourceLocation> arrowCatalystItemIds = DEFAULT_ARROW_CATALYST_ITEM_IDS;
    private static FocusStaffbowChargeSettings chargeSettings = FocusStaffbowChargeSettings.DEFAULT;

    private FocusStaffbowClientConfigState() {
    }

    public static void set(
            boolean continuousFocusedCastEnabled,
            boolean arrowCatalystRequired,
            List<ResourceLocation> arrowCatalystItemIds,
            FocusStaffbowChargeSettings chargeSettings
    ) {
        FocusStaffbowClientConfigState.continuousFocusedCastEnabled = continuousFocusedCastEnabled;
        FocusStaffbowClientConfigState.arrowCatalystRequired = arrowCatalystRequired;
        FocusStaffbowClientConfigState.arrowCatalystItemIds = List.copyOf(arrowCatalystItemIds);
        FocusStaffbowClientConfigState.chargeSettings = chargeSettings;
    }

    public static void reset() {
        continuousFocusedCastEnabled = true;
        arrowCatalystRequired = true;
        arrowCatalystItemIds = DEFAULT_ARROW_CATALYST_ITEM_IDS;
        chargeSettings = FocusStaffbowChargeSettings.DEFAULT;
    }

    public static boolean continuousFocusedCastEnabled() {
        return continuousFocusedCastEnabled;
    }

    public static boolean arrowCatalystRequired() {
        return arrowCatalystRequired;
    }

    public static List<ResourceLocation> arrowCatalystItemIds() {
        return arrowCatalystItemIds;
    }

    public static FocusStaffbowChargeSettings chargeSettings() {
        return chargeSettings;
    }
}
