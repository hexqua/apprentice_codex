package jp.aquafactory.apprenticecodex.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Locale;

public final class ApprenticeCodexClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SPELLGUN_AMMO_HUD;
    private static final ForgeConfigSpec.BooleanValue ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SMASHCAST_SCEPTER_TREMOR_BLOCK_RENDERING;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_CATEGORIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_ITEMS;
    private static final ForgeConfigSpec.BooleanValue DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("Items");
        ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION = builder
                .comment("Enable cape animation for the Apprentice Mage Robe.")
                .define("enableApprenticeMageRobeCapeAnimation", true);
        ENABLE_SPELLGUN_AMMO_HUD = builder
                .comment("Show Spellgun ammo HUD near the crosshair.")
                .define("enableSpellgunAmmoHud", true);
        ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING = builder
                .comment(
                        "Render the Mana Force Blade sheath on your hip when the blade is in your hotbar.",
                        "Always disabled in Epic Fight environments."
                )
                .define("enableManaForceBladeHotbarSheathRendering", true);
        ENABLE_SMASHCAST_SCEPTER_TREMOR_BLOCK_RENDERING = builder
                .comment("Render temporary block tremors when Smashcast Scepter smash attacks hit.")
                .define("enableSmashcastScepterTremorBlockRendering", true);
        SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_CATEGORIES = builder
                .comment(
                        "Epic Fight weapon categories that suppress the Scrollcaster Gauntlet's extra offhand visual rendering.",
                        "This affects only the client-side visual workaround and does not change combat behavior.",
                        "Examples: UCHIGATANA, TACHI, GREATSWORD."
                )
                .defineList("scrollcasterGauntletOffhandVisualDisabledMainhandCategories",
                        List.of("UCHIGATANA"),
                        value -> value instanceof String text && !text.isBlank());
        SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_ITEMS = builder
                .comment(
                        "Main-hand item IDs that suppress the Scrollcaster Gauntlet's extra offhand visual rendering.",
                        "Use this for weapons that share a broad Epic Fight category with unrelated items.",
                        "Default entries cover vanilla crossbow, Iron's Autoloader Crossbow, and Multipurpose Staffrifle."
                )
                .defineList("scrollcasterGauntletOffhandVisualDisabledMainhandItems",
                        List.of(
                                "minecraft:crossbow",
                                "irons_spellbooks:autoloader_crossbow",
                                "apprenticecodex:multipurpose_staffrifle"
                        ),
                        value -> value instanceof String text && ResourceLocation.tryParse(text) != null);
        builder.pop();

        builder.push("Blocks");
        DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS = builder
                .comment("Disable Essence Smoker particle texture analysis and always use fallback colors.")
                .define("disableEssenceSmokerParticleTextureAnalysis", false);
        builder.pop();

        SPEC = builder.build();
    }

    private ApprenticeCodexClientConfig() {
    }

    public static boolean enableApprenticeMageRobeCapeAnimation() {
        return ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION.get();
    }

    public static boolean enableSpellgunAmmoHud() {
        return ENABLE_SPELLGUN_AMMO_HUD.get();
    }

    public static boolean enableManaForceBladeHotbarSheathRendering() {
        return ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING.get();
    }

    public static boolean enableSmashcastScepterTremorBlockRendering() {
        return ENABLE_SMASHCAST_SCEPTER_TREMOR_BLOCK_RENDERING.get();
    }

    public static boolean isScrollcasterGauntletOffhandVisualDisabledForMainhandCategory(String categoryName) {
        var normalizedCategoryName = normalizeEpicFightWeaponCategory(categoryName);
        return SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_CATEGORIES.get().stream()
                .map(String::valueOf)
                .map(ApprenticeCodexClientConfig::normalizeEpicFightWeaponCategory)
                .anyMatch(normalizedCategoryName::equals);
    }

    public static boolean isScrollcasterGauntletOffhandVisualDisabledForMainhandItem(String itemId) {
        var normalizedItemId = normalizeResourceLocation(itemId);
        return SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_ITEMS.get().stream()
                .map(String::valueOf)
                .map(ApprenticeCodexClientConfig::normalizeResourceLocation)
                .anyMatch(normalizedItemId::equals);
    }

    public static boolean disableEssenceSmokerParticleTextureAnalysis() {
        return DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS.get();
    }

    private static String normalizeEpicFightWeaponCategory(String categoryName) {
        return categoryName.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeResourceLocation(String itemId) {
        var resourceLocation = ResourceLocation.tryParse(itemId.trim());
        return resourceLocation == null
                ? itemId.trim().toLowerCase(Locale.ROOT)
                : resourceLocation.toString();
    }
}
