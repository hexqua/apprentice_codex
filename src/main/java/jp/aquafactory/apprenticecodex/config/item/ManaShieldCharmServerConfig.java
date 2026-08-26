package jp.aquafactory.apprenticecodex.config.item;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ManaShieldCharmServerConfig {
    private final ForgeConfigSpec.DoubleValue manaPerDamage;
    private final ForgeConfigSpec.IntValue recoveryThresholdMana;
    private final ForgeConfigSpec.DoubleValue synchronizationManaPerDamage;
    private final ForgeConfigSpec.IntValue neutralizationAntiManaArrowManaCost;
    private final ForgeConfigSpec.IntValue neutralizationCounterspellManaCost;
    private final ForgeConfigSpec.IntValue shellArmorDurabilityDamage;
    private final ForgeConfigSpec.IntValue invulnerableTimeTicks;
    private Double manaPerDamageOverride;
    private Integer recoveryThresholdManaOverride;
    private Double synchronizationManaPerDamageOverride;
    private Integer neutralizationAntiManaArrowManaCostOverride;
    private Integer neutralizationCounterspellManaCostOverride;
    private Integer shellArmorDurabilityDamageOverride;
    private Integer invulnerableTimeTicksOverride;

    private ManaShieldCharmServerConfig(
            ForgeConfigSpec.DoubleValue manaPerDamage,
            ForgeConfigSpec.IntValue recoveryThresholdMana,
            ForgeConfigSpec.DoubleValue synchronizationManaPerDamage,
            ForgeConfigSpec.IntValue neutralizationAntiManaArrowManaCost,
            ForgeConfigSpec.IntValue neutralizationCounterspellManaCost,
            ForgeConfigSpec.IntValue shellArmorDurabilityDamage,
            ForgeConfigSpec.IntValue invulnerableTimeTicks
    ) {
        this.manaPerDamage = manaPerDamage;
        this.recoveryThresholdMana = recoveryThresholdMana;
        this.synchronizationManaPerDamage = synchronizationManaPerDamage;
        this.neutralizationAntiManaArrowManaCost = neutralizationAntiManaArrowManaCost;
        this.neutralizationCounterspellManaCost = neutralizationCounterspellManaCost;
        this.shellArmorDurabilityDamage = shellArmorDurabilityDamage;
        this.invulnerableTimeTicks = invulnerableTimeTicks;
    }

    public static ManaShieldCharmServerConfig define(ForgeConfigSpec.Builder builder) {
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
        var neutralizationAntiManaArrowManaCost = builder
                .comment("Mana spent when Neutralization blocks an Anti Mana Arrow. The resistance requires the full cost; 0 makes it free.")
                .defineInRange("neutralizationAntiManaArrowManaCost", 50, 0, 10000);
        var neutralizationCounterspellManaCost = builder
                .comment("Mana spent when Neutralization blocks Counterspell. The resistance requires the full cost; 0 makes it free.")
                .defineInRange("neutralizationCounterspellManaCost", 100, 0, 10000);
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
                neutralizationAntiManaArrowManaCost,
                neutralizationCounterspellManaCost,
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

    public int neutralizationAntiManaArrowManaCost() {
        return neutralizationAntiManaArrowManaCostOverride == null
                ? neutralizationAntiManaArrowManaCost.get()
                : neutralizationAntiManaArrowManaCostOverride;
    }

    public int neutralizationCounterspellManaCost() {
        return neutralizationCounterspellManaCostOverride == null
                ? neutralizationCounterspellManaCost.get()
                : neutralizationCounterspellManaCostOverride;
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
            int neutralizationAntiManaArrowManaCost,
            int neutralizationCounterspellManaCost,
            int shellArmorDurabilityDamage,
            int invulnerableTimeTicks
    ) {
        this.manaPerDamageOverride = manaPerDamage;
        this.recoveryThresholdManaOverride = recoveryThresholdMana;
        this.synchronizationManaPerDamageOverride = synchronizationManaPerDamage;
        this.neutralizationAntiManaArrowManaCostOverride = neutralizationAntiManaArrowManaCost;
        this.neutralizationCounterspellManaCostOverride = neutralizationCounterspellManaCost;
        this.shellArmorDurabilityDamageOverride = shellArmorDurabilityDamage;
        this.invulnerableTimeTicksOverride = invulnerableTimeTicks;
    }
}
