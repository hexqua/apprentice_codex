package jp.aquafactory.apprenticecodex.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ApprenticeCodexClientConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION;
    private static final ModConfigSpec.BooleanValue ENABLE_SPELLGUN_AMMO_HUD;
    private static final ModConfigSpec.BooleanValue ENABLE_MANA_FORCE_BLADE_HOTBAR_SHEATH_RENDERING;
    private static final ModConfigSpec.BooleanValue ENABLE_SMASHCAST_SCEPTER_TREMOR_BLOCK_RENDERING;
    private static final ModConfigSpec.BooleanValue DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("Items");
        ENABLE_APPRENTICE_MAGE_ROBE_CAPE_ANIMATION = builder
                .comment("隕狗ｿ偵＞鬲碑｡灘ｸｫ縺ｮ繝ｭ繝ｼ繝悶・繧ｱ繝ｼ繝励い繝九Γ繝ｼ繧ｷ繝ｧ繝ｳ繧呈怏蜉ｹ蛹悶☆繧・)
                .define("enableApprenticeMageRobeCapeAnimation", true);
        ENABLE_SPELLGUN_AMMO_HUD = builder
                .comment("繧ｹ繝壹Ν繧ｬ繝ｳ縺ｮ谿句ｼｾ HUD 繧偵け繝ｭ繧ｹ繝倥い莉倩ｿ代↓陦ｨ遉ｺ縺吶ｋ")
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
        builder.pop();

        builder.push("Blocks");
        DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS = builder
                .comment("繧ｨ繝・そ繝ｳ繧ｹ辯ｻ陬ｽ蜿ｰ縺ｮ繝代・繝・ぅ繧ｯ繝ｫ濶ｲ隗｣譫舌ｒ辟｡蜉ｹ蛹悶＠縲∝ｸｸ縺ｫ莉｣譖ｿ濶ｲ繧剃ｽｿ縺・)
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

    public static boolean disableEssenceSmokerParticleTextureAnalysis() {
        return DISABLE_ESSENCE_SMOKER_PARTICLE_TEXTURE_ANALYSIS.get();
    }
}
