package jp.aquafactory.apprenticecodex.config.item;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

public final class ScrollcasterGauntletServerConfig {
    private final ModConfigSpec.ConfigValue<List<? extends String>> deniedEnchantments;
    private final ModConfigSpec.ConfigValue<List<? extends String>> compatAdditionalAllowedEnchantments;

    private List<String> deniedEnchantmentsOverride;
    private List<String> compatAdditionalAllowedEnchantmentsOverride;

    private ScrollcasterGauntletServerConfig(
            ModConfigSpec.ConfigValue<List<? extends String>> deniedEnchantments,
            ModConfigSpec.ConfigValue<List<? extends String>> compatAdditionalAllowedEnchantments
    ) {
        this.deniedEnchantments = deniedEnchantments;
        this.compatAdditionalAllowedEnchantments = compatAdditionalAllowedEnchantments;
    }

    public static ScrollcasterGauntletServerConfig define(ModConfigSpec.Builder builder) {
        builder.push("ScrollcasterGauntlet");
        var deniedEnchantments = builder
                .comment("Enchantment IDs that cannot be copied to Scrollcaster Gauntlets through Spell Calibration Bench books. This is a final safety gate for books that already exist outside normal enchantment-control paths.")
                .defineListAllowEmpty("deniedEnchantments", List.<String>of(), ScrollcasterGauntletServerConfig::isEnchantmentId);
        var compatAdditionalAllowedEnchantments = builder
                .comment("Compatibility escape hatch for enchantment IDs that are not normally supported but have been verified by the modpack. No special behavior is added; effects and stability depend on each enchantment implementation.")
                .defineListAllowEmpty("compatAdditionalAllowedEnchantments", List.<String>of(), ScrollcasterGauntletServerConfig::isEnchantmentId);
        builder.pop();

        return new ScrollcasterGauntletServerConfig(deniedEnchantments, compatAdditionalAllowedEnchantments);
    }

    public boolean isEnchantmentDenied(ResourceLocation enchantmentId) {
        return enchantmentId != null && containsEnchantmentId(deniedEnchantments(), enchantmentId);
    }

    public boolean isCompatAdditionalAllowedEnchantment(ResourceLocation enchantmentId) {
        return enchantmentId != null && containsEnchantmentId(compatAdditionalAllowedEnchantments(), enchantmentId);
    }

    public List<String> deniedEnchantments() {
        return Objects.requireNonNullElseGet(deniedEnchantmentsOverride, () -> deniedEnchantments.get().stream()
                .map(String::valueOf)
                .toList());
    }

    public List<String> compatAdditionalAllowedEnchantments() {
        return Objects.requireNonNullElseGet(
                compatAdditionalAllowedEnchantmentsOverride,
                () -> compatAdditionalAllowedEnchantments.get().stream()
                        .map(String::valueOf)
                        .toList()
        );
    }

    public void setForGameTest(List<String> deniedEnchantments, List<String> compatAdditionalAllowedEnchantments) {
        this.deniedEnchantmentsOverride = List.copyOf(deniedEnchantments);
        this.compatAdditionalAllowedEnchantmentsOverride = List.copyOf(compatAdditionalAllowedEnchantments);
    }

    private static boolean containsEnchantmentId(List<String> configuredIds, ResourceLocation enchantmentId) {
        for (var configuredId : configuredIds) {
            if (enchantmentId.equals(ResourceLocation.tryParse(String.valueOf(configuredId)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnchantmentId(Object value) {
        return value instanceof String text && text.contains(":") && ResourceLocation.tryParse(text) != null;
    }
}
