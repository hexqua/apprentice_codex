package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.Objects;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsDisplayMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast.Aim;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // client描画を起動せず、軌道と姿勢の時刻契約を検証する。
public final class PsychicDriveGauntletsSingleMotionGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsSingleMotionGameTests() {}

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_single_motion");
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(4);
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
        Objects.requireNonNull(player.getAttribute(AttributeRegistry.MAX_MANA.get())).setBaseValue(100000);
        MagicData.getPlayerMagicData(player).setMana(100000);
        return player;
    }

    private static Cow target(GameTestHelper helper, Vec3 position) {
        var cow = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
        cow.setPos(position);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(100);
        cow.setHealth(100);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    private static void equal(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-5, message + ": " + actual + " != " + expected);
    }

    @GameTest(template = TEMPLATE)
    public static void preparationDefersDamageAndReleaseKeepsTheAcceptedShot(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, player.getEyePosition().add(0, -0.5, 2));
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.input(player, Input.PRESS, 0) && state.right().phase() == Phase.PREPARING,
                "A left click must reserve the right fist for preparation");
        var destination = state.right().snapshot().destination();
        state.input(player, Input.RELEASE, 0);
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(40);
        player.setPos(player.position().add(0.2, 0, 0));
        player.setYRot(90);
        state.tick(player, 1);
        helper.assertTrue(state.right().phase() == Phase.PREPARING, "Preparation must still hold after one tick");
        equal(helper, cow.getHealth(), 100, "The first preparation tick must not deal damage");
        state.tick(player, 2);
        helper.assertTrue(state.right().phase() == Phase.OUTBOUND, "Preparation must finish in two ticks");
        equal(helper, cow.getHealth(), 100, "Preparation and departure must not deal damage");
        equal(helper, state.right().snapshot().destination().distanceTo(destination), 0,
                "Turning during preparation must not change the accepted aim point");
        for (int tick = 3; tick <= 5; tick++) state.tick(player, tick);
        equal(helper, cow.getHealth(), 96, "The accepted shot must use input-time damage after release");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void replacementDuringPreparationReturnsWithoutFiring(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, player.getEyePosition().add(0, -0.5, 2));
        var state = new PsychicDriveGauntletsAttackState();
        state.input(player, Input.PRESS, 100);
        equal(helper, PsychicDriveGauntletsHudState.sample(state.right().snapshot(), 0.5).recovery(), 0,
                "Preparation must keep the recovery gauge empty");
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        state.tick(player, 101);
        helper.assertTrue(state.right().phase() == Phase.RETURNING, "Replacement must recall before firing");
        var pose = PsychicDriveGauntletsMotion.singlePose(state.right().snapshot(), 101);
        equal(helper, pose.power(), 0.5, "Early recall must retain the preparation pose");
        equal(helper, pose.twist(), 5, "Early recall must retain the fist twist");
        for (int tick = 102; tick <= 106; tick++) state.tick(player, tick);
        equal(helper, cow.getHealth(), 100, "Preparation recall must not cause an invisible hit");
        helper.assertTrue(state.right().available(), "Recall must finish in five ticks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sevenTickRepeatsUseBusyArmFallbackAtEveryRange(GameTestHelper helper) {
        var player = player(helper);
        for (int distance : new int[] {1, 3, 6}) {
            player.setPos(player.position().add(0, 5, 0));
            var cow = target(helper, player.getEyePosition().add(0, -0.5, distance));
            var state = new PsychicDriveGauntletsAttackState();
            helper.assertTrue(state.input(player, Input.PRESS, 0), "First shot must start");
            for (int tick = 1; tick <= 7; tick++) state.tick(player, tick);
            helper.assertTrue(!state.right().available() && state.left().available(),
                    "The right fist must still be returning at the next repeat, range=" + distance);
            helper.assertTrue(state.input(player, Input.REPEAT, 7) && state.left().phase() == Phase.PREPARING,
                    "The next repeat must prepare the available left fist");
            for (int tick = 8; tick <= 14; tick++) state.tick(player, tick);
            helper.assertTrue(state.right().available() && !state.left().available(),
                    "The right fist must be ready while the left fist returns");
            helper.assertTrue(state.input(player, Input.REPEAT, 14) && state.right().phase() == Phase.PREPARING,
                    "The third shot must reuse the right fist");
            cow.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void preparationAndReturnPosesConnectWithoutStrike(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.prepare(player, new Aim(player.getEyePosition(), start.add(0, 0, 6), true), 1, 100);
        var halfway = PsychicDriveGauntletsMotion.singlePose(arm.snapshot(), 101);
        equal(helper, halfway.power(), 0.5, "Preparation must have a visible intermediate pose");
        equal(helper, halfway.twist(), 5, "Preparation must twist the whole fist");
        var display = new PsychicDriveGauntletsDisplayMotion(arm.snapshot(), 1000.25);
        equal(helper, display.sample(1001.25).position().distanceTo(start.add(0, 0, -0.05)), 0,
                "Client preparation must use the same backward movement");
        arm.tick(player, 1, 101);
        arm.tick(player, 1, 102);
        equal(helper, arm.position().distanceTo(start.add(0, 0, -0.1)), 0, "The fist must pull back 0.1 blocks");
        var departure = PsychicDriveGauntletsMotion.singlePose(arm.snapshot(), 102);
        equal(helper, departure.power(), 1, "Departure must start from the prepared ring pose");
        equal(helper, departure.twist(), 10, "Departure must retain preparation twist");
        arm.tick(player, 1, 103);
        equal(helper, PsychicDriveGauntletsMotion.singlePose(arm.snapshot(), 103).twist(), 0,
                "The fist must straighten in the first outbound tick");
        for (int tick = 104; tick <= 107; tick++) arm.tick(player, 1, tick);
        helper.assertTrue(arm.phase() == Phase.RETURNING, "Block hold must enter return after two ticks");
        equal(helper, PsychicDriveGauntletsMotion.singlePose(arm.snapshot(), 107).power(), 1,
                "Return must start from the actual contact pose rather than a strike pose");
        for (int tick = 108; tick <= 112; tick++) arm.tick(player, 1, tick);
        equal(helper, PsychicDriveGauntletsMotion.singlePose(arm.snapshot(), 112).power(), 0,
                "Return must finish in the neutral pose without a trailing nine-tick clip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void curvedReturnDeceleratesAndMirrorsOutward(GameTestHelper helper) {
        var start = new Vec3(0, 0, 6);
        var end = Vec3.ZERO;
        var rightSide = new Vec3(-1, 0, 0);
        var leftSide = new Vec3(1, 0, 0);
        var first = PsychicDriveGauntletsMotion.returning(start, end, rightSide, 1);
        var second = PsychicDriveGauntletsMotion.returning(start, end, rightSide, 2);
        var third = PsychicDriveGauntletsMotion.returning(start, end, rightSide, 3);
        helper.assertTrue(start.z - first.z > first.z - second.z && first.z - second.z > second.z - third.z,
                "The returning fist must decelerate toward the attachment");
        var right = PsychicDriveGauntletsMotion.returning(start, end, rightSide, 2.5);
        var left = PsychicDriveGauntletsMotion.returning(start, end, leftSide, 2.5);
        equal(helper, right.x, -0.15, "The right fist must clear the outbound path outward");
        equal(helper, left.x, 0.15, "The left fist must mirror the outward curve");
        equal(helper, right.z, left.z, "Mirroring must preserve return timing");
        equal(helper, PsychicDriveGauntletsMotion.returning(start, end, rightSide, 0).distanceTo(start), 0,
                "Return must start at the impact point");
        equal(helper, PsychicDriveGauntletsMotion.returning(start, end, rightSide, 5).distanceTo(end), 0,
                "Return must finish at the attachment");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void returnClockAndPoseSurviveDifferentClientTimeAndRepeatedPackets(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, new Aim(player.getEyePosition(), start.add(0, 0, 6), true), 1, 100, InteractionHand.MAIN_HAND, 0.5F);
        for (int tick = 101; tick <= 105; tick++) arm.tick(player, 1, tick);
        var initial = arm.snapshot();
        helper.assertTrue(initial.phase() == Phase.RETURNING, "The fixture must start at return");
        var display = new PsychicDriveGauntletsDisplayMotion(initial, 1000.25);
        var atStart = display.sample(1000.25);
        equal(helper, PsychicDriveGauntletsMotion.singlePose(atStart, 1000.25).power(), initial.motion().returnPower(),
                "Client pose must use the mapped server phase clock");
        arm.tick(player, 1, 106);
        var before = display.sample(1001.25).position();
        display.accept(arm.snapshot(), 1001.25);
        equal(helper, display.sample(1001.25).position().distanceTo(before), 0, "A return packet must not restart the curve");
        equal(helper, before.distanceTo(arm.position()), 0, "Client and server must follow the same curve");
        var rendered = display.sample(1002.75);
        var expected = PsychicDriveGauntletsMotion.singlePose(initial, 107.5);
        equal(helper, PsychicDriveGauntletsMotion.singlePose(rendered, 1002.75).power(), expected.power(),
                "Repeated packets must not restart pose recovery");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var other = new PsychicDriveGauntletsArm(false);
            other.prepare(player, new Aim(player.getEyePosition(), start.add(0, 0, 3), false), 1, 106);
            var packet = new SyncPsychicDriveGauntletsPacket(player.getUUID(), false, arm.snapshot(), other.snapshot());
            SyncPsychicDriveGauntletsPacket.encode(packet, buffer);
            helper.assertTrue(packet.equals(SyncPsychicDriveGauntletsPacket.decode(buffer)) && !buffer.isReadable(),
                    "Preparation, return path and captured pose must survive the complete packet codec");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missedFollowThroughIsHarmlessAndConnectsToReturn(GameTestHelper helper) {
        var player = player(helper);
        for (float charge : new float[] {0, 0.5F}) {
            for (boolean right : new boolean[] {true, false}) {
                player.setYRot(0);
                var arm = new PsychicDriveGauntletsArm(right);
                var start = PsychicDriveGauntletsPose.position(player, 1, right);
                var destination = start.add(0, 0, 6);
                arm.launch(player, new Aim(player.getEyePosition(), destination, false), 1, 100, InteractionHand.MAIN_HAND, charge);
                for (int tick = 101; tick <= 103; tick++) arm.tick(player, 1, tick);
                helper.assertTrue(arm.phase() == Phase.FOLLOW_THROUGH, "A single miss must follow through");
                var initial = arm.snapshot();
                var display = new PsychicDriveGauntletsDisplayMotion(initial, 1000.25);
                var cow = target(helper, destination.add(0, -0.5, 0));
                // 余韻の抜き方向は到達時に固定し、視点を回しても軌道が跳ねない。
                player.setYRot(90);
                arm.tick(player, 1, 104);
                equal(helper, arm.position().distanceTo(destination), 0, "The first follow-through tick must hold extension");
                var before = display.sample(1001.25);
                display.accept(arm.snapshot(), 1001.25);
                equal(helper, display.sample(1001.25).position().distanceTo(before.position()), 0,
                        "Repeated follow-through packets must not restart the path");
                arm.tick(player, 1, 105);
                var halfway = display.sample(1002.25);
                equal(helper, halfway.position().distanceTo(arm.position()), 0, "Client follow-through must match server motion");
                equal(helper, arm.position().x - destination.x, right ? -0.075 : 0.075, "Follow-through must mirror outward");
                arm.tick(player, 1, 106);
                helper.assertTrue(arm.phase() == Phase.RETURNING, "Return must begin after three follow-through ticks");
                equal(helper, arm.position().x - destination.x, right ? -0.15 : 0.15, "Return must start outside the flight path");
                var endPose = PsychicDriveGauntletsMotion.singlePose(arm.snapshot(), 106);
                equal(helper, endPose.power(), 0.75, "Return must retain the relaxed follow-through pose");
                equal(helper, endPose.twist(), -5, "Return must retain the outward twist");
                equal(helper, display.sample(1003.25).position().distanceTo(arm.position()), 0,
                        "Follow-through and return positions must connect");
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    var packet = new SyncPsychicDriveGauntletsPacket(player.getUUID(), false, initial, arm.snapshot());
                    SyncPsychicDriveGauntletsPacket.encode(packet, buffer);
                    helper.assertTrue(packet.equals(SyncPsychicDriveGauntletsPacket.decode(buffer)),
                            "Follow-through and return must survive the packet codec");
                } finally { buffer.release(); }
                for (int tick = 107; tick <= 111; tick++) arm.tick(player, 1, tick);
                equal(helper, cow.getHealth(), 100, "Targets entering during follow-through or return must not be hit");
                helper.assertTrue(arm.available(), "Follow-through plus return must take eight ticks after arrival");
                cow.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missedRepeatsAlternateAndSourceChangeRecallsFollowThrough(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        state.input(player, Input.PRESS, 0);
        for (int tick = 1; tick <= 7; tick++) state.tick(player, tick);
        helper.assertTrue(state.right().phase() == Phase.FOLLOW_THROUGH
                        && state.input(player, Input.REPEAT, 7) && state.left().phase() == Phase.PREPARING,
                "A missed right fist must leave the next seven-tick repeat to the left fist");
        for (int tick = 8; tick <= 14; tick++) state.tick(player, tick);
        helper.assertTrue(state.right().available() && state.input(player, Input.REPEAT, 14),
                "An empty-range miss must finish before that fist is needed again");
        var arm = new PsychicDriveGauntletsArm(true);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        arm.launch(player, start.add(0, 0, 6), 1, 100);
        for (int tick = 101; tick <= 104; tick++) arm.tick(player, 1, tick);
        var position = arm.position();
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        arm.tick(player, 1, 104);
        helper.assertTrue(arm.phase() == Phase.RETURNING && arm.position().equals(position),
                "Source replacement must recall follow-through without moving the fist");
        equal(helper, PsychicDriveGauntletsHudState.sample(arm.snapshot(), 0).recovery(), 1 / 8F,
                "Interrupted follow-through must preserve its accrued recovery");
        for (int tick = 105; tick <= 109; tick++) arm.tick(player, 1, tick);
        helper.assertTrue(arm.available(), "Source replacement must not restart return");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void teleportDuringPreparationCannotCreateALongDamageSweep(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.prepare(player, new Aim(player.getEyePosition(), start.add(0, 0, 6), false), 1, 0);
        var cow = target(helper, start.add(32, -0.5, 3));
        player.setPos(player.position().add(64, 0, 0));
        arm.tick(player, 1, 1);
        helper.assertTrue(arm.phase() == Phase.RETURNING,
                "An aim point outside the normal flight range after teleport must recall before flight");
        for (int tick = 2; tick <= 6; tick++) arm.tick(player, 1, tick);
        equal(helper, cow.getHealth(), 100, "Preparation must not create damage along the teleport route");
        helper.assertTrue(arm.available(), "Teleport recall must finish without extending recovery");
        helper.succeed();
    }
}
