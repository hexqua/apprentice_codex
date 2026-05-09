package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION;
    private static final ModConfigSpec.BooleanValue ENABLE_SPELLGUN_AMMO_HUD;
    private static final ModConfigSpec.BooleanValue ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING;
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

    public static boolean disableEssenceSmokerParticleTextureAnalysis() {
        return DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS.get();
    }
}
