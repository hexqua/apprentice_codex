package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ManaShieldCharmServerConfig {
    private final ModConfigSpec.DoubleValue manaPerDamage;
    private final ModConfigSpec.IntValue recoveryThresholdMana;
    private final ModConfigSpec.IntValue shellActivationManaCost;
    private final ModConfigSpec.DoubleValue synchronizationAdditionalManaPerDamage;
    private final ModConfigSpec.DoubleValue synchronizationArmorBypassManaReductionPerDamage;
    private final ModConfigSpec.IntValue neutralizationAntiManaArrowManaCost;
    private final ModConfigSpec.IntValue neutralizationCounterspellManaCost;
    private final ModConfigSpec.IntValue shellArmorDurabilityDamage;
    private final ModConfigSpec.IntValue invulnerableTimeTicks;
    private Double manaPerDamageOverride;
    private Integer recoveryThresholdManaOverride;
    private Integer shellActivationManaCostOverride;
    private Double synchronizationAdditionalManaPerDamageOverride;
    private Double synchronizationArmorBypassManaReductionPerDamageOverride;
    private Integer neutralizationAntiManaArrowManaCostOverride;
    private Integer neutralizationCounterspellManaCostOverride;
    private Integer shellArmorDurabilityDamageOverride;
    private Integer invulnerableTimeTicksOverride;

    private ManaShieldCharmServerConfig(
            ModConfigSpec.DoubleValue manaPerDamage,
            ModConfigSpec.IntValue recoveryThresholdMana,
            ModConfigSpec.IntValue shellActivationManaCost,
            ModConfigSpec.DoubleValue synchronizationAdditionalManaPerDamage,
            ModConfigSpec.DoubleValue synchronizationArmorBypassManaReductionPerDamage,
            ModConfigSpec.IntValue neutralizationAntiManaArrowManaCost,
            ModConfigSpec.IntValue neutralizationCounterspellManaCost,
            ModConfigSpec.IntValue shellArmorDurabilityDamage,
            ModConfigSpec.IntValue invulnerableTimeTicks
    ) {
        this.manaPerDamage = manaPerDamage;
        this.recoveryThresholdMana = recoveryThresholdMana;
        this.shellActivationManaCost = shellActivationManaCost;
        this.synchronizationAdditionalManaPerDamage = synchronizationAdditionalManaPerDamage;
        this.synchronizationArmorBypassManaReductionPerDamage = synchronizationArmorBypassManaReductionPerDamage;
        this.neutralizationAntiManaArrowManaCost = neutralizationAntiManaArrowManaCost;
        this.neutralizationCounterspellManaCost = neutralizationCounterspellManaCost;
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
        var shellActivationManaCost = builder
                .comment("Fixed mana spent whenever Shell reacts to incoming damage. The cost is collected even when armor mitigation cannot be applied.")
                .defineInRange("shellActivationManaCost", 50, 0, 10000);
        var synchronizationAdditionalManaPerDamage = builder
                .comment("Additional mana spent per whole damage point absorbed while Synchronization is active.")
                .defineInRange("synchronizationAdditionalManaPerDamage", 15.0D, 0.0D, 10000.0D);
        var synchronizationArmorBypassManaReductionPerDamage = builder
                .comment("Mana cost reduction per whole damage point when Synchronization absorbs eligible armor-bypassing damage. The final cost is clamped to zero.")
                .defineInRange("synchronizationArmorBypassManaReductionPerDamage", 30.0D, 0.0D, 10000.0D);
        var neutralizationAntiManaArrowManaCost = builder
                .comment("Mana spent when Neutralization blocks an Anti Mana Arrow. The resistance requires the full cost; 0 makes it free.")
                .defineInRange("neutralizationAntiManaArrowManaCost", 50, 0, 10000);
        var neutralizationCounterspellManaCost = builder
                .comment("Mana spent when Neutralization blocks Counterspell. The resistance requires the full cost; 0 makes it free.")
                .defineInRange("neutralizationCounterspellManaCost", 100, 0, 10000);
        var shellArmorDurabilityDamage = builder
                .comment("Multiplier for armor durability damage when Shell gains mitigation. Each equipped armor piece takes ceil(raw damage / 4 * multiplier); 0 disables this armor wear.")
                .defineInRange("shellArmorDurabilityDamage", 1, 0, 1000);
        var invulnerableTimeTicks = builder
                .comment("Vanilla-style invulnerability ticks applied after Mana Shield Charm fully negates damage.")
                .defineInRange("invulnerableTimeTicks", 20, 0, 72000);

        builder.pop();
        return new ManaShieldCharmServerConfig(
                manaPerDamage,
                recoveryThresholdMana,
                shellActivationManaCost,
                synchronizationAdditionalManaPerDamage,
                synchronizationArmorBypassManaReductionPerDamage,
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

    public int shellActivationManaCost() {
        return shellActivationManaCostOverride == null ? shellActivationManaCost.get() : shellActivationManaCostOverride;
    }

    public float synchronizationAdditionalManaPerDamage() {
        return (synchronizationAdditionalManaPerDamageOverride == null
                ? synchronizationAdditionalManaPerDamage.get()
                : synchronizationAdditionalManaPerDamageOverride).floatValue();
    }

    public float synchronizationArmorBypassManaReductionPerDamage() {
        return (synchronizationArmorBypassManaReductionPerDamageOverride == null
                ? synchronizationArmorBypassManaReductionPerDamage.get()
                : synchronizationArmorBypassManaReductionPerDamageOverride).floatValue();
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
            int shellActivationManaCost,
            double synchronizationAdditionalManaPerDamage,
            double synchronizationArmorBypassManaReductionPerDamage,
            int neutralizationAntiManaArrowManaCost,
            int neutralizationCounterspellManaCost,
            int shellArmorDurabilityDamage,
            int invulnerableTimeTicks
    ) {
        this.manaPerDamageOverride = manaPerDamage;
        this.recoveryThresholdManaOverride = recoveryThresholdMana;
        this.shellActivationManaCostOverride = shellActivationManaCost;
        this.synchronizationAdditionalManaPerDamageOverride = synchronizationAdditionalManaPerDamage;
        this.synchronizationArmorBypassManaReductionPerDamageOverride = synchronizationArmorBypassManaReductionPerDamage;
        this.neutralizationAntiManaArrowManaCostOverride = neutralizationAntiManaArrowManaCost;
        this.neutralizationCounterspellManaCostOverride = neutralizationCounterspellManaCost;
        this.shellArmorDurabilityDamageOverride = shellArmorDurabilityDamage;
        this.invulnerableTimeTicksOverride = invulnerableTimeTicks;
    }
}
