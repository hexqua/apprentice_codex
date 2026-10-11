package jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule;

import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig;

public final class SpellboundArmsModuleConfigState {
    private static Values values = SpellboundArmsModuleServerConfig.DEFAULTS;

    private SpellboundArmsModuleConfigState() {}

    public static Values values() { return values; }
    public static void set(Values synced) { values = synced; }
    public static void reset() { values = SpellboundArmsModuleServerConfig.DEFAULTS; }
}
