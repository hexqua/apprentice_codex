package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellCastParryingRingMalumGameTests {
    private SpellCastParryingRingMalumGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void staffWindowsAndCancellation(GameTestHelper helper) {
        // optional 型は登録時に解決せず、Malum 導入時だけテスト本体を読み込む。
        if (MalumStaffChargeBridge.isAvailable()) SpellCastParryingRingMalumScenarios.windows(helper);
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void staffDefenseBoundaries(GameTestHelper helper) {
        if (MalumStaffChargeBridge.isAvailable()) SpellCastParryingRingMalumScenarios.defense(helper);
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void staffCooldownRejectionAndRelease(GameTestHelper helper) {
        if (MalumStaffChargeBridge.isAvailable()) SpellCastParryingRingMalumScenarios.release(helper);
        helper.succeed();
    }
}
