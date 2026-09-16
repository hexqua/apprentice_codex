package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class GemsMaterialGameTests {
    private GemsMaterialGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gemsCooldownActionScalesAndExpiresSpells(GameTestHelper helper) {
        if (ModList.get().isLoaded("irons_jewelry")) {
            GemsCooldownActionGameTestScenarios.verifyScalingAndExpiry(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gemsCooldownActionRespectsTargetsAndNoOps(GameTestHelper helper) {
        if (ModList.get().isLoaded("irons_jewelry")) {
            GemsCooldownActionGameTestScenarios.verifyTargetsAndNoOps(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gemsCooldownActionUsesMaterialAndAttackTrigger(GameTestHelper helper) {
        if (ModList.get().isLoaded("irons_jewelry")) {
            GemsCooldownActionGameTestScenarios.verifyAttackTrigger(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void generatedJewelryMaterialsRetainTheirDefinitions(GameTestHelper helper) {
        // optional MODなしの通常GameTestでは、外部型を含むシナリオをロードしない。
        if (ModList.get().isLoaded("irons_jewelry")) {
            GemsMaterialGameTestScenarios.verifyLoadedMaterials(helper);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void jewelryMaterialBuilderSupportsRegisteredParametersAndActions(GameTestHelper helper) {
        if (ModList.get().isLoaded("irons_jewelry")) {
            GemsMaterialGameTestScenarios.verifyCodecCoverage(helper);
        }
        helper.succeed();
    }
}
