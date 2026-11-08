package jp.aquafactory.apprenticecodex.gametest.spell.wizardlamp;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void wizardlampUsesClientCellAndClampsItWithoutLineOfSight(GameTestHelper helper) {
        WizardlampScenarios.wizardlampUsesClientCellAndClampsItWithoutLineOfSight(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void blockPlacementSpellsRespectForgePlaceEvent(GameTestHelper helper) {
        WizardlampScenarios.blockPlacementSpellsRespectForgePlaceEvent(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void wizardlampLanternHasLanternCollisionAndNoDrops(GameTestHelper helper) {
        WizardlampScenarios.wizardlampLanternHasLanternCollisionAndNoDrops(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void wizardlampSupportsWaterloggingWhileMageLightRemainsDry(GameTestHelper helper) {
        WizardlampScenarios.wizardlampSupportsWaterloggingWhileMageLightRemainsDry(helper);
    }
}
