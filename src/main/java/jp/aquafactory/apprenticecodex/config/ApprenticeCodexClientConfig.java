package jp.aquafactory.apprenticecodex.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

public final class ApprenticeCodexClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION;
    private static final ModConfigSpec.BooleanValue ENABLE_SPELLGUN_AMMO_HUD;
    private static final ModConfigSpec.BooleanValue ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING;
    private static final ModConfigSpec.BooleanValue ENABLE_SMASHCAST_SCEPTER_TREMOR_BLOCK_RENDERING;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_CATEGORIES;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            SCROLLCASTER_GAUNTLET_OFFHAND_VISUAL_DISABLED_MAINHAND_ITEMS;
    private static final ModConfigSpec.BooleanValue ENABLE_BETTER_COMBAT_SCROLLCASTER_GAUNTLET_FIRST_PERSON_OFFHAND_VISUAL;
    private static final ModConfigSpec.BooleanValue ENABLE_BETTER_COMBAT_SCROLLCASTER_GAUNTLET_THIRD_PERSON_OFFHAND_VISUAL;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            BETTER_COMBAT_SCROLLCASTER_GAUNTLET_THIRD_PERSON_OFFHAND_VISUAL_DENIED_MAINHAND_ITEMS;
    private static final ModConfigSpec.BooleanValue DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("Items");
        ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION = builder
                .comment("見習い魔術師のローブのケープアニメーションを有効化する")
                .define("enableApprenticeMageRobeCapeAnimation", true);
        ENABLE_SPELLGUN_AMMO_HUD = builder
                .comment("スペルガンの残弾 HUD をクロスヘア付近に表示する")
                .define("enableSpellgunAmmoHud", true);
        ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING = builder
                .comment(
                        "Render the Mana Force Blade sheath on your hip when the blade is in your hotbar.",
                        "Always disabled in Epic Fight environments."
                )
                .define("enableManaForceBladeHotbarSheathRendering", true);
        ENABLE_SMASHCAST_SCEPTER_TREMOR_BLOCK_RENDERING = builder
                .comment("スマッシュキャストの錫杖のスマッシュ攻撃命中時に一時的なブロック揺れを描画する")
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
        ENABLE_BETTER_COMBAT_SCROLLCASTER_GAUNTLET_FIRST_PERSON_OFFHAND_VISUAL = builder
                .comment(
                        "Render the offhand Scrollcaster Gauntlet in first person while casting through Better Combat's two-handed offhand hiding.",
                        "This controls only the normal first-person hand renderer used for casting, not Better Combat attack animations.",
                        "This affects only the client-side visual workaround and does not change combat behavior."
                )
                .define("enableBetterCombatScrollcasterGauntletFirstPersonOffhandVisual", true);
        ENABLE_BETTER_COMBAT_SCROLLCASTER_GAUNTLET_THIRD_PERSON_OFFHAND_VISUAL = builder
                .comment(
                        "Render the offhand Scrollcaster Gauntlet in third person while Better Combat hides offhand equipment for two-handed weapons.",
                        "Better Combat first-person attack animations can use the third-person player model, so this may also affect first-person attack animations.",
                        "This affects only the client-side visual workaround and does not change combat behavior."
                )
                .define("enableBetterCombatScrollcasterGauntletThirdPersonOffhandVisual", true);
        BETTER_COMBAT_SCROLLCASTER_GAUNTLET_THIRD_PERSON_OFFHAND_VISUAL_DENIED_MAINHAND_ITEMS = builder
                .comment(
                        "Main-hand item IDs that suppress Better Combat's third-person Scrollcaster Gauntlet offhand visual workaround.",
                        "This denylist also applies when Better Combat renders first-person attack animations through the third-person player model.",
                        "Use this only for specific weapons whose model visually conflicts with the gauntlet."
                )
                .defineListAllowEmpty("betterCombatScrollcasterGauntletThirdPersonOffhandVisualDeniedMainhandItems",
                        List.of(),
                        value -> value instanceof String text && ResourceLocation.tryParse(text) != null);
        builder.pop();

        builder.push("Blocks");
        DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS = builder
                .comment("エッセンス燻製台のパーティクル色解析を無効化し、常に代替色を使う")
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

    public static boolean enableBetterCombatScrollcasterGauntletFirstPersonOffhandVisual() {
        return ENABLE_BETTER_COMBAT_SCROLLCASTER_GAUNTLET_FIRST_PERSON_OFFHAND_VISUAL.get();
    }

    public static boolean enableBetterCombatScrollcasterGauntletThirdPersonOffhandVisual() {
        return ENABLE_BETTER_COMBAT_SCROLLCASTER_GAUNTLET_THIRD_PERSON_OFFHAND_VISUAL.get();
    }

    public static boolean isBetterCombatScrollcasterGauntletThirdPersonOffhandVisualDeniedForMainhandItem(String itemId) {
        var normalizedItemId = normalizeResourceLocation(itemId);
        return BETTER_COMBAT_SCROLLCASTER_GAUNTLET_THIRD_PERSON_OFFHAND_VISUAL_DENIED_MAINHAND_ITEMS.get().stream()
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
