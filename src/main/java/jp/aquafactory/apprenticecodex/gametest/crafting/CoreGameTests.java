package jp.aquafactory.apprenticecodex.gametest.crafting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        ComfortBerriesScenarios.comfortBerriesCanBePottedAsDecoration(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void comfortBerriesHaveAppleTierCompostChance(GameTestHelper helper) {
        ComfortBerriesScenarios.comfortBerriesHaveAppleTierCompostChance(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void customRecipeDataIsLoaded(GameTestHelper helper) {
        RecipeLoadingScenarios.customRecipeDataIsLoaded(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "processing_recipe_denylist_config")
    public static void processingRecipeDenylistsRejectConfiguredRecipeIds(GameTestHelper helper) {
        ProcessingRecipeScenarios.processingRecipeDenylistsRejectConfiguredRecipeIds(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void arcanumInAJarComparatorOutputMatchesStoredEssence(GameTestHelper helper) {
        ComparatorScenarios.arcanumInAJarComparatorOutputMatchesStoredEssence(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void atelierStationComparatorOutputMatchesStoredPotionFluidAmount(GameTestHelper helper) {
        ComparatorScenarios.atelierStationComparatorOutputMatchesStoredPotionFluidAmount(helper);
    }
}
