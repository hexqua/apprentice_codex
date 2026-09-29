package jp.aquafactory.apprenticecodex.compat.patchouli;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.PatchouliAPI;

import java.io.InputStream;
import java.util.Objects;

public final class PatchouliBuiltinTemplateSupport {
    public static final ResourceLocation ESSENCE_SMOKER_RECIPE_TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "essence_smoker_recipe");
    public static final ResourceLocation SPELLCASTER_WORKBENCH_RECIPE_TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellcaster_workbench_recipe");
    private static final String ESSENCE_SMOKER_TEMPLATE_RESOURCE =
            "/assets/apprenticecodex/patchouli_builtin_templates/essence_smoker_recipe.json";
    private static final String SPELLCASTER_WORKBENCH_TEMPLATE_RESOURCE =
            "/assets/apprenticecodex/patchouli_builtin_templates/spellcaster_workbench_recipe.json";
    private static boolean builtinTemplatesRegistered;

    private PatchouliBuiltinTemplateSupport() {
    }

    public static synchronized void registerBuiltinTemplates() {
        if (builtinTemplatesRegistered) {
            return;
        }

        // manual 側のJSONから直接参照できるよう、template本体はmod側でbuiltin登録する。
        PatchouliAPI.get().registerTemplateAsBuiltin(
                ESSENCE_SMOKER_RECIPE_TEMPLATE_ID,
                PatchouliBuiltinTemplateSupport::openEssenceSmokerTemplate
        );
        PatchouliAPI.get().registerTemplateAsBuiltin(
                SPELLCASTER_WORKBENCH_RECIPE_TEMPLATE_ID,
                PatchouliBuiltinTemplateSupport::openSpellcasterWorkbenchTemplate
        );
        builtinTemplatesRegistered = true;
    }

    private static InputStream openEssenceSmokerTemplate() {
        return Objects.requireNonNull(
                PatchouliBuiltinTemplateSupport.class.getResourceAsStream(ESSENCE_SMOKER_TEMPLATE_RESOURCE),
                "Missing Patchouli builtin template: " + ESSENCE_SMOKER_TEMPLATE_RESOURCE
        );
    }

    private static InputStream openSpellcasterWorkbenchTemplate() {
        return Objects.requireNonNull(
                PatchouliBuiltinTemplateSupport.class.getResourceAsStream(SPELLCASTER_WORKBENCH_TEMPLATE_RESOURCE),
                "Missing Patchouli builtin template: " + SPELLCASTER_WORKBENCH_TEMPLATE_RESOURCE
        );
    }
}
