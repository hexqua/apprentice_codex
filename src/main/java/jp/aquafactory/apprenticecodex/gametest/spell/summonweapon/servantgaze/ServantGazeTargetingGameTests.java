package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.TARGETING_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ServantGazeTargetingGameTests {
    private ServantGazeTargetingGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = TARGETING_ISOLATED_BATCH, timeoutTicks = 100)
    public static void servantGazeConsumesManaPerHighestHealthTarget(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_targeting");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(20.0F);
        var low = scene.zombie(new BlockPos(0, 2, 4), 5.0F);
        var middle = scene.zombie(new BlockPos(3, 2, 1), 10.0F);
        var high = scene.zombie(new BlockPos(4, 2, 3), 15.0F);
        scene.activate(100, 1, 2.0F, 4.0, 10);

        helper.runAfterDelay(55, () -> {
            helper.assertTrue(magicData.getMana() == 0.0F,
                    "Servant Gaze must pay per selected target: mana=" + magicData.getMana() + ", low=" + low.getHealth() + ", middle=" + middle.getHealth() + ", high=" + high.getHealth());
            helper.assertTrue(low.getHealth() == 5.0F,
                    "Servant Gaze should leave the lower-health third target unselected when mana is insufficient");
            helper.assertTrue(middle.getHealth() < 10.0F && high.getHealth() < 15.0F,
                    "Servant Gaze should attack the two highest-current-health visible targets");
            helper.succeed();
        });
    }
}
