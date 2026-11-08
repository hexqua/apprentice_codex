package jp.aquafactory.apprenticecodex.gametest.crafting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.PROCESSING_RECIPE_DENYLIST_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        ComfortBerriesScenarios.comfortBerriesCanBePottedAsDecoration(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesHaveAppleTierCompostChance(GameTestHelper helper) {
        ComfortBerriesScenarios.comfortBerriesHaveAppleTierCompostChance(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void customRecipeDataIsLoaded(GameTestHelper helper) {
        RecipeLoadingScenarios.customRecipeDataIsLoaded(helper);
    }

    @GameTest(template = TEMPLATE, batch = PROCESSING_RECIPE_DENYLIST_CONFIG_BATCH)
    public static void processingRecipeDenylistsRejectConfiguredRecipeIds(GameTestHelper helper) {
        ProcessingRecipeScenarios.processingRecipeDenylistsRejectConfiguredRecipeIds(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void arcanumInAJarComparatorOutputMatchesStoredEssence(GameTestHelper helper) {
        ComparatorScenarios.arcanumInAJarComparatorOutputMatchesStoredEssence(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void atelierStationComparatorOutputMatchesStoredPotionFluidAmount(GameTestHelper helper) {
        ComparatorScenarios.atelierStationComparatorOutputMatchesStoredPotionFluidAmount(helper);
    }
}
