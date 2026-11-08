package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.echo.EchoWeaponTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoWeaponAttackGameTests {
    private EchoWeaponAttackGameTests() {}

    // 全対応武器で「生成しただけ」「タイムアウトで消えただけ」を攻撃不成立として検出する。
    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void everySupportedWeaponAttacksAndExpires(GameTestHelper helper) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> verifySupportedWeaponAttacks(helper));
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 100)
    public static void rifleEchoTracksSupportedSummonWeapons(GameTestHelper helper) {
        // UUIDによる即時取得は、1.20.1のentity section公開後に検証する。
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> verifyRifleSummonWeapons(helper));
    }
}
