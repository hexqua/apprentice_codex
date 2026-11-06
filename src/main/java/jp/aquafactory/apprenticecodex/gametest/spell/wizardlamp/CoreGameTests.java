package jp.aquafactory.apprenticecodex.gametest.spell.wizardlamp;

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
    public static void wizardlampUsesClientCellAndClampsItWithoutLineOfSight(GameTestHelper helper) {
        WizardlampScenarios.wizardlampUsesClientCellAndClampsItWithoutLineOfSight(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void blockPlacementSpellsRespectForgePlaceEvent(GameTestHelper helper) {
        WizardlampScenarios.blockPlacementSpellsRespectForgePlaceEvent(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void wizardlampLanternHasLanternCollisionAndNoDrops(GameTestHelper helper) {
        WizardlampScenarios.wizardlampLanternHasLanternCollisionAndNoDrops(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void wizardlampSupportsWaterloggingWhileMageLightRemainsDry(GameTestHelper helper) {
        WizardlampScenarios.wizardlampSupportsWaterloggingWhileMageLightRemainsDry(helper);
    }
}
