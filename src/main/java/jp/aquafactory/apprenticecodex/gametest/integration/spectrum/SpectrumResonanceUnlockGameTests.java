package jp.aquafactory.apprenticecodex.gametest.integration.spectrum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

// Spectrum 1.8.15は使用進捗を判定するため、未解禁時の回収拒否も必須で検証する。
@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpectrumResonanceUnlockGameTests {
    private SpectrumResonanceUnlockGameTests() {}

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void touchDigMustNotRecoverSpawnerBeforeResonanceUnlock(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.touchDig(helper, false);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void worldFlatterMustNotRecoverSpawnerBeforeResonanceUnlock(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.worldFlatter(helper, false);
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void psychicDriveGauntletsMustNotRecoverSpawnerBeforeResonanceUnlock(GameTestHelper helper) {
        if (!ModList.get().isLoaded("spectrum")) {
            helper.succeed();
            return;
        }
        // 1.20.1は構造配置直後のentity公開を保証しないため、操作前に対象chunkを待つ。
        GameTestFixtureSupport.whenEntityChunksReady(helper, BlockPos.ZERO, new BlockPos(8, 0, 8), () -> {
            SpectrumMiningScenarios.gauntlets(helper, false);
            helper.succeed();
        });
    }

}
