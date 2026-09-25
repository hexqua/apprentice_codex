package jp.aquafactory.apprenticecodex.config.item;

import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public final class FullautoRapidcastSpellrifleServerConfig {
    private final ForgeConfigSpec.IntValue cooldownBypassThresholdTicks;
    private final ForgeConfigSpec.IntValue cooldownReductionTicks;
    private final ForgeConfigSpec.IntValue reducedCooldownMinimumTicks;
    private final ForgeConfigSpec.IntValue adsFullAutoIntervalTicks;
    private final ForgeConfigSpec.DoubleValue adsMovementSpeedMultiplier;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist;
    private List<String> spellDenylistOverride;
    private final ForgeConfigSpec.BooleanValue echoCastEnabled;
    private final ForgeConfigSpec.DoubleValue echoCastDamageMultiplier;
    private final ForgeConfigSpec.DoubleValue echoCastManaCostMultiplier;

    private FullautoRapidcastSpellrifleServerConfig(
            ForgeConfigSpec.IntValue cooldownBypassThresholdTicks,
            ForgeConfigSpec.IntValue cooldownReductionTicks,
            ForgeConfigSpec.IntValue reducedCooldownMinimumTicks,
            ForgeConfigSpec.IntValue adsFullAutoIntervalTicks,
            ForgeConfigSpec.DoubleValue adsMovementSpeedMultiplier,
            ForgeConfigSpec.ConfigValue<List<? extends String>> spellDenylist,
            ForgeConfigSpec.BooleanValue echoCastEnabled,
            ForgeConfigSpec.DoubleValue echoCastDamageMultiplier,
            ForgeConfigSpec.DoubleValue echoCastManaCostMultiplier
    ) {
        this.cooldownBypassThresholdTicks = cooldownBypassThresholdTicks;
        this.cooldownReductionTicks = cooldownReductionTicks;
        this.reducedCooldownMinimumTicks = reducedCooldownMinimumTicks;
        this.adsFullAutoIntervalTicks = adsFullAutoIntervalTicks;
        this.adsMovementSpeedMultiplier = adsMovementSpeedMultiplier;
        this.spellDenylist = spellDenylist;
        this.echoCastEnabled = echoCastEnabled;
        this.echoCastDamageMultiplier = echoCastDamageMultiplier;
        this.echoCastManaCostMultiplier = echoCastManaCostMultiplier;
    }

    public static FullautoRapidcastSpellrifleServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("FullautoRapidcastSpellrifle");
        var cooldownBypassThresholdTicks = builder
                .comment("Spells with an unmodified cooldown at or below this value skip cooldown for Fullauto Rapidcast Spellrifle casts. Ignores weapon multipliers, player reductions and cast time. 100 ticks = 5 seconds.")
                .defineInRange("cooldownBypassThresholdTicks", 20 * 5, 0, 72000);
        var cooldownReductionTicks = builder
                .comment("Cooldown ticks subtracted from longer Fullauto Rapidcast Spellrifle special casts. 200 ticks = 10 seconds.")
                .defineInRange("cooldownReductionTicks", 20 * 10, 0, 72000);
        var reducedCooldownMinimumTicks = builder
                .comment("Minimum cooldown after Fullauto Rapidcast Spellrifle special cast reduction. 10 ticks = 0.5 seconds.")
                .defineInRange("reducedCooldownMinimumTicks", 10, 0, 72000);
        var adsFullAutoIntervalTicks = builder
                .comment("Minimum server-side interval between full-auto special cast attempts, both hip fire and ADS.")
                .defineInRange("adsFullAutoIntervalTicks", 3, 1, 72000);
        var adsMovementSpeedMultiplier = builder
                .comment("Movement speed multiplier while aiming Fullauto Rapidcast Spellrifle. 0.7 slows movement by 30%; 0 prevents movement; 1 applies no slowdown.")
                .defineInRange("adsMovementSpeedMultiplier", 0.7D, 0.0D, 1.0D);
        var spellDenylist = builder
                .comment("Additional spell IDs blocked only for Fullauto Rapidcast Spellrifle special casts. Entries use \"modid:path\".")
                .defineListAllowEmpty("spellDenylist", List.<String>of(), FullautoRapidcastSpellrifleServerConfig::isSpellId);
        var echoCastEnabled = builder.comment("Enables the Multicast Echo Staff adjustment, including its mana penalty.")
                .define("echoCastEnabled", true);
        var echoCastDamageMultiplier = builder.comment("Additional multiplier applied to attack profile damage from this rifle only.")
                .defineInRange("echoCastDamageMultiplier", 1.0D, 0.0D, 100.0D);
        var echoCastManaCostMultiplier = builder.comment("Mana cost multiplier for all rifle spells while the echo adjustment is active, including unsupported spells.")
                .defineInRange("echoCastManaCostMultiplier", 2.0D, 1.0D, 10.0D);
        builder.pop();

        return new FullautoRapidcastSpellrifleServerConfig(
                cooldownBypassThresholdTicks,
                cooldownReductionTicks,
                reducedCooldownMinimumTicks,
                adsFullAutoIntervalTicks,
                adsMovementSpeedMultiplier,
                spellDenylist,
                echoCastEnabled,
                echoCastDamageMultiplier,
                echoCastManaCostMultiplier
        );
    }

    public boolean echoCastEnabled() { return echoCastEnabled.get(); }

    public double echoCastDamageMultiplier() { return echoCastDamageMultiplier.get(); }

    public double echoCastManaCostMultiplier() { return echoCastManaCostMultiplier.get(); }

    public int cooldownBypassThresholdTicks() {
        return cooldownBypassThresholdTicks.get();
    }

    public int cooldownReductionTicks() {
        return cooldownReductionTicks.get();
    }

    public int reducedCooldownMinimumTicks() {
        return reducedCooldownMinimumTicks.get();
    }

    public int adsFullAutoIntervalTicks() {
        return adsFullAutoIntervalTicks.get();
    }

    public double adsMovementSpeedMultiplier() {
        return adsMovementSpeedMultiplier.get();
    }

    public boolean isSpellDenied(ResourceLocation spellId) {
        if (spellId == null) {
            return false;
        }
        for (var configuredId : spellDenylist()) {
            if (spellId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    public List<String> spellDenylist() {
        return Objects.requireNonNullElseGet(spellDenylistOverride, () -> spellDenylist.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public void setSpellDenylistForGameTest(List<String> spellDenylist) {
        spellDenylistOverride = List.copyOf(spellDenylist);
    }

    private static boolean isSpellId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
