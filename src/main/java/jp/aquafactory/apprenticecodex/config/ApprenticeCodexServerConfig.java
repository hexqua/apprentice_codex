package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;

public final class ApprenticeCodexServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final EnumMap<DamageMultiplierKey, ForgeConfigSpec.DoubleValue> DAMAGE_MULTIPLIERS = new EnumMap<>(DamageMultiplierKey.class);

    private static final double DEFAULT_DAMAGE_MULTIPLIER = 1.0d;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.1d;
    private static final double MAX_DAMAGE_MULTIPLIER = 10.0d;

    static {
        var builder = new ForgeConfigSpec.Builder();

        builder.comment("Damage multiplier settings for spells added by this mod.")
                .push("DamageMultipliers");
        for (var key : DamageMultiplierKey.values()) {
            DAMAGE_MULTIPLIERS.put(
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

        SPEC = builder.build();
    }

    private ApprenticeCodexServerConfig() {
    }

    public static float damageMultiplier(DamageMultiplierKey key) {
        return DAMAGE_MULTIPLIERS.get(key).get().floatValue();
    }

    // コンフィグキーをenumに集約して、定義漏れやtypoを防ぐ.
    public enum DamageMultiplierKey {
        SKY_EDGE("skyEdge"),
        ARCANE_BLAST("arcaneBlast"),
        ARCHER_MULTIPLE("archerMultiple"),
        BREACHING_ENEMY("breachingEnemy"),
        ARCANE_BEAM("arcaneBeam"),
        BULLET_STREAM("bulletStream"),
        HIGANBANA("higanbana"),
        WORLD_FLATTER("worldFlatter"),
        FLY_SWATTER("flySwatter"),
        COMPOUND_PHIAL("compoundPhial"),
        COMMENCE_FIRE("commenceFire"),
        PHALANX_CHARGE("phalanxCharge"),
        SLASH_BLADE("slashBlade"),
        THERMAL_PROCESS("thermalProcess"),
        MANTIS_LEAP("mantisLeap"),
        FEATHER_RUSH("featherRush"),
        TINY_LUMBERJACK("tinyLumberjack"),
        MOON_LIGHT("moonLight"),
        QUICK_ARMS("quickArms");

        private final String configKey;

        DamageMultiplierKey(String configKey) {
            this.configKey = configKey;
        }

        public String configKey() {
            return configKey;
        }
    }
}
