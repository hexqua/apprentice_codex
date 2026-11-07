package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.support;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class SummonWeaponTestSupport {
    private SummonWeaponTestSupport() {
    }

    public static <T extends FakePlayer> T registerPlayer(GameTestHelper helper, T player) {
        trackPlayer(helper, player);
        return TargetingTestFixture.of(helper).add(player);
    }

    public static <T extends FakePlayer> T trackPlayer(GameTestHelper helper, T player) {
        var scope = TargetingTestFixture.of(helper);
        scope.track(player);
        scope.assertInside(player);
        // 成功・途中失敗・timeoutで、生成物を破棄する前に所有者のrecastを終了させる。
        scope.onClose(() -> MagicData.getPlayerMagicData(player).getPlayerRecasts().removeAll(RecastResult.COMMAND));
        return player;
    }
}
