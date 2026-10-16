package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsDisplayMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState.Gauge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.Objects;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // HUDの状態計算と同期を、描画APIやclient起動に依存せず検証する。
public final class PsychicDriveGauntletsHudGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsHudGameTests() {}

    private static ItemStack gauntlets() {
        return new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get());
    }

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_hud");
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlets());
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
        return player;
    }

    private static Gauge gauge(PsychicDriveGauntletsArm arm) {
        return PsychicDriveGauntletsHudState.sample(arm.snapshot(), 0);
    }

    private static void equal(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-6, message + ": " + actual + " != " + expected);
    }

    @GameTest(template = TEMPLATE)
    public static void impactRecoversInSevenTicksAndMissInEight(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        for (boolean impact : new boolean[] {false, true}) {
            if (impact) {
                var target = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
                target.setPos(start.add(0, -0.5, 1.2));
                target.setNoAi(true);
                target.setNoGravity(true);
                helper.getLevel().addFreshEntity(target);
            }
            var arm = new PsychicDriveGauntletsArm(true);
            arm.launch(player, start.add(0, 0, 6), 1, 100);
            equal(helper, gauge(arm).recovery(), 0, "Launch must empty the gauge");
            for (int tick = 101; arm.phase() == Phase.OUTBOUND && tick <= 103; tick++) arm.tick(player, 1, tick);
            helper.assertTrue(arm.phase() == (impact ? Phase.HOLDING : Phase.FOLLOW_THROUGH),
                    "A miss must follow through while a contact holds");
            long arrived = arm.snapshot().sampleTick();
            int duration = impact ? 7 : 8;
            equal(helper, gauge(arm).recovery(), 0, "Recovery must begin at confirmed arrival");
            for (int elapsed = 1; elapsed <= duration; elapsed++) {
                arm.tick(player, 1, arrived + elapsed);
                equal(helper, gauge(arm).recovery(), (float) elapsed / duration, "Recovery must match availability");
                helper.assertTrue(arm.available() == (elapsed == duration), "Only completed return must be available");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void forcedReturnsPreserveProgressAndDoNotRestart(GameTestHelper helper) {
        var player = player(helper);
        for (int holdTicks : new int[] {-1, 0, 1}) {
            var arm = new PsychicDriveGauntletsArm(true);
            var start = PsychicDriveGauntletsPose.position(player, 1, true);
            arm.launch(player, new PsychicDriveGauntletsRaycast.Aim(null, start.add(0, 0, 6), true),
                    1, 100, InteractionHand.MAIN_HAND, 0);
            int recalled = holdTicks < 0 ? 101 : 103 + holdTicks;
            for (int tick = 101; tick < recalled; tick++) arm.tick(player, 1, tick);
            if (holdTicks >= 0) arm.tick(player, 1, recalled);
            float initial = holdTicks < 0 ? 0 : holdTicks / 7F;
            player.setItemInHand(InteractionHand.MAIN_HAND, gauntlets());
            arm.tick(player, 1, recalled);
            helper.assertTrue(arm.phase() == Phase.RETURNING, "Replacing the source must recall the arm");
            equal(helper, gauge(arm).recovery(), initial, "Early return must preserve the accrued recovery");
            for (int elapsed = 1; elapsed <= 5; elapsed++) {
                arm.tick(player, 1, recalled + elapsed);
                equal(helper, gauge(arm).recovery(), initial + (1 - initial) * elapsed / 5,
                        "Forced return must finish the remaining recovery in five ticks");
            }
            helper.assertTrue(arm.available(), "Source changes must not restart return on every tick");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chargingAndLaunchingKeepTheOtherArmIndependent(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.beginUse(player, InteractionHand.MAIN_HAND, 100), "Charge must start");
        helper.assertTrue(state.input(player, Input.PRESS, 100), "The other arm must still launch");
        for (int elapsed = 0; elapsed <= 21; elapsed++) {
            state.tick(player, 100 + elapsed);
            equal(helper, gauge(state.left()).recovery(), 1, "Charging must keep its normal gauge full");
            equal(helper, gauge(state.left()).charge(), Math.min(elapsed, 20) / 20F, "Charge must cap at twenty ticks");
            equal(helper, gauge(state.right()).charge(), 0, "The other arm must not receive the red overlay");
        }
        helper.assertTrue(state.releaseUse(player, 121), "Releasing a full charge must launch");
        equal(helper, gauge(state.left()).recovery(), 0, "Charged launch must empty its normal gauge");
        equal(helper, gauge(state.left()).charge(), 0, "Charged flight must clear its red overlay");
        equal(helper, gauge(state.right()).recovery(), 1, "The returned other arm must stay ready");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shortChargesAndCanceledChargesRestoreReadyState(GameTestHelper helper) {
        var player = player(helper);
        for (int elapsed : new int[] {0, 4, 5, 20}) {
            var state = new PsychicDriveGauntletsAttackState();
            state.beginUse(player, InteractionHand.MAIN_HAND, 100);
            state.tick(player, 100 + elapsed);
            if (elapsed < 5) {
                helper.assertTrue(!state.releaseUse(player, 100 + elapsed), "Short release must not launch");
            } else state.cancelUse();
            helper.assertTrue(gauge(state.left()).equals(PsychicDriveGauntletsHudState.READY),
                    "Rejected or canceled charge must clear red and restore full normal gauge");
        }
        var duplicate = new PsychicDriveGauntletsAttackState();
        duplicate.beginUse(player, InteractionHand.MAIN_HAND, 100);
        duplicate.input(player, Input.PRESS, 100);
        player.getInventory().setItem(8, gauntlets());
        duplicate.tick(player, 101);
        helper.assertTrue(gauge(duplicate.left()).equals(PsychicDriveGauntletsHudState.READY)
                        && gauge(duplicate.right()).equals(PsychicDriveGauntletsHudState.READY),
                "Duplicate reset must discard recovery and charge for both arms");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void samplesUseServerRecoveryClockAndWaitForIdleConfirmation(GameTestHelper helper) {
        var holding = new Snapshot(Phase.HOLDING, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO,
                100, 104, 0, 103, 0);
        equal(helper, PsychicDriveGauntletsHudState.sample(holding, 0).recovery(), 1 / 7F,
                "First received holding packet must include recovery before receipt");
        equal(helper, PsychicDriveGauntletsHudState.sample(holding, 0.5).recovery(), 1.5F / 7,
                "Recovery must interpolate partial ticks");
        equal(helper, PsychicDriveGauntletsHudState.sample(holding, 50).recovery(), 2 / 7F,
                "Missing return updates must not complete a holding arm");
        var returning = new Snapshot(Phase.RETURNING, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO,
                100, 107, 0, 105, 2 / 7F);
        equal(helper, PsychicDriveGauntletsHudState.sample(returning, 0.5).recovery(), 4.5F / 7,
                "A first return packet must retain the recovery clock");
        helper.assertTrue(PsychicDriveGauntletsHudState.sample(returning, 100).recovery() < 1,
                "Missing idle confirmation must not claim the arm is available");
        var outbound = new Snapshot(Phase.OUTBOUND, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO, 100, 100);
        equal(helper, PsychicDriveGauntletsHudState.sample(outbound, 100).recovery(), 0,
                "Flight duration must never predict recovery before confirmed arrival");
        var charging = new Snapshot(Phase.CHARGING, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO, 100, 105);
        equal(helper, PsychicDriveGauntletsHudState.sample(charging, 0.5).charge(), 5.5F / 20,
                "Red charge must interpolate from the server start time");
        var idle = new Snapshot(Phase.IDLE, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO, 100, 118);
        var motion = new PsychicDriveGauntletsDisplayMotion(returning, 1000);
        motion.accept(idle, 1000.5);
        helper.assertTrue(motion.sample(1000.5).phase() == Phase.RETURNING
                        && PsychicDriveGauntletsHudState.sample(idle, 0).equals(PsychicDriveGauntletsHudState.READY),
                "HUD readiness must not wait for the final 3D return interpolation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void recoveryMetadataSurvivesIndependentArmPackets(GameTestHelper helper) {
        var right = new Snapshot(Phase.RETURNING, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO,
                100, 110, 0.5F, 108, 1 / 7F);
        var left = new Snapshot(Phase.HOLDING, Vec3.ZERO, 0, 0, 1, Vec3.ZERO, Vec3.ZERO,
                104, 110, 1, 107, 0, PsychicDriveGauntletsArm.Impact.ENTITY, 107, 12);
        var player = player(helper);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        try {
            var packet = new SyncPsychicDriveGauntletsPacket(player.getUUID(), false, right, left);
            SyncPsychicDriveGauntletsPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = SyncPsychicDriveGauntletsPacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(packet.equals(decoded) && buffer.readableBytes() == 0,
                    "Arm recovery metadata must survive the full packet codec");
            helper.assertTrue(PsychicDriveGauntletsHudState.sample(decoded.right(), 0)
                            .equals(PsychicDriveGauntletsHudState.sample(right, 0))
                            && PsychicDriveGauntletsHudState.sample(decoded.left(), 0)
                            .equals(PsychicDriveGauntletsHudState.sample(left, 0)),
                    "Each decoded arm must retain its own recovery timeline");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void displayEligibilityIncludesBothAttackModesButExcludesSheathedAndDuplicate(GameTestHelper helper) {
        var player = player(helper);
        helper.assertTrue(PsychicDriveGauntletsHudState.canDisplay(player), "Mainhand gauntlets must show HUD");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(PsychicDriveGauntletsHudState.canDisplay(player), "Left-only mainhand operation must show HUD");
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        helper.assertTrue(PsychicDriveGauntletsHudState.canDisplay(player), "Offhand charge-only operation must show HUD");
        player.getInventory().setItem(8, gauntlets());
        helper.assertTrue(!PsychicDriveGauntletsHudState.canDisplay(player), "Duplicates must hide HUD");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        helper.assertTrue(!PsychicDriveGauntletsHudState.canDisplay(player), "Sheathed hotbar gauntlets must hide HUD");
        player.getInventory().setItem(8, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, gauntlets());
        player.setHealth(0);
        helper.assertTrue(!PsychicDriveGauntletsHudState.canDisplay(player), "Dead players must hide HUD");
        helper.succeed();
    }
}
