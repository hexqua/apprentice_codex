package jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff;

import jp.aquafactory.apprenticecodex.config.item.ChargedTwinBladeStaffServerConfig;

public final class ChargedTwinBladeStaffClientConfigState {
    private static ChargedTwinBladeStaffServerConfig.Values values = ChargedTwinBladeStaffServerConfig.Values.DEFAULT;

    private ChargedTwinBladeStaffClientConfigState() {
    }

    public static ChargedTwinBladeStaffServerConfig.Values values() {
        return values;
    }

    public static void set(ChargedTwinBladeStaffServerConfig.Values updated) {
        values = updated;
    }

    public static void reset() {
        values = ChargedTwinBladeStaffServerConfig.Values.DEFAULT;
    }
}
