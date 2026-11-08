package jp.aquafactory.apprenticecodex.gametest.equipment.broom.control;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.BroomCoreWarningState;
import jp.aquafactory.apprenticecodex.entity.broom.BroomInputTransition;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomPresentation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.magicData;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnHoverrideBroom;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class HoverrideControlGameTests {
    private HoverrideControlGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void hoverrideReleaseRequiresOneSuccessfulServerTick(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.5D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride release test rider should mount");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "A same-tick Hoverride release must be rejected without consuming mana");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(broom.isServerInertiaGlideActive()
                            && broom.getServerSuccessfulGlideTicks() == 1,
                    "Hoverride inertia glide should become releasable after one paid server tick");
            helper.assertTrue(Math.abs(magicData.getMana() - 99.5F) < 1.0e-4F,
                    "One Hoverride inertia glide tick should consume its configured mana");

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 2L);
            helper.assertTrue(Math.abs(magicData.getMana() - 49.5F) < 1.0e-4F,
                    "An accepted Hoverride release should consume its configured mana once");
            helper.assertFalse(broom.isServerInertiaGlideActive(),
                    "An accepted Hoverride release should end server glide state");

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 2L);
            helper.assertTrue(Math.abs(magicData.getMana() - 49.5F) < 1.0e-4F,
                    "A repeated Hoverride release must not consume mana again");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideLocalReleaseWaitsForItsMatchingResult(GameTestHelper helper) {
        var broom = spawnHoverrideBroom(helper, 1.5D);
        var player = serverRider(helper);
        var magicData = magicData(helper, player);
        magicData.setMana(1000.0F);
        helper.assertTrue(player.startRiding(broom, true), "Hoverride delayed release test rider should mount");

        broom.setYRot(0.0F);
        broom.setDeltaMovement(0.1D, 0.0D, 0.0D);
        broom.setLocalInput(0.0F, 0.0F, true, false);
        broom.handleLocalInputTransition(BroomInputTransition.RELEASE, 42L);

        // 旧実装の20 client tick相当を超えても、serverで確定した解除結果との相関を維持する。
        for (var i = 0; i < 25; ++i) {
            broom.tick();
        }
        broom.acceptLocalReleaseResult(41L, true, 0.4D);
        helper.assertTrue(broom.getDeltaMovement().equals(Vec3.ZERO),
                "A mismatched Hoverride release result must not apply an impulse");

        broom.acceptLocalReleaseResult(42L, true, 0.4D);
        helper.assertTrue(Math.abs(broom.getDeltaMovement().horizontalDistance() - 0.4D) < 1.0e-6D,
                "A delayed matching Hoverride release result should still apply its impulse");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideCancelDoesNotTriggerReleaseCost(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.5D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride cancel test rider should mount");
            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.CANCEL, 1L);

            helper.assertTrue(Math.abs(magicData.getMana() - 99.5F) < 1.0e-4F,
                    "Hoverride cancellation should only retain the already paid glide tick");
            helper.assertFalse(broom.isServerInertiaGlideActive(),
                    "Hoverride cancellation should clear glide state without a release");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideActionSequenceResetsForTheNextRider(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.0D, 10.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var firstRider = serverRider(helper);
            var firstMana = magicData(helper, firstRider);
            firstMana.setMana(100.0F);
            helper.assertTrue(firstRider.startRiding(broom, true), "First Hoverride rider should mount");
            broom.acceptServerInput(firstRider, 0.0F, 0.0F, false, false,
                    BroomInputTransition.CANCEL, 100L);
            firstRider.stopRiding();

            var nextRider = serverRider(helper);
            var nextMana = magicData(helper, nextRider);
            nextMana.setMana(100.0F);
            helper.assertTrue(nextRider.startRiding(broom, true), "Next Hoverride rider should mount");
            broom.acceptServerInput(nextRider, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            broom.acceptServerInput(nextRider, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);

            helper.assertTrue(Math.abs(nextMana.getMana() - 90.0F) < 1.0e-4F,
                    "A new Hoverride rider should be able to release from a fresh action sequence");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideReleaseDepletionAndRecoveryUseConfiguredCost(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(1.0D, 0.0D, 50.0D, 0.5D, 20.0D);
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(50.0F);
            helper.assertTrue(player.startRiding(broom, true), "Hoverride depletion test rider should mount");
            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "An exact-cost Hoverride release should consume all remaining mana");
            helper.assertTrue(broom.isManaDepleted(),
                    "An exact-cost Hoverride release should enter depleted mode");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.CRITICAL,
                    "Hoverride depleted mode should activate the core warning flash");

            magicData.setMana(49.999F);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(),
                    "Hoverride depleted mode should remain below the configured recovery threshold");
            magicData.setMana(50.0F);
            broom.tick();
            helper.assertFalse(broom.isManaDepleted(),
                    "Hoverride depleted mode should recover at the configured release cost");
            helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                    "Recovering Hoverride propulsion should clear the core warning flash");

            magicData.setMana(0.0F);
            broom.acceptServerInput(player, 0.0F, -1.0F, false, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(), "Zero mana should enter Hoverride depleted mode");
            helper.assertTrue(broom.getPresentationState() == HoverrideBroomPresentation.BRAKING,
                    "Hoverride braking feedback should remain active while mana is depleted");
            player.stopRiding();
            helper.assertFalse(broom.isManaDepleted(), "Dismounting should clear Hoverride depleted mode");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideReleaseManaCostMustBePositive(GameTestHelper helper) {
        var rejected = false;
        try {
            new HoverrideBroomServerConfig.Values(0.0D, 0.0D, 0.0D, 0.5D, 20.0D);
        } catch (IllegalArgumentException ignored) {
            rejected = true;
        }
        helper.assertTrue(rejected,
                "Hoverride release mana cost must reject zero even when per-tick costs are disabled");
        helper.succeed();
    }
}
