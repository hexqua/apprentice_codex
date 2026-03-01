package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ScarletThirstServerConfig {
    private final ModConfigSpec.DoubleValue requiredHealth;
    private final ModConfigSpec.DoubleValue drainHealth;
    private final ModConfigSpec.DoubleValue drainEmergencyHealth;
    private final ModConfigSpec.DoubleValue recoverMana;
    private final ModConfigSpec.DoubleValue recoverEmergencyMana;

    private ScarletThirstServerConfig(
            ModConfigSpec.DoubleValue requiredHealth,
            ModConfigSpec.DoubleValue drainHealth,
            ModConfigSpec.DoubleValue drainEmergencyHealth,
            ModConfigSpec.DoubleValue recoverMana,
            ModConfigSpec.DoubleValue recoverEmergencyMana
    ) {
        this.requiredHealth = requiredHealth;
        this.drainHealth = drainHealth;
        this.drainEmergencyHealth = drainEmergencyHealth;
        this.recoverMana = recoverMana;
        this.recoverEmergencyMana = recoverEmergencyMana;
    }

    public static ScarletThirstServerConfig define(ModConfigSpec.Builder builder) {
        builder.comment("Health settings use Minecraft health points (2 = 1 heart).")
                .push("ScarletThirst");

        var requiredHealth = builder.defineInRange("requiredHealth", 4.0d, 2.0d, 20.0d);
        var drainHealth = builder.defineInRange("drainHealth", 1.0d, 1.0d, 20.0d);
        var drainEmergencyHealth = builder.defineInRange("drainEmergencyHealth", 4.0d, 1.0d, 20.0d);
        var recoverMana = builder.defineInRange("recoverMana", 30.0d, 0.0d, 10000.0d);
        var recoverEmergencyMana = builder.defineInRange("recoverEmergencyMana", 100.0d, 0.0d, 10000.0d);

        builder.pop();
        return new ScarletThirstServerConfig(
                requiredHealth,
                drainHealth,
                drainEmergencyHealth,
                recoverMana,
                recoverEmergencyMana
        );
    }

    public float requiredHealth() {
        return requiredHealth.get().floatValue();
    }

    public float drainHealth() {
        return drainHealth.get().floatValue();
    }

    public float drainEmergencyHealth() {
        return drainEmergencyHealth.get().floatValue();
    }

    public float recoverMana() {
        return recoverMana.get().floatValue();
    }

    public float recoverEmergencyMana() {
        return recoverEmergencyMana.get().floatValue();
    }
}

