package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ManaShieldCharmServerConfig {
    private final ModConfigSpec.DoubleValue manaPerDamage;
    private final ModConfigSpec.IntValue recoveryThresholdMana;
    private final ModConfigSpec.DoubleValue synchronizationManaPerDamage;
    private final ModConfigSpec.DoubleValue neutralizationRecoverManaPerDamage;
    private final ModConfigSpec.IntValue shellArmorDurabilityDamage;
    private final ModConfigSpec.IntValue invulnerableTimeTicks;
    private Double manaPerDamageOverride;
    private Integer recoveryThresholdManaOverride;
    private Double synchronizationManaPerDamageOverride;
    private Double neutralizationRecoverManaPerDamageOverride;
    private Integer shellArmorDurabilityDamageOverride;
    private Integer invulnerableTimeTicksOverride;

    private ManaShieldCharmServerConfig(
            ModConfigSpec.DoubleValue manaPerDamage,
            ModConfigSpec.IntValue recoveryThresholdMana,
            ModConfigSpec.DoubleValue synchronizationManaPerDamage,
            ModConfigSpec.DoubleValue neutralizationRecoverManaPerDamage,
            ModConfigSpec.IntValue shellArmorDurabilityDamage,
            ModConfigSpec.IntValue invulnerableTimeTicks
    ) {
        this.manaPerDamage = manaPerDamage;
        this.recoveryThresholdMana = recoveryThresholdMana;
        this.synchronizationManaPerDamage = synchronizationManaPerDamage;
        this.neutralizationRecoverManaPerDamage = neutralizationRecoverManaPerDamage;
        this.shellArmorDurabilityDamage = shellArmorDurabilityDamage;
        this.invulnerableTimeTicks = invulnerableTimeTicks;
    }

    public static ManaShieldCharmServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ManaShieldCharm");

        var manaPerDamage = builder
                .comment("Mana spent per whole damage point absorbed by Mana Shield Charm. 0 makes absorbed whole-damage steps free and can make the charm effectively invulnerable against many damage sources; use only when that is intended.")
                .defineInRange("manaPerDamage", 25.0D, 0.0D, 10000.0D);
        var recoveryThresholdMana = builder
                .comment("Mana required to reactivate Mana Shield Charm after depletion. 0 disables depletion cooldown.")
                .defineInRange("recoveryThresholdMana", 100, 0, 10000);
        var synchronizationManaPerDamage = builder
                .comment("Extra mana spent per whole damage point mitigated by Synchronization before the normal barrier stage.")
                .defineInRange("synchronizationManaPerDamage", 30.0D, 0.0D, 10000.0D);
        var neutralizationRecoverManaPerDamage = builder
                .comment("Mana recovered per whole damage point nullified by Neutralization. 0 keeps nullification but disables mana recovery.")
                .defineInRange("neutralizationRecoverManaPerDamage", 25.0D, 0.0D, 10000.0D);
        var shellArmorDurabilityDamage = builder
                .comment("Durability damage applied to each equipped armor piece when Shell uses the armor path. 0 disables this armor wear.")
                .defineInRange("shellArmorDurabilityDamage", 1, 0, 1000);
        var invulnerableTimeTicks = builder
                .comment("Vanilla-style invulnerability ticks applied after Mana Shield Charm fully negates damage.")
                .defineInRange("invulnerableTimeTicks", 20, 0, 72000);

        builder.pop();
        return new ManaShieldCharmServerConfig(
                manaPerDamage,
                recoveryThresholdMana,
                synchronizationManaPerDamage,
                neutralizationRecoverManaPerDamage,
                shellArmorDurabilityDamage,
                invulnerableTimeTicks
        );
    }

    public float manaPerDamage() {
        return (manaPerDamageOverride == null ? manaPerDamage.get() : manaPerDamageOverride).floatValue();
    }

    public int recoveryThresholdMana() {
        return recoveryThresholdManaOverride == null ? recoveryThresholdMana.get() : recoveryThresholdManaOverride;
    }

    public float synchronizationManaPerDamage() {
        return (synchronizationManaPerDamageOverride == null
                ? synchronizationManaPerDamage.get()
                : synchronizationManaPerDamageOverride).floatValue();
    }

    public float neutralizationRecoverManaPerDamage() {
        return (neutralizationRecoverManaPerDamageOverride == null
                ? neutralizationRecoverManaPerDamage.get()
                : neutralizationRecoverManaPerDamageOverride).floatValue();
    }

    public int shellArmorDurabilityDamage() {
        return shellArmorDurabilityDamageOverride == null
                ? shellArmorDurabilityDamage.get()
                : shellArmorDurabilityDamageOverride;
    }

    public int invulnerableTimeTicks() {
        return invulnerableTimeTicksOverride == null ? invulnerableTimeTicks.get() : invulnerableTimeTicksOverride;
    }

    public void setForGameTest(
            double manaPerDamage,
            int recoveryThresholdMana,
            double synchronizationManaPerDamage,
            double neutralizationRecoverManaPerDamage,
            int shellArmorDurabilityDamage,
            int invulnerableTimeTicks
    ) {
        this.manaPerDamageOverride = manaPerDamage;
        this.recoveryThresholdManaOverride = recoveryThresholdMana;
        this.synchronizationManaPerDamageOverride = synchronizationManaPerDamage;
        this.neutralizationRecoverManaPerDamageOverride = neutralizationRecoverManaPerDamage;
        this.shellArmorDurabilityDamageOverride = shellArmorDurabilityDamage;
        this.invulnerableTimeTicksOverride = invulnerableTimeTicks;
    }
}
