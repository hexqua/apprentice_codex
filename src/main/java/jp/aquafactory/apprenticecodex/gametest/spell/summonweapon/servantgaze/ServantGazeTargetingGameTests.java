package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.TARGETING_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.activate;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeLifecycleGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ServantGazeTargetingGameTests {
    private ServantGazeTargetingGameTests() {}

    @GameTest(template = TEMPLATE, batch = TARGETING_ISOLATED_BATCH, timeoutTicks = 100)
    public static void servantGazeConsumesManaPerHighestHealthTarget(GameTestHelper helper) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> {
            var player = createPlayer(helper, "servant_gaze_targeting");
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(20.0F);
            var low = createZombie(helper, new BlockPos(0, 2, 4), 5.0F);
            var middle = createZombie(helper, new BlockPos(3, 2, 1), 10.0F);
            var high = createZombie(helper, new BlockPos(4, 2, 3), 15.0F);
            activate(helper, player, 100, 1, 2.0F, 4.0, 10);

            // 対象選定後の飛翔体到達は周辺 test の負荷で前後するため、固定 tick ではなく結果成立を待つ。
            helper.succeedWhen(() -> {
                helper.assertTrue(magicData.getMana() == 0.0F,
                        "Servant Gaze should consume one mana payment for each selected target");
                helper.assertTrue(low.getHealth() == 5.0F,
                        "Servant Gaze should leave the lower-health third target unselected when mana is insufficient");
                helper.assertTrue(middle.getHealth() < 10.0F && high.getHealth() < 15.0F,
                        "Servant Gaze should attack the two highest-current-health visible targets");
            });
        });
    }
}
