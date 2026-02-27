package jp.aquafactory.apprenticecodex.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;

public final class ApprenticeCodexServerConfig {
    public static final ForgeConfigSpec SPEC;
    private static final EnumMap<DamageMultiplierKey, ForgeConfigSpec.DoubleValue> DAMAGE_MULTIPLIERS = new EnumMap<>(DamageMultiplierKey.class);

    private static final double DEFAULT_DAMAGE_MULTIPLIER = 1.0d;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.1d;
    private static final double MAX_DAMAGE_MULTIPLIER = 10.0d;
    private static final ForgeConfigSpec.DoubleValue SCARLET_THIRST_REQUIRED_HEALTH;
    private static final ForgeConfigSpec.DoubleValue SCARLET_THIRST_DRAIN_HEALTH;
    private static final ForgeConfigSpec.DoubleValue SCARLET_THIRST_DRAIN_EMERGENCY_HEALTH;
    private static final ForgeConfigSpec.DoubleValue SCARLET_THIRST_RECOVER_MANA;
    private static final ForgeConfigSpec.DoubleValue SCARLET_THIRST_RECOVER_EMERGENCY_MANA;
    private static final ForgeConfigSpec.BooleanValue CRAFTSMANS_DELIGHT_CAN_IMBUE_ENCHANTMENT;
    private static final ForgeConfigSpec.DoubleValue CRAFTSMANS_DELIGHT_REQUIRED_MANA;
    private static final ForgeConfigSpec.IntValue CRAFTSMANS_DELIGHT_FORTUNE_LEVEL;
    private static final ForgeConfigSpec.IntValue PASTEL_STAFF_AMPLIFY_TINTED_MAGIC;

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

        builder.push("Items");

        builder.comment("Health settings use Minecraft health points (2 = 1 heart).")
                .push("ScarletThirst");
        SCARLET_THIRST_REQUIRED_HEALTH = builder.defineInRange("requiredHealth", 4.0d, 2.0d, 20.0d);
        SCARLET_THIRST_DRAIN_HEALTH = builder.defineInRange("drainHealth", 1.0d, 1.0d, 20.0d);
        SCARLET_THIRST_DRAIN_EMERGENCY_HEALTH = builder.defineInRange("drainEmergencyHealth", 4.0d, 1.0d, 20.0d);
        SCARLET_THIRST_RECOVER_MANA = builder.defineInRange("recoverMana", 30.0d, 0.0d, 10000.0d);
        SCARLET_THIRST_RECOVER_EMERGENCY_MANA = builder.defineInRange("recoverEmergencyMana", 100.0d, 0.0d, 10000.0d);
        builder.pop();

        builder.push("CraftsmansDelight");
        CRAFTSMANS_DELIGHT_CAN_IMBUE_ENCHANTMENT = builder.define("canImbueEnchantment", true);
        CRAFTSMANS_DELIGHT_REQUIRED_MANA = builder.defineInRange("requiredMana", 500.0d, 0.0d, 10000.0d);
        CRAFTSMANS_DELIGHT_FORTUNE_LEVEL = builder.defineInRange("fortuneLevel", 3, 1, 10);
        builder.pop();

        builder.push("PastelStaff");
        PASTEL_STAFF_AMPLIFY_TINTED_MAGIC = builder.defineInRange("amplify_tinted_magic", 20, 0, 1000);
        builder.pop();

        builder.pop();

        SPEC = builder.build();
    }

    private ApprenticeCodexServerConfig() {
    }

    public static float damageMultiplier(DamageMultiplierKey key) {
        return DAMAGE_MULTIPLIERS.get(key).get().floatValue();
    }

    public static float scarletThirstRequiredHealth() {
        return SCARLET_THIRST_REQUIRED_HEALTH.get().floatValue();
    }

    public static float scarletThirstDrainHealth() {
        return SCARLET_THIRST_DRAIN_HEALTH.get().floatValue();
    }

    public static float scarletThirstDrainEmergencyHealth() {
        return SCARLET_THIRST_DRAIN_EMERGENCY_HEALTH.get().floatValue();
    }

    public static float scarletThirstRecoverMana() {
        return SCARLET_THIRST_RECOVER_MANA.get().floatValue();
    }

    public static float scarletThirstRecoverEmergencyMana() {
        return SCARLET_THIRST_RECOVER_EMERGENCY_MANA.get().floatValue();
    }

    public static boolean craftsmansDelightCanImbueEnchantment() {
        return CRAFTSMANS_DELIGHT_CAN_IMBUE_ENCHANTMENT.get();
    }

    public static float craftsmansDelightRequiredMana() {
        return CRAFTSMANS_DELIGHT_REQUIRED_MANA.get().floatValue();
    }

    public static int craftsmansDelightFortuneLevel() {
        return CRAFTSMANS_DELIGHT_FORTUNE_LEVEL.get();
    }

    public static double pastelStaffAmplifyTintedMagicMultiplier() {
        return PASTEL_STAFF_AMPLIFY_TINTED_MAGIC.get() / 100.0d;
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
