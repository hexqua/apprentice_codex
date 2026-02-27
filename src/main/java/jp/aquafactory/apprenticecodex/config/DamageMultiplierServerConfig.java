package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;

final class DamageMultiplierServerConfig {
    private static final double DEFAULT_DAMAGE_MULTIPLIER = 1.0d;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.1d;
    private static final double MAX_DAMAGE_MULTIPLIER = 10.0d;

    private final EnumMap<DamageMultiplierKey, ForgeConfigSpec.DoubleValue> values;

    private DamageMultiplierServerConfig(EnumMap<DamageMultiplierKey, ForgeConfigSpec.DoubleValue> values) {
        this.values = values;
    }

    static DamageMultiplierServerConfig define(
            ForgeConfigSpec.Builder builder,
            DamageMultiplierKey[] keys
    ) {
        builder.comment("Damage multiplier settings for spells added by this mod.")
                .push("DamageMultipliers");

        var values = new EnumMap<DamageMultiplierKey, ForgeConfigSpec.DoubleValue>(DamageMultiplierKey.class);
        for (var key : keys) {
            values.put(
                    key,
                    builder.defineInRange(
                            key.configKey(),
                            DEFAULT_DAMAGE_MULTIPLIER,
                            MIN_DAMAGE_MULTIPLIER,
                            MAX_DAMAGE_MULTIPLIER
                    )
            );
        }
        builder.pop();
        return new DamageMultiplierServerConfig(values);
    }

    float value(DamageMultiplierKey key) {
        return values.get(key).get().floatValue();
    }
}
