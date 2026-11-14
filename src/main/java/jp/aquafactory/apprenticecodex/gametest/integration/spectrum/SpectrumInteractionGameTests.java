package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpectrumInteractionGameTests {
    private SpectrumInteractionGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigRecoversSpawnerOnlyWithResonance(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.touchDig(helper, true);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void worldFlatterTransfersResonanceOnlyWithCraftsmansDelight(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.worldFlatter(helper, true);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void psychicDriveGauntletsRecoverSpawnerWithStoredResonanceTool(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.gauntlets(helper, true);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void tinyLumberjackDropsFollowColoredTreeRevelationAndSilkTouch(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) SpectrumMiningScenarios.coloredTrees(helper);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void exuberanceAndWisdomComposeOnActualBlockExperience(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) SpectrumExperienceScenarios.blockExperience(helper);
        helper.succeed();
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void exuberanceAndWisdomComposeOnActualMobExperience(GameTestHelper helper) {
        if (ModList.get().isLoaded("spectrum")) SpectrumExperienceScenarios.mobExperience(helper);
        helper.succeed();
    }
}
