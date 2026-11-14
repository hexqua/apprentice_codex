package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpectrumEnchantmentGameTests {
    private SpectrumEnchantmentGameTests() {
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spectrum_recipe_reload")
    public static void spectrumRecipesRemainSafeAfterReload(GameTestHelper helper) {
        SpectrumEnchantmentScenarios.reload(helper, ModList.get().isLoaded("spectrum"));
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spectrumBookRecipesMatchMaterialsAndProduceEnchantedBooks(GameTestHelper helper) {
        // 通常構成ではSpectrum型を持つシナリオをロードせず、条件付きJSONの不在を検証する。
        if (ModList.get().isLoaded("spectrum")) {
            SpectrumEnchantmentScenarios.books(helper);
        } else {
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream()
                            .noneMatch(recipe -> recipe.getId().getNamespace().equals(ApprenticeCodex.MODID)
                                    && recipe.getId().getPath().startsWith("spectrum/")),
                    "Spectrum recipes must not load without Spectrum");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spectrumUpgradesConsumeExperienceAndRespectResourceBoundaries(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) {
            SpectrumEnchantmentScenarios.upgrades(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spectrumOverenchantingRequiresUnlockAndStopsAtRecipeCap(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) {
            SpectrumEnchantmentScenarios.overenchanting(helper);
        }
        helper.succeed();
    }
}
