package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class ElementalBowServerConfig {
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> magicArrowCatalystItems;
    private final ForgeConfigSpec.DoubleValue magicReadyDrawTicksMultiplier;
    private final ForgeConfigSpec.DoubleValue overheatAdditionalManaLinearMultiplier;
    private final ForgeConfigSpec.DoubleValue overheatAdditionalManaQuadraticMultiplier;
    private final ForgeConfigSpec.DoubleValue overheatDurationMultiplier;
    private final ForgeConfigSpec.IntValue overheatDurationMinTicks;
    private final ForgeConfigSpec.IntValue overheatDurationCapTicks;
    private final ForgeConfigSpec.DoubleValue powerArrowSpellLevelBonusPerLevel;

    private List<String> magicArrowCatalystItemsOverride;
    private Double magicReadyDrawTicksMultiplierOverride;
    private Double overheatAdditionalManaLinearMultiplierOverride;
    private Double overheatAdditionalManaQuadraticMultiplierOverride;
    private Double overheatDurationMultiplierOverride;
    private Integer overheatDurationMinTicksOverride;
    private Integer overheatDurationCapTicksOverride;
    private Double powerArrowSpellLevelBonusPerLevelOverride;

    private ElementalBowServerConfig(
            ForgeConfigSpec.ConfigValue<List<? extends String>> magicArrowCatalystItems,
            ForgeConfigSpec.DoubleValue magicReadyDrawTicksMultiplier,
            ForgeConfigSpec.DoubleValue overheatAdditionalManaLinearMultiplier,
            ForgeConfigSpec.DoubleValue overheatAdditionalManaQuadraticMultiplier,
            ForgeConfigSpec.DoubleValue overheatDurationMultiplier,
            ForgeConfigSpec.IntValue overheatDurationMinTicks,
            ForgeConfigSpec.IntValue overheatDurationCapTicks,
            ForgeConfigSpec.DoubleValue powerArrowSpellLevelBonusPerLevel
    ) {
        this.magicArrowCatalystItems = magicArrowCatalystItems;
        this.magicReadyDrawTicksMultiplier = magicReadyDrawTicksMultiplier;
        this.overheatAdditionalManaLinearMultiplier = overheatAdditionalManaLinearMultiplier;
        this.overheatAdditionalManaQuadraticMultiplier = overheatAdditionalManaQuadraticMultiplier;
        this.overheatDurationMultiplier = overheatDurationMultiplier;
        this.overheatDurationMinTicks = overheatDurationMinTicks;
        this.overheatDurationCapTicks = overheatDurationCapTicks;
        this.powerArrowSpellLevelBonusPerLevel = powerArrowSpellLevelBonusPerLevel;
    }

    public static ElementalBowServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("ElementalBow");
        var magicArrowCatalystItems = builder
                .comment("Item IDs accepted as Elemental Bow magic mode arrow catalysts. Empty list makes non-Synthesis survival casts unusable.")
                .defineListAllowEmpty("magicArrowCatalystItems", List.of("minecraft:arrow"), ElementalBowServerConfig::isItemId);
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
                magicArrowCatalystItems,
                magicReadyDrawTicksMultiplier,
                overheatAdditionalManaLinearMultiplier,
                overheatAdditionalManaQuadraticMultiplier,
                overheatDurationMultiplier,
                overheatDurationMinTicks,
                overheatDurationCapTicks,
                powerArrowSpellLevelBonusPerLevel
        );
    }

    public List<String> magicArrowCatalystItems() {
        return Objects.requireNonNullElseGet(magicArrowCatalystItemsOverride, () -> magicArrowCatalystItems.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public List<ResourceLocation> magicArrowCatalystItemIds() {
        return magicArrowCatalystItems().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .toList();
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

    public void setMagicArrowCatalystItemsForGameTest(List<String> magicArrowCatalystItems) {
        this.magicArrowCatalystItemsOverride = List.copyOf(magicArrowCatalystItems);
    }

    private static boolean isItemId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
