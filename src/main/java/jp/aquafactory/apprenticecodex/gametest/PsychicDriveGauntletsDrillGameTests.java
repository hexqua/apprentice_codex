package jp.aquafactory.apprenticecodex.gametest;

import java.util.Objects;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsPose;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsRaycast;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused") // 多段の接触と中断境界をserver側で固定する。
public final class PsychicDriveGauntletsDrillGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private PsychicDriveGauntletsDrillGameTests() {}

    private static FakePlayer player(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper,
                new BlockPos(1, 18, 1), "psychic_drill");
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.PSYCHIC_DRIVE_GAUNTLETS.get()));
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(4);
        player.setYRot(0);
        player.setXRot(0);
        player.yBodyRot = 0;
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

    private static PsychicDriveGauntletsArm launch(FakePlayer player, float charge) {
        var arm = new PsychicDriveGauntletsArm(true);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        arm.launch(player, start.add(0, 0, 12), 1, 0, InteractionHand.MAIN_HAND, charge);
        arm.tick(player, 1, 1);
        return arm;
    }

    private static void ticks(PsychicDriveGauntletsArm arm, FakePlayer player, int from, int to) {
        for (int tick = from; tick <= to; tick++) arm.tick(player, 1, tick);
    }

    private static void health(GameTestHelper helper, Cow target, float expected) {
        helper.assertTrue(Math.abs(target.getHealth() - expected) < 1.0e-5,
                "Unexpected target health: " + target.getHealth() + " != " + expected);
    }

    @GameTest(template = TEMPLATE)
    public static void fourHitsUseExactTimingSnapshotDamageAndShortSlow(GameTestHelper helper) {
        var player = player(helper);
        var sharpness = Enchantments.SHARPNESS;
        player.getMainHandItem().enchant(sharpness, 3);
        var start = PsychicDriveGauntletsPose.position(player, 1, true);
        var cow = target(helper, start.add(0, -0.5, 2));
        var arm = launch(player, 1);
        var impact = arm.position();
        Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(40);
        player.getMainHandItem().enchant(sharpness, 5);
        // 効果の経過はworld tickに依存するため、この同期テストでは付与時の長さを検査する。
        for (int tick = 1; tick <= 13; tick++) {
            if (tick > 1) arm.tick(player, 1, tick);
            int hits = 1 + (tick - 1) / 4;
            health(helper, cow, 100 - hits * 6);
            helper.assertTrue(Math.abs(PsychicDriveGauntletsHudState.sample(arm.snapshot(), 0).recovery()
                    - (tick - 1) / 17F) < 1.0e-6, "Drill recovery must span twelve hold and five return ticks");
            if (tick == 1 || tick == 5 || tick == 9) {
                var slow = Objects.requireNonNull(cow.getEffect(MobEffects.MOVEMENT_SLOWDOWN));
                helper.assertTrue(slow.getAmplifier() == 4 && slow.getDuration() == 5,
                        "Each non-final hit must apply Slowness V for five ticks");
                cow.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            if (tick < 13) helper.assertTrue(cow.getDeltaMovement().lengthSqr() < 1.0e-12,
                    "Non-final hits must suppress ordinary and extra knockback");
        }
        helper.assertTrue(!cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Final hit must not refresh slow");
        helper.assertTrue(arm.phase() == Phase.RETURNING && arm.snapshot().holdTicks() == 12,
                "Final hit must immediately return after twelve ticks");
        helper.assertTrue(Math.abs(arm.position().distanceTo(impact) - 0.2) < 1.0e-6,
                "The fist must advance exactly 0.2 blocks without following the target");
        helper.assertTrue(cow.getDeltaMovement().z >= 0.49, "The final hit must push forward");
        ticks(arm, player, 14, 18);
        health(helper, cow, 76);
        helper.assertTrue(arm.available(), "The drill must return in five ticks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void departureEndsDrillAtNextHitAndPreservesRecovery(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 1);
        cow.setPos(cow.position().add(3, 0, 0));
        ticks(arm, player, 2, 4);
        helper.assertTrue(arm.phase() == Phase.HOLDING, "Departure between hits must not immediately recall");
        arm.tick(player, 1, 5);
        helper.assertTrue(arm.phase() == Phase.RETURNING, "An empty hit must terminate the drill");
        health(helper, cow, 96);
        helper.assertTrue(Math.abs(PsychicDriveGauntletsHudState.sample(arm.snapshot(), 0).recovery() - 4 / 17F) < 1.0e-6,
                "Early return must preserve four ticks of drill recovery");
        ticks(arm, player, 6, 10);
        helper.assertTrue(arm.available(), "Early return must finish in five ticks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void establishedDrillIgnoresOcclusionIncludingReplacementTargets(GameTestHelper helper) {
        var player = player(helper);
        var first = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 1);
        health(helper, first, 96);
        // 初発後に目からの視線を塞いでも、成立済みの拳の接触判定は継続する。
        var wall = BlockPos.containing(player.getEyePosition().add(0, 0, 1));
        for (int x = -1; x <= 1; x++) {
            helper.getLevel().setBlockAndUpdate(wall.offset(x, 0, 0), Blocks.STONE.defaultBlockState());
        }
        player.setPos(player.position().add(0, 0, -2));
        helper.assertTrue(PsychicDriveGauntletsRaycast.firstContact(player, arm.position(), arm.position(),
                        player.getEyePosition()) == null,
                "The established drill fixture must be occluded from the retreated player");
        ticks(arm, player, 2, 5);
        health(helper, first, 92);
        var replacement = target(helper, first.position());
        first.setPos(first.position().add(3, 0, 0));
        ticks(arm, player, 6, 13);
        health(helper, replacement, 92);
        helper.assertTrue(arm.phase() == Phase.RETURNING,
                "Occlusion and replacement must preserve the existing four-hit drill schedule");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void departureAndReentryBetweenHitsRemainHittable(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var original = cow.position();
        var arm = launch(player, 1);
        cow.setPos(original.add(3, 0, 0));
        ticks(arm, player, 2, 4);
        cow.setPos(original);
        arm.tick(player, 1, 5);
        health(helper, cow, 92);
        helper.assertTrue(arm.phase() == Phase.HOLDING, "Returning before a hit must allow continuation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void previousTargetWinsThenDepartureAndDeathAllowReplacement(GameTestHelper helper) {
        var player = player(helper);
        var first = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 1);
        var second = target(helper, arm.position().add(0, -0.5, 0.2));
        ticks(arm, player, 2, 5);
        health(helper, first, 92);
        health(helper, second, 100);
        first.setPos(first.position().add(3, 0, 0));
        ticks(arm, player, 6, 9);
        health(helper, second, 96);
        var third = target(helper, second.position());
        second.discard();
        ticks(arm, player, 10, 13);
        health(helper, third, 96);
        helper.assertTrue(arm.phase() == Phase.RETURNING, "Replacement must not restart or extend four hits");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedHitStopsWithoutTryingOtherTargets(GameTestHelper helper) {
        var player = player(helper);
        var first = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 1);
        var other = target(helper, first.position());
        first.setInvulnerable(true);
        ticks(arm, player, 2, 5);
        health(helper, first, 96);
        health(helper, other, 100);
        helper.assertTrue(arm.phase() == Phase.RETURNING, "A rejected preferred hit must stop without fallback");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedFirstHitReturnsImmediately(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        cow.setInvulnerable(true);
        var arm = launch(player, 1);
        health(helper, cow, 100);
        helper.assertTrue(arm.phase() == Phase.RETURNING && !cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "A rejected initial hit must return immediately without slow");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shieldBlocksFromFistPositionAndTerminatesDrill(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 1);
        cow.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHIELD));
        cow.setYRot(180);
        cow.startUsingItem(InteractionHand.MAIN_HAND);
        // 盾の使用開始直後の猶予を実際のLivingEntity tickで進める。
        var original = cow.position();
        for (int tick = 0; tick < 6; tick++) cow.tick();
        cow.setPos(original);
        helper.assertTrue(cow.isBlocking(), "The target fixture must have an active shield");
        ticks(arm, player, 2, 5);
        health(helper, cow, 96);
        helper.assertTrue(arm.phase() == Phase.RETURNING, "Shield rejection must terminate the drill");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void allyAndSourceChangesStopFurtherDamage(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 1);
        var board = helper.getLevel().getScoreboard();
        var team = board.addPlayerTeam("drill_" + player.getId());
        team.setAllowFriendlyFire(false);
        try {
            board.addPlayerToTeam(player.getScoreboardName(), team);
            board.addPlayerToTeam(cow.getScoreboardName(), team);
            ticks(arm, player, 2, 5);
            health(helper, cow, 96);
            helper.assertTrue(arm.phase() == Phase.RETURNING, "New allies must be excluded at every hit");
        } finally { board.removePlayerTeam(team); }
        var next = launch(player, 1);
        health(helper, cow, 92);
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem().copy());
        next.tick(player, 1, 2);
        helper.assertTrue(next.phase() == Phase.RETURNING, "Replacing the weapon must immediately recall");
        ticks(next, player, 3, 7);
        health(helper, cow, 92);
        helper.assertTrue(next.available(), "Source changes must not restart the return");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void partialChargeAndBlockContactUseShortHold(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        var arm = launch(player, 0.95F);
        ticks(arm, player, 2, 3);
        health(helper, cow, 96);
        helper.assertTrue(arm.phase() == Phase.RETURNING && !cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Partial charge must deal one normal hit and hold only two ticks");
        cow.discard();
        for (float charge : new float[] {0, 1}) {
            var block = new PsychicDriveGauntletsArm(true);
            var start = PsychicDriveGauntletsPose.position(player, 1, true);
            block.launch(player, new PsychicDriveGauntletsRaycast.Aim(player.getEyePosition(), start.add(0, 0, 2), true),
                    1, 100, InteractionHand.MAIN_HAND, charge);
            for (int tick = 101; block.phase() == Phase.OUTBOUND && tick <= 103; tick++) block.tick(player, 1, tick);
            helper.assertTrue(block.phase() == Phase.HOLDING, "Block contact must first enter holding");
            long arrived = block.snapshot().sampleTick();
            block.tick(player, 1, arrived + 1);
            helper.assertTrue(block.phase() == Phase.HOLDING, "Block hold must last two full ticks");
            block.tick(player, 1, arrived + 2);
            helper.assertTrue(block.phase() == Phase.RETURNING && block.snapshot().holdTicks() == 2,
                    "Block contact must hold only two ticks regardless of charge");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void strongerSlowIsPreservedAndSelectionUsesDistanceThenId(GameTestHelper helper) {
        var player = player(helper);
        var first = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        first.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5));
        var arm = launch(player, 1);
        var slow = Objects.requireNonNull(first.getEffect(MobEffects.MOVEMENT_SLOWDOWN));
        helper.assertTrue(slow.getAmplifier() == 5 && slow.getDuration() == 100,
                "The drill must not replace stronger existing slow");
        first.discard();
        var lowerId = target(helper, arm.position().add(0, -0.5, 0.1));
        var higherId = target(helper, lowerId.position());
        helper.assertTrue(PsychicDriveGauntletsRaycast.drillTarget(player, arm.position(), first) == lowerId,
                "Equal-distance contacts must use entity ID order");
        lowerId.setPos(lowerId.position().add(0.8, 0, 0));
        helper.assertTrue(PsychicDriveGauntletsRaycast.drillTarget(player, arm.position(), null) == higherId,
                "A closer contact must win before entity ID");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void slowExpiresAfterFiveLivingTicks(GameTestHelper helper) {
        var player = player(helper);
        var cow = target(helper, PsychicDriveGauntletsPose.position(player, 1, true).add(0, -0.5, 2));
        launch(player, 1);
        for (int tick = 1; tick <= 5; tick++) {
            cow.tick();
            helper.assertTrue(cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) == (tick < 5),
                    "Drill slow must expire after five living ticks without another hit");
        }
        health(helper, cow, 96);
        helper.succeed();
    }
}
