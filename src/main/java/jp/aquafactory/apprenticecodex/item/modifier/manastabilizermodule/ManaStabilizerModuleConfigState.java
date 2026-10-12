package jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule;

import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig.Values;

public final class ManaStabilizerModuleConfigState {
    private static Values values = ManaStabilizerModuleServerConfig.DEFAULTS;
    private ManaStabilizerModuleConfigState() {}
    public static Values values() { return values; }
    public static void set(Values next) { values = next; }
    public static void reset() { values = ManaStabilizerModuleServerConfig.DEFAULTS; }
}
