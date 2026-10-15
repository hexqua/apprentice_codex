package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAnimation;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAnimation.Stage;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsDisplayMotion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // 再生順序をclientに依存しない状態遷移として検証する。
public final class PsychicDriveGauntletsAnimationGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsAnimationGameTests() {}

    private static Snapshot arm(Phase phase, long launch, Impact impact) {
        return new Snapshot(phase, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO,
                launch, launch + 2, 0, 0, 0, impact, impact == Impact.NONE ? 0 : launch + 1);
    }

    @GameTest(template = TEMPLATE)
    public static void contactInterruptsWindupAndArmsRemainIndependent(GameTestHelper helper) {
        var right = new PsychicDriveGauntletsAnimation();
        var left = new PsychicDriveGauntletsAnimation();
        right.update(arm(Phase.CHARGING, 90, Impact.NONE), 0);
        helper.assertTrue(right.stage() == Stage.IDLE, "Charging must retain idle");
        right.update(arm(Phase.OUTBOUND, 100, Impact.NONE), 1);
        var revision = right.revision();
        right.update(arm(Phase.OUTBOUND, 100, Impact.NONE), 1.5);
        helper.assertTrue(right.stage() == Stage.WINDUP && right.revision() == revision,
                "Repeated frames and snapshots must not restart windup");
        var motion = new PsychicDriveGauntletsDisplayMotion(arm(Phase.OUTBOUND, 100, Impact.NONE), 1);
        motion.accept(arm(Phase.HOLDING, 100, Impact.ENTITY), 2);
        var displayed = motion.sample(2);
        right.update(displayed, 2);
        helper.assertTrue(right.stage() == Stage.STRIKE && displayed.impactTick() == 101,
                "Contact metadata must survive interpolation and immediately interrupt unfinished windup");
        revision = right.revision();
        right.update(motion.sample(3), 3);
        helper.assertTrue(right.revision() == revision && left.stage() == Stage.IDLE,
                "Strike must not restart or affect the other arm");
        left.update(arm(Phase.HOLDING, 102, Impact.BLOCK), 3);
        helper.assertTrue(left.stage() == Stage.STRIKE, "Block impact must also trigger strike");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missesSkipStrikeAndRecoverOutlivesReturn(GameTestHelper helper) {
        var animation = new PsychicDriveGauntletsAnimation();
        animation.update(arm(Phase.OUTBOUND, 100, Impact.NONE), 0);
        animation.update(arm(Phase.HOLDING, 100, Impact.NONE), 3);
        helper.assertTrue(animation.stage() == Stage.WINDUP, "A miss must keep windup instead of striking");
        animation.update(arm(Phase.RETURNING, 100, Impact.NONE), 13);
        var revision = animation.revision();
        animation.update(arm(Phase.RETURNING, 100, Impact.NONE), 14);
        helper.assertTrue(animation.stage() == Stage.RECOVER && animation.revision() == revision,
                "Return snapshots must not restart recovery");
        animation.update(null, 18);
        helper.assertTrue(animation.stage() == Stage.RECOVER,
                "Recovery must continue after the five-tick return has finished");
        animation.update(null, 21.99);
        helper.assertTrue(animation.stage() == Stage.RECOVER, "Recovery must last the full nine ticks");
        animation.update(null, 22);
        helper.assertTrue(animation.stage() == Stage.IDLE, "Recovery must finish in idle after 0.45 seconds");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void earlyRecallAndNewLaunchInterruptRecovery(GameTestHelper helper) {
        var animation = new PsychicDriveGauntletsAnimation();
        animation.update(arm(Phase.OUTBOUND, 100, Impact.NONE), 0);
        animation.update(arm(Phase.RETURNING, 100, Impact.NONE), 1);
        helper.assertTrue(animation.stage() == Stage.RECOVER, "Early recall must recover without striking");
        animation.update(null, 6);
        animation.update(arm(Phase.OUTBOUND, 106, Impact.NONE), 6);
        helper.assertTrue(animation.stage() == Stage.WINDUP, "A new attack must interrupt unfinished recovery");
        var revision = animation.revision();
        // 描画されない間に攻撃が完了しても、次の同種アニメーションを先頭から再生する。
        animation.update(arm(Phase.OUTBOUND, 130, Impact.NONE), 30);
        helper.assertTrue(animation.revision() > revision && animation.stage() == Stage.WINDUP,
                "A new launch observed in the same stage must restart that stage");
        helper.succeed();
    }
}
