package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

public final class CraftsmansDelightServerConfig {
    private final ForgeConfigSpec.BooleanValue canImbueEnchantment;
    private final ForgeConfigSpec.DoubleValue requiredMana;
    private final ForgeConfigSpec.IntValue fortuneLevel;
    private final ForgeConfigSpec.DoubleValue cooldownMultiplier;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> gracedRainGrowthDenylist;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> gracedRainBreedingCooldownDenylist;
    private List<String> gracedRainGrowthDenylistOverride;
    private List<String> gracedRainBreedingCooldownDenylistOverride;
    private Double cooldownMultiplierOverride;

    private CraftsmansDelightServerConfig(
            ForgeConfigSpec.BooleanValue canImbueEnchantment,
            ForgeConfigSpec.DoubleValue requiredMana,
            ForgeConfigSpec.IntValue fortuneLevel,
            ForgeConfigSpec.DoubleValue cooldownMultiplier,
            ForgeConfigSpec.ConfigValue<List<? extends String>> gracedRainGrowthDenylist,
            ForgeConfigSpec.ConfigValue<List<? extends String>> gracedRainBreedingCooldownDenylist
    ) {
        this.canImbueEnchantment = canImbueEnchantment;
        this.requiredMana = requiredMana;
        this.fortuneLevel = fortuneLevel;
        this.cooldownMultiplier = cooldownMultiplier;
        this.gracedRainGrowthDenylist = gracedRainGrowthDenylist;
        this.gracedRainBreedingCooldownDenylist = gracedRainBreedingCooldownDenylist;
    }

    public static CraftsmansDelightServerConfig define(ForgeConfigSpec.Builder builder) {
        builder.push("CraftsmansDelight");

        var canImbueEnchantment = builder.define("canImbueEnchantment", true);
        var requiredMana = builder.defineInRange("requiredMana", 500.0d, 0.0d, 10000.0d);
        var fortuneLevel = builder.defineInRange("fortuneLevel", 3, 1, 10);
        var cooldownMultiplier = builder
                .comment("Multiplier applied to target spell cooldowns. 0.5 = 50%. A value of 0.0 still leaves a minimum cooldown of 1 tick.")
                .defineInRange("cooldownMultiplier", 0.5D, 0.0D, 1.0D);
        var gracedRainGrowthDenylist = builder
                .comment("Entity type IDs blocked from Craftsman's Delight Graced Rain baby growth acceleration. Entries use \"modid:path\".")
                .defineListAllowEmpty("gracedRainGrowthDenylist", List.<String>of(), CraftsmansDelightServerConfig::isEntityTypeId);
        var gracedRainBreedingCooldownDenylist = builder
                .comment("Entity type IDs blocked from Craftsman's Delight Graced Rain breeding cooldown reduction. Entries use \"modid:path\".")
                .defineListAllowEmpty("gracedRainBreedingCooldownDenylist", List.<String>of(), CraftsmansDelightServerConfig::isEntityTypeId);

        builder.pop();
        return new CraftsmansDelightServerConfig(
                canImbueEnchantment,
                requiredMana,
                fortuneLevel,
                cooldownMultiplier,
                gracedRainGrowthDenylist,
                gracedRainBreedingCooldownDenylist
        );
    }

    public boolean canImbueEnchantment() {
        return canImbueEnchantment.get();
    }

    public float requiredMana() {
        return requiredMana.get().floatValue();
    }

    public int fortuneLevel() {
        return fortuneLevel.get();
    }

    public double cooldownMultiplier() {
        return cooldownMultiplierOverride == null ? cooldownMultiplier.get() : cooldownMultiplierOverride;
    }

    public boolean isGracedRainGrowthDenied(ResourceLocation entityTypeId) {
        return containsEntityTypeId(gracedRainGrowthDenylist(), entityTypeId);
    }

    public boolean isGracedRainBreedingCooldownDenied(ResourceLocation entityTypeId) {
        return containsEntityTypeId(gracedRainBreedingCooldownDenylist(), entityTypeId);
    }

    public List<String> gracedRainGrowthDenylist() {
        return Objects.requireNonNullElseGet(gracedRainGrowthDenylistOverride,
                () -> stringList(gracedRainGrowthDenylist));
    }

    public List<String> gracedRainBreedingCooldownDenylist() {
        return Objects.requireNonNullElseGet(gracedRainBreedingCooldownDenylistOverride,
                () -> stringList(gracedRainBreedingCooldownDenylist));
    }

    public void setGracedRainDenylistsForGameTest(
            List<String> gracedRainGrowthDenylist,
            List<String> gracedRainBreedingCooldownDenylist
    ) {
        this.gracedRainGrowthDenylistOverride = List.copyOf(gracedRainGrowthDenylist);
        this.gracedRainBreedingCooldownDenylistOverride = List.copyOf(gracedRainBreedingCooldownDenylist);
    }

    public void setCooldownMultiplierForGameTest(double value) {
        cooldownMultiplierOverride = value;
    }

    private static boolean containsEntityTypeId(List<String> configuredIds, ResourceLocation entityTypeId) {
        if (entityTypeId == null) {
            return false;
        }
        for (var configuredId : configuredIds) {
            if (entityTypeId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> stringList(ForgeConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }

    private static boolean isEntityTypeId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
