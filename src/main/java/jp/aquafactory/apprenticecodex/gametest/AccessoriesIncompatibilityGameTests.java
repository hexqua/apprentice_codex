package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AccessoriesIncompatibilityGameTests {
    private AccessoriesIncompatibilityGameTests() {
    }

    // 将来の上流修正を検知する任意テスト。既知の非互換が残る間の失敗は必須検証を止めない。
    @GameTest(template = "gametest/basic_floor", required = false, timeoutTicks = 40)
    public static void accessoryCaseMenuTracksSlotChangesIfUpstreamFixesCompatibility(GameTestHelper helper) throws ReflectiveOperationException {
        AccessoriesIncompatibilityGameTestScenarios.caseMenuTracksSlotChanges(helper);
    }
}
