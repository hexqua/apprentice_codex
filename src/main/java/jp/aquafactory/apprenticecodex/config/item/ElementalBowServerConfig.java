package jp.aquafactory.apprenticecodex.config.item;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ElementalBowServerConfig {
    private final ModConfigSpec.DoubleValue magicReadyDrawTicksMultiplier;
    private final ModConfigSpec.DoubleValue overheatAdditionalManaLinearMultiplier;
    private final ModConfigSpec.DoubleValue overheatAdditionalManaQuadraticMultiplier;
    private final ModConfigSpec.DoubleValue overheatDurationMultiplier;
    private final ModConfigSpec.IntValue overheatDurationMinTicks;
    private final ModConfigSpec.IntValue overheatDurationCapTicks;
    private final ModConfigSpec.DoubleValue powerArrowSpellLevelBonusPerLevel;

    private Double magicReadyDrawTicksMultiplierOverride;
    private Double overheatAdditionalManaLinearMultiplierOverride;
    private Double overheatAdditionalManaQuadraticMultiplierOverride;
    private Double overheatDurationMultiplierOverride;
    private Integer overheatDurationMinTicksOverride;
    private Integer overheatDurationCapTicksOverride;
    private Double powerArrowSpellLevelBonusPerLevelOverride;

    private ElementalBowServerConfig(
            ModConfigSpec.DoubleValue magicReadyDrawTicksMultiplier,
            ModConfigSpec.DoubleValue overheatAdditionalManaLinearMultiplier,
            ModConfigSpec.DoubleValue overheatAdditionalManaQuadraticMultiplier,
            ModConfigSpec.DoubleValue overheatDurationMultiplier,
            ModConfigSpec.IntValue overheatDurationMinTicks,
            ModConfigSpec.IntValue overheatDurationCapTicks,
            ModConfigSpec.DoubleValue powerArrowSpellLevelBonusPerLevel
    ) {
        this.magicReadyDrawTicksMultiplier = magicReadyDrawTicksMultiplier;
        this.overheatAdditionalManaLinearMultiplier = overheatAdditionalManaLinearMultiplier;
        this.overheatAdditionalManaQuadraticMultiplier = overheatAdditionalManaQuadraticMultiplier;
        this.overheatDurationMultiplier = overheatDurationMultiplier;
        this.overheatDurationMinTicks = overheatDurationMinTicks;
        this.overheatDurationCapTicks = overheatDurationCapTicks;
        this.powerArrowSpellLevelBonusPerLevel = powerArrowSpellLevelBonusPerLevel;
    }

    public static ElementalBowServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ElementalBow");
        var magicReadyDrawTicksMultiplier = builder
                .comment("Multiplier applied to Elemental Bow magic mode required draw ticks from its mode profile.")
                .defineInRange("magicReadyDrawTicksMultiplier", 1.0D, 0.0D, 100.0D);
        var overheatAdditionalManaLinearMultiplier = builder
                .comment("Linear extra mana multiplier per repeated Elemental Bow magic shot overheat step.")
                .defineInRange("overheatAdditionalManaLinearMultiplier", 0.20D, 0.0D, 100.0D);
        var overheatAdditionalManaQuadraticMultiplier = builder
                .comment("Quadratic extra mana multiplier per repeated Elemental Bow magic shot overheat step.")
                .defineInRange("overheatAdditionalManaQuadraticMultiplier", 0.08D, 0.0D, 100.0D);
        var overheatDurationMultiplier = builder
                .comment("Multiplier applied to Elemental Bow magic shot overheat duration after cooldown resolution.")
                .defineInRange("overheatDurationMultiplier", 1.0D, 0.0D, 100.0D);
        var overheatDurationMinTicks = builder
                .comment("Minimum Elemental Bow magic shot overheat duration in ticks. 0 disables this minimum.")
                .defineInRange("overheatDurationMinTicks", 0, 0, 72000);
        var overheatDurationCapTicks = builder
                .comment("Maximum Elemental Bow magic shot overheat duration in ticks. 0 disables this cap.")
                .defineInRange("overheatDurationCapTicks", 0, 0, Integer.MAX_VALUE);
        var powerArrowSpellLevelBonusPerLevel = builder
                .comment("Elemental Bow magic shot spell level bonus per Power enchantment level. Fractional totals are rounded down.")
                .defineInRange("powerArrowSpellLevelBonusPerLevel", 1.0D, 0.0D, 100.0D);
        builder.pop();

        return new ElementalBowServerConfig(
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks,
                powerArrowSpellLevelBonusPerLevel
        );
    }

    public double magicReadyDrawTicksMultiplier() {
        return magicReadyDrawTicksMultiplierOverride == null
                ? magicReadyDrawTicksMultiplier.get()
                : magicReadyDrawTicksMultiplierOverride;
    }

    public float overheatAdditionalManaLinearMultiplier() {
        return (overheatAdditionalManaLinearMultiplierOverride == null
                ? overheatAdditionalManaLinearMultiplier.get()
                : overheatAdditionalManaLinearMultiplierOverride).floatValue();
    }

    public float overheatAdditionalManaQuadraticMultiplier() {
        return (overheatAdditionalManaQuadraticMultiplierOverride == null
                ? overheatAdditionalManaQuadraticMultiplier.get()
                : overheatAdditionalManaQuadraticMultiplierOverride).floatValue();
    }

    public double overheatDurationMultiplier() {
        return overheatDurationMultiplierOverride == null
                ? overheatDurationMultiplier.get()
                : overheatDurationMultiplierOverride;
    }

    public int overheatDurationMinTicks() {
        return overheatDurationMinTicksOverride == null ? overheatDurationMinTicks.get() : overheatDurationMinTicksOverride;
    }

    public int overheatDurationCapTicks() {
        return overheatDurationCapTicksOverride == null ? overheatDurationCapTicks.get() : overheatDurationCapTicksOverride;
    }

    public double powerArrowSpellLevelBonusPerLevel() {
        return powerArrowSpellLevelBonusPerLevelOverride == null
                ? powerArrowSpellLevelBonusPerLevel.get()
                : powerArrowSpellLevelBonusPerLevelOverride;
    }

    public void setForGameTest(
            double magicReadyDrawTicksMultiplier,
            double overheatAdditionalManaLinearMultiplier,
            double overheatAdditionalManaQuadraticMultiplier,
            double overheatDurationMultiplier,
            int overheatDurationMinTicks,
            int overheatDurationCapTicks,
            double powerArrowSpellLevelBonusPerLevel
    ) {
        this.magicReadyDrawTicksMultiplierOverride = magicReadyDrawTicksMultiplier;
        this.overheatAdditionalManaLinearMultiplierOverride = overheatAdditionalManaLinearMultiplier;
        this.overheatAdditionalManaQuadraticMultiplierOverride = overheatAdditionalManaQuadraticMultiplier;
        this.overheatDurationMultiplierOverride = overheatDurationMultiplier;
        this.overheatDurationMinTicksOverride = overheatDurationMinTicks;
        this.overheatDurationCapTicksOverride = overheatDurationCapTicks;
        this.powerArrowSpellLevelBonusPerLevelOverride = powerArrowSpellLevelBonusPerLevel;
    }
}
