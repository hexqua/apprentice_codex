package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class CraftsmansDelightServerConfig {
    private final ModConfigSpec.BooleanValue canImbueEnchantment;
    private final ModConfigSpec.DoubleValue requiredMana;
    private final ModConfigSpec.IntValue fortuneLevel;
    private final ModConfigSpec.ConfigValue<List<? extends String>> gracedRainGrowthDenylist;
    private final ModConfigSpec.ConfigValue<List<? extends String>> gracedRainBreedingCooldownDenylist;
    private List<String> gracedRainGrowthDenylistOverride;
    private List<String> gracedRainBreedingCooldownDenylistOverride;

    private CraftsmansDelightServerConfig(
            ModConfigSpec.BooleanValue canImbueEnchantment,
            ModConfigSpec.DoubleValue requiredMana,
            ModConfigSpec.IntValue fortuneLevel,
            ModConfigSpec.ConfigValue<List<? extends String>> gracedRainGrowthDenylist,
            ModConfigSpec.ConfigValue<List<? extends String>> gracedRainBreedingCooldownDenylist
    ) {
        this.canImbueEnchantment = canImbueEnchantment;
        this.requiredMana = requiredMana;
        this.fortuneLevel = fortuneLevel;
        this.gracedRainGrowthDenylist = gracedRainGrowthDenylist;
        this.gracedRainBreedingCooldownDenylist = gracedRainBreedingCooldownDenylist;
    }

    public static CraftsmansDelightServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("CraftsmansDelight");

        var canImbueEnchantment = builder.define("canImbueEnchantment", true);
        var requiredMana = builder.defineInRange("requiredMana", 500.0d, 0.0d, 10000.0d);
        var fortuneLevel = builder.defineInRange("fortuneLevel", 3, 1, 10);
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

    private static List<String> stringList(ModConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return configValue.get().stream()
                .map(String::valueOf)
                .toList();
    }

    private static boolean isEntityTypeId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}

