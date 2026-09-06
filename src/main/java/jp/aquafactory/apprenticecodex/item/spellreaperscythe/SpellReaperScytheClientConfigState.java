package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig;

public final class SpellReaperScytheClientConfigState {
    private static SpellReaperScytheServerConfig.Values values =
            SpellReaperScytheServerConfig.Values.DEFAULT;

    private SpellReaperScytheClientConfigState() {
    }

    public static SpellReaperScytheServerConfig.Values values() {
        return values;
    }

    public static void set(SpellReaperScytheServerConfig.Values values) {
        SpellReaperScytheClientConfigState.values = values;
    }

    public static void reset() {
        values = SpellReaperScytheServerConfig.Values.DEFAULT;
    }
}
