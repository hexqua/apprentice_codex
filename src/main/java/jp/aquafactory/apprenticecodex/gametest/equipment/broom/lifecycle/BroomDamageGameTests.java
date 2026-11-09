package jp.aquafactory.apprenticecodex.gametest.equipment.broom.lifecycle;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.player;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomDamageGameTests {
    private BroomDamageGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void damageScalesAndRecoversAtTenTickIntervals(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_damage_recovery");
            var source = helper.getLevel().damageSources().playerAttack(player);

            broom.hurt(source, 1.0F);
            helper.assertTrue(broom.getDamage() == 50,
                    "One point of incoming damage should add fifty broom damage");
            // Entity.tickの手動呼び出しではLevel側のtickCount更新が入らないため、境界値を明示する。
            broom.tickCount = 9;
            broom.tick();
            helper.assertTrue(broom.getDamage() == 50,
                    "Damage must not recover before the ten tick interval");
            broom.tickCount = 10;
            broom.tick();
            helper.assertTrue(broom.getDamage() == 0,
                    "The configured amount should recover at the ten tick interval");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void damageIFrameIncludesAcceptedTickAndReopensAtTickTen(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var source = helper.getLevel().damageSources().generic();

        helper.assertTrue(broom.hurt(source, 1.0F), "The first damage hit should be accepted");
        helper.assertFalse(broom.hurt(source, 1.0F),
                "The accepted tick itself should be inside the broom damage i-frame");

        helper.runAfterDelay(9, () -> helper.assertFalse(broom.hurt(source, 1.0F),
                "A normal hit at T+9 should still be rejected"));
        helper.runAfterDelay(10, () -> {
            var damageBeforeHit = broom.getDamage();
            helper.assertTrue(broom.hurt(source, 1.0F), "A normal hit at T+10 should be accepted");
            helper.assertTrue(broom.getDamage() == damageBeforeHit + 50,
                    "The T+10 hit should apply the configured damage conversion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void damageIFrameZeroSettingDisablesRejection(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 0, 0, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var source = helper.getLevel().damageSources().generic();

            helper.assertTrue(broom.hurt(source, 1.0F), "The first hit should be accepted");
            helper.assertTrue(broom.hurt(source, 1.0F), "A zero-tick i-frame should accept a same-tick hit");
            helper.assertTrue(broom.getDamage() == 100,
                    "Disabling the i-frame should apply both same-tick hits");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ignoredAndZeroDamageDoNotInteractWithDamageIFrame(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(
                1000, 0, 10, Set.of(DamageTypes.GENERIC.location()), 100, 50,
                1.0D, 1.0D, 1.5D
        );
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_iframe_owner");
            var ignoredSource = helper.getLevel().damageSources().generic();
            var normalSource = helper.getLevel().damageSources().playerAttack(player);

            helper.assertTrue(broom.hurt(normalSource, 0.01F),
                    "A hit converted to zero broom damage should retain the existing hurt result");
            helper.assertTrue(broom.hurt(ignoredSource, 1.0F),
                    "A configured ignored damage type should pass without starting an i-frame");
            helper.assertTrue(broom.hurt(normalSource, 1.0F),
                    "Normal damage should still be accepted after zero and ignored damage");
            helper.assertTrue(broom.hurt(ignoredSource, 1.0F),
                    "Ignored damage should pass through an active i-frame");
            helper.assertFalse(broom.hurt(normalSource, 1.0F),
                    "Ignored damage must not clear the active normal-damage i-frame");
            helper.assertTrue(broom.getDamage() == 150,
                    "Only the three positive accepted hits should change broom damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageIFrameIsNotPersisted(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 0, 10, Set.of(), 100, 50,
                1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var source = helper.getLevel().damageSources().generic();
            var broom = spawnBroom(helper, 1.5D);
            broom.hurt(source, 1.0F);
            var saved = new CompoundTag();
            broom.saveWithoutId(saved);

            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.hurt(source, 1.0F),
                    "Loading should not restore the volatile damage i-frame");
            helper.assertTrue(loaded.getDamage() == 100,
                    "The loaded broom should retain damage while accepting a new hit immediately");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void lavaDamagesBroomWithoutPersistentFire(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnBroom(helper, 0.2D);

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(broom.getDamage() > 0, "Lava contact should still damage the broom");
            helper.assertFalse(broom.isOnFire(), "The broom should clear persistent fire after contact damage");
            helper.assertTrue(broom.getRemainingFireTicks() <= 0,
                    "Persistent fire ticks should not remain on the broom");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void firewardCalibratedBroomsIgnoreFireDamageAndLavaContact(GameTestHelper helper) {
        var level = helper.getLevel();
        var firstLava = helper.absolutePos(TEST_POS);
        var secondLava = helper.absolutePos(TEST_POS.offset(2, 0, 0));
        var floatmount = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        var hoverride = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
        var brooms = List.of((AbstractBroomEntity) floatmount, hoverride);
        var broomItems = List.of(ItemRegistry.FLOATMOUNT_BROOM.get(), ItemRegistry.HOVERRIDE_BROOM.get());
        var lavaPositions = List.of(firstLava, secondLava);

        for (var index = 0; index < brooms.size(); ++index) {
            var broom = brooms.get(index);
            var broomStack = new ItemStack(broomItems.get(index));
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            broomStack,
                            0,
                            new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIREWARD_RING.get())
                    ),
                    "Broom fire immunity test should accept its Fireward Ring calibration");
            broom.setBroomItemStack(broomStack);

            var lava = lavaPositions.get(index);
            level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
            broom.setPos(lava.getX() + 0.5D, lava.getY() + 0.2D, lava.getZ() + 0.5D);
            level.addFreshEntity(broom);
            broom.setRemainingFireTicks(100);
            helper.assertFalse(broom.hurt(level.damageSources().lava(), 1.0F),
                    "Fireward-calibrated broom should reject direct lava damage");
            helper.assertTrue(broom.getRemainingFireTicks() <= 0,
                    "Rejecting fire damage should extinguish a Fireward-calibrated broom immediately");
        }

        helper.runAfterDelay(5, () -> {
            for (var broom : brooms) {
                helper.assertTrue(broom.getDamage() == 0,
                        "Fireward-calibrated broom should take no damage from lava contact");
                helper.assertFalse(broom.isOnFire(),
                        "Fireward-calibrated broom should not remain on fire in lava");
                helper.assertTrue(broom.hurt(level.damageSources().generic(), 1.0F),
                        "Fireward calibration should not reject non-fire damage");
                helper.assertTrue(broom.getDamage() > 0,
                        "Non-fire damage should still damage a Fireward-calibrated broom");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void combatToolsIgnoresVehiclesAndTargetsOnlyPvpHarmableRiders(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        helper.assertTrue(server != null, "Floatmount Broom PvP test requires a server");
        var attacker = new ServerPlayer(server, level,
                new GameProfile(UUID.randomUUID(), "broom_pvp_attacker"));
        // 降車時のteleport処理も通るため、騎乗者には接続を備えたGameTestのmock playerを使う。
        var rider = serverRider(helper);
        var broom = spawnBroom(helper, 1.5D);
        var boat = new Boat(level, broom.getX() + 2.0D, broom.getY(), broom.getZ());
        level.addFreshEntity(boat);
        var scoreboard = level.getScoreboard();
        var team = scoreboard.addPlayerTeam("broom_pvp_policy");
        var previousPvp = server.isPvpAllowed();

        try {
            server.setPvpAllowed(true);
            helper.assertTrue(rider.startRiding(broom, true), "PvP target rider should mount the broom");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "A ridden broom should remain excluded as a non-living vehicle");
            helper.assertFalse(CombatTools.isValidCombatTarget(boat, attacker),
                    "A vanilla boat should remain excluded as a non-living vehicle");
            helper.assertTrue(CombatTools.isValidCombatTarget(rider, attacker),
                    "An enemy rider should be a combat target while PvP is enabled");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, rider),
                    "A rider must not target their own root vehicle");
            helper.assertFalse(CombatTools.isValidCombatTarget(rider, rider),
                    "A rider must not target themselves");

            scoreboard.addPlayerToTeam(attacker.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(rider.getScoreboardName(), team);
            team.setAllowFriendlyFire(false);
            helper.assertFalse(CombatTools.isValidCombatTarget(rider, attacker),
                    "Friendly-fire protection should protect the rider");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "Friendly-fire settings must not make the non-living broom targetable");
            team.setAllowFriendlyFire(true);
            helper.assertTrue(CombatTools.isValidCombatTarget(rider, attacker),
                    "Friendly-fire-enabled teammates should expose the rider");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "A ridden broom should remain excluded when friendly fire is enabled");

            scoreboard.removePlayerFromTeam(attacker.getScoreboardName(), team);
            scoreboard.removePlayerFromTeam(rider.getScoreboardName(), team);
            server.setPvpAllowed(false);
            helper.assertFalse(CombatTools.isValidCombatTarget(rider, attacker),
                    "Server PvP disablement should protect the rider");
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "Server PvP settings must not make the non-living broom targetable");

            server.setPvpAllowed(true);
            rider.stopRiding();
            helper.assertFalse(CombatTools.isValidCombatTarget(broom, attacker),
                    "An unoccupied broom should remain excluded as a non-living vehicle");
        } finally {
            rider.stopRiding();
            server.setPvpAllowed(previousPvp);
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void maximumDamageIsPersistentAndPreventsMounting(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = serverRider(helper);
            helper.assertTrue(player.startRiding(broom, true), "Damage test player should mount directly");
            broom.hurt(helper.getLevel().damageSources().playerAttack(player), 20.0F);

            helper.assertTrue(broom.isDamaged(), "Maximum damage should enter the damaged state");
            helper.assertFalse(broom.isRemoved(), "Normal damage must not itemize the broom");
            helper.assertTrue(player.getVehicle() == broom, "Damage must not eject the current rider immediately");
            helper.assertTrue(broom.isForcedLanding(), "Damaged broom must enter forced landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Damaged broom must always require dismount confirmation");
            helper.assertTrue(broom.getDamage() == broom.getMaxDamage(),
                    "Damaged broom should remain at maximum damage");
            for (var tick = 0; tick < 20; tick++) {
                broom.tick();
            }
            helper.assertTrue(broom.getDamage() == broom.getMaxDamage(),
                    "Damaged state must disable natural recovery");
            player.stopRiding();
            helper.assertFalse(player.startRiding(broom), "Damaged broom must reject riders");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isDamaged(), "Damaged state must persist in entity NBT");
            helper.assertTrue(loaded.getDamage() == loaded.getMaxDamage(),
                    "Loaded damaged broom should retain maximum damage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damagedBroomItemizesOnlyBelowWorldBottomRegardlessOfDamageSource(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var expectedName = Component.literal("Void Survivor").withStyle(ChatFormatting.LIGHT_PURPLE);
            var broom = spawnBroom(helper, 1.5D);
            broom.setCustomName(expectedName);
            broom.setCustomNameVisible(true);
            broom.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20.0F);
            helper.assertTrue(broom.isDamaged(), "Void damage may damage the broom like any other source");
            helper.assertFalse(broom.isRemoved(),
                    "Void damage source alone must never be used as the itemization condition");

            broom.setPos(broom.getX(), helper.getLevel().getMinBuildHeight() - 1.0D, broom.getZ());
            var dropPos = broom.position();
            broom.tick();
            helper.assertTrue(broom.isRemoved(), "Damaged broom below world bottom should itemize");
            helper.assertTrue(countDroppedBrooms(helper, dropPos) == 1,
                    "World-bottom itemization should drop exactly one fresh broom");
            var dropped = findDroppedBroom(helper, dropPos).getItem();
            helper.assertTrue(expectedName.equals(dropped.getHoverName()),
                    "World-bottom itemization should preserve the broom custom name");
        }
        helper.succeed();
    }

    static int countDroppedBrooms(GameTestHelper helper, Vec3 center) {
        return droppedBrooms(helper, center)
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }

    static ItemEntity findDroppedBroom(GameTestHelper helper, Vec3 center) {
        var drops = droppedBrooms(helper, center);
        helper.assertTrue(drops.size() == 1, "Expected exactly one dropped broom item entity");
        return drops.get(0);
    }

    static List<ItemEntity> droppedBrooms(GameTestHelper helper,
                                                             Vec3 center) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(center, 4.0D, 4.0D, 4.0D),
                item -> item.getItem().is(ItemRegistry.FLOATMOUNT_BROOM.get()));
    }
}
