package jp.aquafactory.apprenticecodex.gametest.equipment.broom.movement;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.config.item.HoverrideBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.BroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.broom.BroomInputTransition;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomManaGameTests {
    private BroomManaGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void hoverrideMountingDoesNotRequireMinimumMana(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var pos = helper.absolutePos(TEST_POS);
            var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
            broom.setPos(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(broom);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.0F);

            broom.interact(player, InteractionHand.MAIN_HAND);

            helper.assertTrue(player.getVehicle() == broom,
                    "Hoverride Broom should allow mounting with zero mana");
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Mounting the Hoverride Broom must not consume mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mountingRequiresConfiguredManaButCreativeIsExempt(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(99.0F);

            broom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(player.isPassenger(), "Player below the normal flight threshold must not mount");

            magicData.setMana(100.0F);
            broom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == broom, "Player at the normal flight threshold should mount");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Mounting must not consume mana");

            player.stopRiding();
            var creativeBroom = spawnBroom(helper, 1.5D);
            player.getAbilities().instabuild = true;
            magicData.setMana(0.0F);
            creativeBroom.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getVehicle() == creativeBroom, "Creative players must mount with zero mana");
            creativeBroom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            creativeBroom.tick();
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Creative broom movement must not consume mana");
            helper.assertFalse(creativeBroom.isManaEmergencyLanding(),
                    "Creative broom movement must not enter emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void movementInputsUseConfiguredManaCosts(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            assertMovementManaCost(helper, "floatmount_broom_horizontal_cost", 1.0F,
                    0.0F, 1.0F, false, false);
            assertMovementManaCost(helper, "floatmount_broom_ascending_cost", 2.0F,
                    0.0F, 0.0F, true, false);
            assertMovementManaCost(helper, "floatmount_broom_combined_cost", 3.5F,
                    0.0F, -1.0F, true, false);
            assertMovementManaCost(helper, "floatmount_broom_turning_free", 0.0F,
                    1.0F, 0.0F, false, false);
            assertMovementManaCost(helper, "floatmount_broom_descending_free", 0.0F,
                    0.0F, 0.0F, false, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inactiveInputStopsMovementManaCost(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(100.0F);
            helper.assertTrue(player.startRiding(broom, true), "Inactive input test player should mount directly");

            broom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 96.5F) < 1.0e-4F,
                    "Active combined movement should consume configured mana");

            // clientの画面操作はGameTestで再現せず、inactive受理後のserver課金停止だけを固定する。
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 96.5F) < 1.0e-4F,
                    "Inactive movement input must stop additional mana consumption");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void serverTeleportWithoutReportedPoweredInputRemainsFree(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Server teleport test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 0.0F, false, false);

            // client権威のvehicle位置差だけでは、惰性・server再配置・他MODの外力と偽装移動を区別できない。
            // 正規playerへの誤課金を避けるため、申告された動力入力がない再配置は課金対象にしない。
            // その結果、改造clientによる入力の過少申告も防げないことを設計上受容する。
            var beforeTeleport = broom.position();
            broom.teleportRelative(1.0D, 1.0D, 0.0D);
            helper.assertTrue(broom.position().distanceToSqr(beforeTeleport) > 1.0D,
                    "Server teleport should move the mounted broom horizontally and upward");

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana() - 0.5F) < 1.0e-4F,
                    "Server teleport without reported powered input must not consume mana");
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "Server teleport without reported powered input must not trigger emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nonControllingPlayerCannotSubmitPaidMovementInput(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 2.0D, 3.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var rider = serverRider(helper);
            var nonController = player(helper, "floatmount_broom_non_controller");
            var riderMagicData = magicData(helper, rider);
            riderMagicData.setMana(100.0F);
            helper.assertTrue(rider.startRiding(broom, true), "Controller test rider should mount directly");

            broom.acceptServerInput(nonController, 0.0F, 1.0F, true, false);
            broom.tick();

            helper.assertTrue(Math.abs(riderMagicData.getMana() - 100.0F) < 1.0e-4F,
                    "A non-controlling player must not consume the rider's mana");
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "A non-controlling player must not trigger the rider's emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void depletionEmergencyRecoveryAndSavedStateFollowServerRules(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Emergency test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "A movement cost above the remaining mana should consume the remainder");
            helper.assertTrue(broom.isManaEmergencyLanding(), "Movement depletion should enter emergency landing");

            magicData.setMana(10.0F);
            broom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                    "Emergency movement must not consume mana");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isManaEmergencyLanding(), "Emergency landing must persist in entity NBT");

            magicData.setMana(100.0F);
            broom.tick();
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "Reaching the normal flight threshold should recover normal flight");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Recovery tick must not immediately consume mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalDepletionWaitsForPaidInputAndEmergencyForcesDismountWarning(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            var magicData = magicData(helper, player);
            magicData.setMana(0.0F);
            helper.assertTrue(player.startRiding(broom, true), "External depletion test player should mount directly");

            broom.tick();
            helper.assertFalse(broom.isManaEmergencyLanding(),
                    "External zero mana must not trigger emergency landing while idle");
            var warningState = new CompoundTag();
            broom.saveWithoutId(warningState);
            helper.assertTrue(warningState.getBoolean("LowManaWarningShown"),
                    "Low mana warning latch should be stored on the broom entity");

            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);
            broom.tick();
            helper.assertTrue(broom.isManaEmergencyLanding(),
                    "Paid movement attempted at zero mana should trigger emergency landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Emergency landing must force dismount confirmation even near the ground");

            BroomDismountEvents.handleSneakInput(player, broom, true);
            var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
            BroomDismountEvents.onDismount(first);
            helper.assertTrue(first.isCanceled(), "First emergency dismount should be canceled");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void floatmountOverdriveUsesConfiguredManaCosts(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(
                1000, 50, 10, Set.of(), 100, 50,
                1.0D, 2.0D, 3.5D,
                1.75D, 1.0D, 2.5D
        );
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            assertMovementManaCost(helper, "floatmount_overdrive_horizontal_cost", 1.75F,
                    0.0F, 1.0F, false, false, true);
            assertMovementManaCost(helper, "floatmount_overdrive_ascending_cost", 1.0F,
                    0.0F, 0.0F, true, false, true);
            assertMovementManaCost(helper, "floatmount_overdrive_combined_cost", 2.5F,
                    0.0F, -1.0F, true, false, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideOverdriveUsesConfiguredManaCostsAndRecoveryThreshold(GameTestHelper helper) {
        var config = new HoverrideBroomServerConfig.Values(
                1.0D, 0.5D, 50.0D,
                2.25D, 1.25D, 77.0D,
                0.5D, 20.0D
        );
        try (var ignored = ApprenticeCodexServerConfig.useHoverrideBroomConfigOverrideForGameTest(config)) {
            var broom = spawnHoverrideBroom(helper, 1.5D);
            var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            stack, 0, new ItemStack(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get())),
                    "Hoverride Overdrive mana test should accept its engine");
            broom.setBroomItemStack(stack);
            var player = serverRider(helper, "hoverride_overdrive_mana_rider");
            var magicData = magicData(helper, player);
            magicData.setMana(78.25F);
            helper.assertTrue(player.startRiding(broom, true),
                    "Hoverride Overdrive mana test rider should mount");

            broom.acceptServerInput(player, 0.0F, 0.0F, true, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 77.0F) < 1.0e-4F,
                    "Overdrive inertia glide should use its configured mana cost");

            broom.acceptServerInput(player, 0.0F, 0.0F, false, false,
                    BroomInputTransition.RELEASE, 1L);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "Overdrive inertia release should use its configured mana cost");
            helper.assertTrue(broom.isManaDepleted(),
                    "Exact Overdrive release cost should enter depleted mode");

            magicData.setMana(76.0F);
            broom.tick();
            helper.assertTrue(broom.isManaDepleted(),
                    "Overdrive depleted mode should remain below its configured release cost");
            magicData.setMana(77.0F);
            broom.tick();
            helper.assertFalse(broom.isManaDepleted(),
                    "Overdrive depleted mode should recover at its configured release cost");

            broom.acceptServerInput(player, 0.0F, 1.0F, false, false,
                    BroomInputTransition.NONE, 0L);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 74.75F) < 1.0e-4F,
                    "Overdrive forward acceleration should use its configured mana cost");
        }
        helper.succeed();
    }

    static void assertMovementManaCost(
            GameTestHelper helper,
            String playerName,
            float expectedCost,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending
    ) {
        assertMovementManaCost(
                helper, playerName, expectedCost, strafe, forward, ascending, descending, false
        );
    }

    static void assertMovementManaCost(
            GameTestHelper helper,
            String playerName,
            float expectedCost,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending,
            boolean overdriveEnabled
    ) {
        var broom = spawnBroom(helper, 1.5D);
        if (overdriveEnabled) {
            var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
            if (!SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                    stack, 0, new ItemStack(ItemRegistry.OVERDRIVE_BROOM_ENGINE.get())
            )) {
                throw new IllegalStateException("Floatmount Overdrive mana test could not install its engine");
            }
            broom.setBroomItemStack(stack);
        }
        var player = serverRider(helper);
        var magicData = magicData(helper, player);
        magicData.setMana(100.0F);
        helper.assertTrue(player.startRiding(broom, true), playerName + " should mount directly");
        broom.acceptServerInput(player, strafe, forward, ascending, descending);

        broom.tick();

        helper.assertTrue(Math.abs(magicData.getMana() - (100.0F - expectedCost)) < 1.0e-4F,
                playerName + " used an unexpected mana cost: " + magicData.getMana());
        player.stopRiding();
    }
}
