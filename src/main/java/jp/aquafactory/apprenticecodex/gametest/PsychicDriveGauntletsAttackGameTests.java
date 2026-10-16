package jp.aquafactory.apprenticecodex.gametest;

import io.netty.buffer.Unpooled;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackEvents;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.network.packet.ClientPsychicDriveGauntletsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.fml.ModList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // GameTestはアノテーション走査で登録される。
public final class PsychicDriveGauntletsAttackGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private PsychicDriveGauntletsAttackGameTests() {}

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 2, 1), "psychic_attack");
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
        // FakePlayerをworldへ追加せず、装備属性更新の代わりに通常装備時の実効値を与える。
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        return player;
    }
    private static Cow target(GameTestHelper helper, Vec3 position) {
        var target = EntityType.COW.create(helper.getLevel());
        target.setPos(position);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        helper.getLevel().addFreshEntity(target);
        return target;
    }
    private static void ticks(PsychicDriveGauntletsArm arm, FakePlayer player, int from, int to) {
        for (int tick = from; tick <= to; tick++) arm.tick(player, 1, tick);
    }

    @GameTest(template = TEMPLATE)
    public static void aimUsesNearestInflatedIntersection(GameTestHelper helper) {
        var player = player(helper);
        var eye = player.getEyePosition();
        var near = target(helper, eye.add(0.7, -0.5, 3));
        target(helper, eye.add(0, -0.5, 5));
        var aim = PsychicDriveGauntletsRaycast.aim(player);
        helper.assertTrue(aim.target() == near, "Aim must select the nearest assisted target");
        var expected = PsychicDriveGauntletsRaycast.intersection(near.getBoundingBox().inflate(0.5),
                eye, eye.add(0, 0, 6));
        helper.assertTrue(expected != null && aim.point().distanceTo(expected) < 1.0e-6,
                "Aim must use the inflated intersection rather than the target center");
        helper.assertTrue(near.getBoundingBox().clip(eye, eye.add(0, 0, 6)).isEmpty(),
                "Assisted target must miss the uninflated center ray");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aimStopsAtWallIncludingInflatedOverhang(GameTestHelper helper) {
        var player = player(helper);
        var eye = player.getEyePosition();
        var wall = BlockPos.containing(eye.add(0, 0, 2));
        helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        var behind = target(helper, new Vec3(eye.x, eye.y - 0.5, wall.getZ() + 1.5));
        var aim = PsychicDriveGauntletsRaycast.aim(player);
        helper.assertTrue(aim.target() == null && Math.abs(aim.point().z - wall.getZ()) < 1.0e-6,
                "Wall must stop aiming and exclude hidden targets");
        behind.setPos(eye.x, eye.y - 0.5, wall.getZ() + 0.6);
        helper.assertTrue(PsychicDriveGauntletsRaycast.aim(player).target() == null,
                "Inflated bounds protruding through a wall must not enable direct targeting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emptyAimAndEndpointRemainInRange(GameTestHelper helper) {
        var player = player(helper);
        // テスト構造物の天井・周囲のentityへ届かない上空で、空の射程を確認する。
        player.setPos(player.position().add(0, 16, 0));
        player.setXRot(-90);
        var eye = player.getEyePosition();
        var aim = PsychicDriveGauntletsRaycast.aim(player);
        helper.assertTrue(aim.target() == null && aim.point().distanceTo(eye.add(0, 6, 0)) < 1.0e-6,
                "Empty aim must end six blocks from the eyes: " + aim + ", eye=" + eye + ", look=" + player.getLookAngle());
        var box = new AABB(0, 0, 6, 1, 1, 7);
        helper.assertTrue(new Vec3(0.5, 0.5, 6).equals(PsychicDriveGauntletsRaycast.intersection(
                box, new Vec3(0.5, 0.5, 0), new Vec3(0.5, 0.5, 6))),
                "A contact exactly at the endpoint must be included");
        target(helper, eye.add(0, 7.5, 0));
        helper.assertTrue(PsychicDriveGauntletsRaycast.aim(player).target() == null,
                "Targets beyond the assisted range must not be selected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clickSelectionAndRepeatHaveSeparateTiming(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.input(player, Input.PRESS, 0) && !state.right().available() && state.left().available(),
                "First click must launch the right arm");
        helper.assertTrue(!state.input(player, Input.PRESS, 0), "Duplicate press must not launch the left arm");
        state.input(player, Input.RELEASE, 0);
        helper.assertTrue(state.input(player, Input.PRESS, 0) && !state.left().available(),
                "A distinct click must immediately launch the left arm");
        state.input(player, Input.RELEASE, 0);
        helper.assertTrue(!state.input(player, Input.PRESS, 0), "Busy arms must reject another attack");

        var held = new PsychicDriveGauntletsAttackState();
        held.input(player, Input.PRESS, 0);
        for (int time = 1; time <= 7; time++) {
            held.tick(player, time);
        }
        helper.assertTrue(held.input(player, Input.REPEAT, 7) && !held.right().available() && !held.left().available(),
                "The seven-tick repeat must use the other arm while the right arm returns");
        for (int time = 8; time <= 14; time++) held.tick(player, time);
        helper.assertTrue(held.right().available(), "The first arm must return before the next seven-tick repeat");
        helper.assertTrue(held.input(player, Input.REPEAT, 14) && !held.right().available(),
                "The next seven-tick repeat must reuse the returned right arm");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repeatsUseClientTimingAndKeepServerArmLimits(GameTestHelper helper) {
        var player = player(helper);
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(!state.input(player, Input.REPEAT, 0), "Repeat must require an active press");
        helper.assertTrue(state.input(player, Input.PRESS, 0), "Press must launch the right arm immediately");
        for (int time = 1; time <= 6; time++) state.tick(player, time);
        helper.assertTrue(state.input(player, Input.REPEAT, 6) && !state.left().available(),
                "A client repeat before seven server ticks must use the available left arm");
        for (int attempt = 0; attempt < 20; attempt++) {
            helper.assertTrue(!state.input(player, Input.REPEAT, 6), "Busy arms must reject repeated requests");
        }
        for (int time = 7; time <= 20; time++) state.tick(player, time);
        helper.assertTrue(state.right().available() && state.left().available(), "Both arms must return");
        helper.assertTrue(state.input(player, Input.REPEAT, 20), "Server must not add a repeat interval");
        state.input(player, Input.RELEASE, 20);
        helper.assertTrue(!state.input(player, Input.REPEAT, 20), "Released input must reject repeats");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missedFlightFollowsThroughThenReturnsInFiveTicks(GameTestHelper helper) {
        var player = player(helper);
        var arm = new PsychicDriveGauntletsArm(true);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var destination = start.add(0, 0, 6);
        arm.launch(player, destination, 1, 0);
        arm.tick(player, 1, 1);
        helper.assertTrue(Math.abs(arm.position().distanceTo(start) - 4.0 / 3.0) < 1.0e-6,
                "Accelerating flight must advance four-thirds of a block on the first tick");
        ticks(arm, player, 2, 3);
        helper.assertTrue(arm.phase() == Phase.FOLLOW_THROUGH && arm.position().equals(destination),
                "An empty endpoint must start harmless follow-through");
        helper.assertTrue(arm.snapshot().impact() == Impact.NONE, "An empty endpoint must not trigger a strike");
        ticks(arm, player, 4, 6);
        helper.assertTrue(arm.phase() == Phase.RETURNING, "A miss must follow through for three ticks");
        ticks(arm, player, 7, 10);
        helper.assertTrue(!arm.available(), "Arm must remain unavailable until return completes");
        arm.tick(player, 1, 11);
        helper.assertTrue(arm.available() && arm.position().distanceTo(start) < 1.0e-6,
                "Return must finish in five ticks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void switchingStackRecallsWithoutRestartingReturn(GameTestHelper helper) {
        var player = player(helper);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, player.getEyePosition().add(0, 0, 6), 1, 0);
        arm.tick(player, 1, 1);
        var departure = arm.position();
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        arm.tick(player, 0.5F, 2);
        helper.assertTrue(arm.phase() == Phase.RETURNING && arm.position().equals(departure),
                "Switching even to an identical stack must start return without teleporting");
        helper.assertTrue(arm.snapshot().impact() == Impact.NONE,
                "Recalling before contact must not manufacture an impact");
        player.setPos(player.position().add(1, 0, 0));
        for (int time = 3; time <= 7; time++) arm.tick(player, 0.5F, time);
        helper.assertTrue(arm.available()
                        && arm.position().distanceTo(PsychicDriveGauntletsPose.position(player, 0.5F, true)) < 1.0e-6,
                "Return must follow the moving attachment and must not restart on every tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sweptContactDealsFullDamageOnceAndBypassesIframe(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var target = target(helper, start.add(0, -0.5, 1.2));
        player.resetAttackStrengthTicker();
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, start.add(0, 0, 6), 1, 0);
        arm.tick(player, 1, 1);
        helper.assertTrue(arm.phase() == Phase.HOLDING && Math.abs(target.getHealth() - 96) < 1.0e-6,
                "Swept contact must deal full damage four without attack-strength scaling");
        var second = new PsychicDriveGauntletsArm(true);
        second.launch(player, start.add(0, 0, 6), 1, 1);
        second.tick(player, 1, 2);
        helper.assertTrue(second.phase() == Phase.HOLDING && Math.abs(target.getHealth() - 92) < 1.0e-6,
                "The second arm must bypass iframes and deal another four damage");
        helper.assertTrue(arm.snapshot().impact() == Impact.ENTITY && arm.snapshot().impactTick() == 1
                        && second.snapshot().impact() == Impact.ENTITY && second.snapshot().impactTick() == 2,
                "Both successful hits must record entity contact at the actual tick");
        ticks(arm, player, 2, 18);
        helper.assertTrue(Math.abs(target.getHealth() - 92) < 1.0e-6,
                "Holding and return must not deal additional damage");
        var type = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.PSYCHIC_DRIVE_GAUNTLETS);
        helper.assertTrue(type.is(DamageTypeTags.BYPASSES_COOLDOWN), "Gauntlet damage must bypass normal iframes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageUsesLaunchSnapshotEnchantments(GameTestHelper helper) {
        var player = player(helper);
        var sharpness = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SHARPNESS);
        player.getMainHandItem().enchant(sharpness, 3);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var target = target(helper, start.add(0, -0.5, 1.2));
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, start.add(0, 0, 6), 1, 0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(40);
        player.getMainHandItem().enchant(sharpness, 5);
        arm.tick(player, 1, 1);
        helper.assertTrue(Math.abs(target.getHealth() - 94) < 1.0e-6,
                "Damage must use launch attack damage and sharpness three from the copied weapon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void flightAllowsEmbeddedArmAndPostLaunchOcclusion(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var destination = start.add(0, 0, 6);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, destination, 1, 0);
        var wall = BlockPos.containing(start.add(0, 0, 1));
        helper.getLevel().setBlockAndUpdate(BlockPos.containing(start), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        var target = target(helper, start.add(0, -0.5, 4));
        ticks(arm, player, 1, 3);
        helper.assertTrue(arm.phase() == Phase.HOLDING && Math.abs(target.getHealth() - 96) < 1.0e-6,
                "An embedded arm must fly through blocks and hit a target hidden after launch");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offhandAndVanillaAttackAreSuppressedCorrectly(GameTestHelper helper) {
        var player = player(helper);
        var target = target(helper, player.getEyePosition().add(0, -0.5, 1));
        var attack = new AttackEntityEvent(player, target);
        PsychicDriveGauntletsAttackEvents.attackEntity(attack);
        helper.assertTrue(attack.isCanceled(), "Mainhand gauntlets must cancel vanilla entity attack");
        var block = new PlayerInteractEvent.LeftClickBlock(player, player.blockPosition(), Direction.UP,
                PlayerInteractEvent.LeftClickBlock.Action.START);
        PsychicDriveGauntletsAttackEvents.leftClickBlock(block);
        helper.assertTrue(block.isCanceled(), "Mainhand gauntlets must cancel block breaking");
        player.setItemInHand(InteractionHand.OFF_HAND, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(!state.input(player, Input.PRESS, 0), "Offhand-only gauntlets must not launch");
        var normal = new AttackEntityEvent(player, target);
        PsychicDriveGauntletsAttackEvents.attackEntity(normal);
        helper.assertTrue(!normal.isCanceled(), "Offhand-only gauntlets must not suppress the mainhand weapon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void allyProtectionAppliesToAimAndFlight(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var ally = target(helper, start.add(0, -0.5, 1.2));
        var enemy = target(helper, start.add(0, -0.5, 3.5));
        var scoreboard = helper.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("gauntlet_" + player.getId());
        team.setAllowFriendlyFire(false);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        try {
            helper.assertTrue(!CombatTools.isValidCombatTarget(player, player), "Owner must not be a valid combat target");
            helper.assertTrue(PsychicDriveGauntletsRaycast.aim(player).target() == enemy,
                    "Aim must skip protected allies");
            var arm = new PsychicDriveGauntletsArm(true);
            arm.launch(player, start.add(0, 0, 6), 1, 0);
            ticks(arm, player, 1, 3);
            helper.assertTrue(ally.getHealth() == 100 && Math.abs(enemy.getHealth() - 96) < 1.0e-6,
                    "Flight must pass through an ally and damage the next valid target");
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void armorStillReducesGauntletDamage(GameTestHelper helper) {
        var player = player(helper);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var target = target(helper, start.add(0, -0.5, 1.2));
        target.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, start.add(0, 0, 6), 1, 0);
        arm.tick(player, 1, 1);
        helper.assertTrue(target.getHealth() > 96 && target.getHealth() < 100,
                "Gauntlet attack damage must remain subject to target armor");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void betterCombatDoesNotSupplyMeleeAttributes(GameTestHelper helper) throws ReflectiveOperationException {
        if (ModList.get().isLoaded("bettercombat")) {
            var registry = Class.forName("net.bettercombat.logic.WeaponRegistry");
            var attributes = registry.getMethod("getAttributes", ItemStack.class).invoke(null,
                    new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
            helper.assertTrue(attributes == null, "Better Combat must defer gauntlet input to the custom attack");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void epicFightBlocksOnlyGauntletBasicAttack(GameTestHelper helper) {
        if (ModList.get().isLoaded("epicfight")) PsychicDriveGauntletsEpicFightTestHelper.check(helper);
        else helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blockImpactRequiresArrivalAndEntityContactTakesPriority(GameTestHelper helper) {
        var player = player(helper);
        var wall = BlockPos.containing(player.getEyePosition().add(0, 0, 2));
        helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        var state = new PsychicDriveGauntletsAttackState();
        helper.assertTrue(state.input(player, Input.PRESS, 100), "Wall attack must launch");
        helper.assertTrue(state.right().snapshot().impact() == Impact.NONE,
                "A block aim must not count as impact before arrival");
        for (int time = 101; (state.right().phase() == Phase.PREPARING || state.right().phase() == Phase.OUTBOUND) && time <= 104; time++) state.tick(player, time);
        var impact = state.right().snapshot();
        helper.assertTrue(impact.phase() == Phase.HOLDING && impact.impact() == Impact.BLOCK
                        && impact.impactTick() > 100 && impact.position().equals(impact.destination()),
                "Reaching the aimed wall must record a block impact");

        var aim = PsychicDriveGauntletsRaycast.aim(player);
        helper.assertTrue(aim.block(), "The wall must remain the selected endpoint");
        var arm = new PsychicDriveGauntletsArm(true);
        arm.launch(player, aim, 1, 200, InteractionHand.MAIN_HAND, 0);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        target(helper, start.add(0, -0.5, 0.8));
        ticks(arm, player, 201, 204);
        helper.assertTrue(arm.snapshot().impact() == Impact.ENTITY,
                "An entity entering a block-directed flight must take priority over the block endpoint");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void packetsPreserveIndependentArmsAndExplicitReset(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var right = new Snapshot(Phase.RETURNING, new Vec3(1.25, -2, 3), 179, -30, 0.5F,
                    new Vec3(0, 1, 2), new Vec3(3, 4, 5), 10, 12, 0, 12, 0, Impact.ENTITY, 11);
            var left = new Snapshot(Phase.HOLDING, new Vec3(-4, 5, 6.75), -179, 45, 1,
                    new Vec3(-1, -2, -3), new Vec3(-4, -5, -6), 11, 13, 0, 0, 0, Impact.BLOCK, 13);
            var id = UUID.randomUUID();
            for (boolean reset : new boolean[] {false, true}) {
                var packet = new SyncPsychicDriveGauntletsPacket(id, reset, right, left);
                SyncPsychicDriveGauntletsPacket.STREAM_CODEC.encode(buffer, packet);
                helper.assertTrue(packet.equals(SyncPsychicDriveGauntletsPacket.STREAM_CODEC.decode(buffer)),
                        "State packets must preserve both arms, player identity, orientation, and reset semantics");
            }
            for (var input : Input.values()) {
                var packet = new ClientPsychicDriveGauntletsPacket(input);
                ClientPsychicDriveGauntletsPacket.STREAM_CODEC.encode(buffer, packet);
                helper.assertTrue(packet.equals(ClientPsychicDriveGauntletsPacket.STREAM_CODEC.decode(buffer)),
                        "Input packets must distinguish press, repeat, and release");
            }
            helper.assertTrue(buffer.readableBytes() == 0, "Packet codecs must consume their complete payload");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
