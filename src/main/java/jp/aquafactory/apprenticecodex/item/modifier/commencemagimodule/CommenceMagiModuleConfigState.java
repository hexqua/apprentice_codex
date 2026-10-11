package jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule;

import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig.Values;

public final class CommenceMagiModuleConfigState {
    private static Values values = CommenceMagiModuleServerConfig.DEFAULTS;

    private CommenceMagiModuleConfigState() {
    }

    public static Values values() {
        return values;
    }

    public static void set(Values synced) {
        values = synced;
    }

    public static void reset() {
        values = CommenceMagiModuleServerConfig.DEFAULTS;
    }
}
