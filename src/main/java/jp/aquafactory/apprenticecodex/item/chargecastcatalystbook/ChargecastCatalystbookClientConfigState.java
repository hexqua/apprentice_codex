package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;

public final class ChargecastCatalystbookClientConfigState {
    private static ChargecastCatalystbookServerConfig.Values values =
            ChargecastCatalystbookServerConfig.Values.DEFAULT;

    private ChargecastCatalystbookClientConfigState() {
    }

    public static ChargecastCatalystbookServerConfig.Values values() {
        return values;
    }

    public static void set(ChargecastCatalystbookServerConfig.Values values) {
        ChargecastCatalystbookClientConfigState.values = values;
    }

    public static void reset() {
        values = ChargecastCatalystbookServerConfig.Values.DEFAULT;
    }
}
