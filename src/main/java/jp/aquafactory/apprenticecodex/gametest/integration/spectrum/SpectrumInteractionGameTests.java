package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpectrumInteractionGameTests {
    private SpectrumInteractionGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigRecoversSpawnerOnlyWithResonance(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.touchDig(helper, true);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void worldFlatterTransfersResonanceOnlyWithCraftsmansDelight(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.worldFlatter(helper, true);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void psychicDriveGauntletsRecoverSpawnerWithStoredResonanceTool(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.gauntlets(helper, true);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void tinyLumberjackDropsFollowColoredTreeRevelationAndSilkTouch(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.coloredTrees(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void exuberanceAndWisdomComposeOnActualBlockExperience(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumExperienceScenarios.blockExperience(helper);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void exuberanceAndWisdomComposeOnActualMobExperience(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumExperienceScenarios.mobExperience(helper);
            helper.succeed();
        });
    }
}
