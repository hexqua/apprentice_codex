package jp.aquafactory.apprenticecodex.compat.patchouli;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.PatchouliAPI;

import java.io.InputStream;
import java.util.Objects;

public final class PatchouliBuiltinTemplateSupport {
    public static final ResourceLocation ESSENCE_SMOKER_RECIPE_TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker_recipe");
    private static final String ESSENCE_SMOKER_TEMPLATE_RESOURCE =
            "/assets/apprenticecodex/patchouli_builtin_templates/essence_smoker_recipe.json";

    private PatchouliBuiltinTemplateSupport() {
    }

    public static void registerBuiltinTemplates() {
        // manual 側のJSONから直接参照できるよう、template本体はmod側でbuiltin登録する。
        PatchouliAPI.get().registerTemplateAsBuiltin(
                ESSENCE_SMOKER_RECIPE_TEMPLATE_ID,
                PatchouliBuiltinTemplateSupport::openEssenceSmokerTemplate
        );
    }

    private static InputStream openEssenceSmokerTemplate() {
        return Objects.requireNonNull(
                PatchouliBuiltinTemplateSupport.class.getResourceAsStream(ESSENCE_SMOKER_TEMPLATE_RESOURCE),
                "Missing Patchouli builtin template: " + ESSENCE_SMOKER_TEMPLATE_RESOURCE
        );
    }
}
