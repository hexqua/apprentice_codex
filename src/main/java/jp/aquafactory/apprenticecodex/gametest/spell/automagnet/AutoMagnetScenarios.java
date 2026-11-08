package jp.aquafactory.apprenticecodex.gametest.spell.automagnet;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetCollectionMode;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarEntity;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetFamiliarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.autocastamulet.AutocastAmuletTestSupport.getOwnedAutoMagnetFamiliars;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class AutoMagnetScenarios {
    private AutoMagnetScenarios() {}

    static void autoMagnetCollectsItemsWithoutSolegnoliaBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_magnet_owner_test");
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 4.0, 0.0);
        var item = new ItemEntity(level, owner.getX() + 2.0, owner.getY(), owner.getZ(), new ItemStack(Items.IRON_INGOT));
        level.addFreshEntity(owner);
        level.addFreshEntity(item);

        helper.runAtTickTime(1, () -> {
            familiar.tickOnServer(level);
            helper.assertFalse(item.isRemoved(), "AutoMagnet test item should remain as an entity before player pickup");
            helper.assertTrue(item.position().distanceToSqr(owner.position()) <= 0.001,
                    "AutoMagnet should collect items when no Solegnolia blocks it");
            helper.succeed();
        });
    }

    static void autoMagnetFamiliarUsesTransientAlwaysTickingLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_magnet_lifecycle_test");
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 4.0, 0.0);

        helper.assertTrue(familiar.isAlwaysTicking(),
                "AutoMagnet familiar should remain ticking across chunk visibility changes");
        helper.assertFalse(familiar.shouldBeSaved(),
                "AutoMagnet familiar should be reconstructed from player spell state");
        helper.succeed();
    }

    static void autoMagnetNormalModeCollectsWhileStanding(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.NORMAL, false, true,
                "auto_magnet_normal_standing_test");
    }

    static void autoMagnetNormalModeStopsWhileCrouching(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.NORMAL, true, false,
                "auto_magnet_normal_crouching_test");
    }

    static void autoMagnetReverseModeStopsWhileStanding(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.REVERSE, false, false,
                "auto_magnet_reverse_standing_test");
    }

    static void autoMagnetReverseModeCollectsWhileCrouching(GameTestHelper helper) {
        assertAutoMagnetCollectionMode(helper, AutoMagnetCollectionMode.REVERSE, true, true,
                "auto_magnet_reverse_crouching_test");
    }

    static void autoMagnetRecastSwitchesModeAndStopsSameMode(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_magnet_recast_mode_test");
            level.addFreshEntity(owner);

            AutoMagnetFamiliarManager.activate(owner, 4.0D, 0.0D, AutoMagnetCollectionMode.NORMAL);
            var initialFamiliars = getOwnedAutoMagnetFamiliars(helper, owner);
            helper.assertTrue(initialFamiliars.size() == 1,
                    "AutoMagnet should spawn exactly one familiar before mode switch");
            var familiar = initialFamiliars.get(0);

            var switched = AutoMagnetFamiliarManager.toggle(owner, 6.0D, 0.0D, AutoMagnetCollectionMode.REVERSE);
            helper.assertTrue(switched, "AutoMagnet should switch mode instead of deactivating");
            var switchedFamiliars = getOwnedAutoMagnetFamiliars(helper, owner);
            helper.assertTrue(switchedFamiliars.size() == 1,
                    "AutoMagnet mode switch should keep exactly one familiar");
            helper.assertTrue(switchedFamiliars.get(0) == familiar,
                    "AutoMagnet mode switch should reuse the existing familiar");
            helper.assertTrue(familiar.getCollectionMode() == AutoMagnetCollectionMode.REVERSE,
                    "AutoMagnet familiar should use reverse mode after switch");
            helper.assertTrue(Math.abs(familiar.getPickupRange() - 6.0D) < 1.0E-4D,
                    "AutoMagnet mode switch should update the familiar range");

            var deactivated = AutoMagnetFamiliarManager.toggle(owner, 6.0D, 0.0D, AutoMagnetCollectionMode.REVERSE);
            helper.assertFalse(deactivated, "AutoMagnet same-mode recast should deactivate");
            helper.assertTrue(getOwnedAutoMagnetFamiliars(helper, owner).isEmpty(),
                    "AutoMagnet same-mode recast should discard the familiar");
            helper.succeed();
        });
    }

    private static void assertAutoMagnetCollectionMode(GameTestHelper helper, AutoMagnetCollectionMode mode,
                                                       boolean crouching, boolean shouldCollect, String profileName) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        setCrouchingForAutoMagnetTest(owner, crouching);
        var familiar = new AutoMagnetFamiliarEntity(EntityRegistry.AUTO_MAGNET_FAMILIAR.get(), level, owner, 4.0, 0.0, mode);
        var item = new ItemEntity(level, owner.getX() + 2.0, owner.getY(), owner.getZ(), new ItemStack(Items.IRON_INGOT));
        var orb = new ExperienceOrb(level, owner.getX() + 2.0, owner.getY(), owner.getZ() + 1.0, 3);
        level.addFreshEntity(owner);
        level.addFreshEntity(item);
        level.addFreshEntity(orb);

        helper.runAtTickTime(1, () -> {
            familiar.tickOnServer(level);
            var itemCollected = item.position().distanceToSqr(owner.position()) <= 0.001D;
            var orbCollected = orb.position().distanceToSqr(owner.position()) <= 0.001D;
            helper.assertTrue(itemCollected == shouldCollect,
                    "AutoMagnet item collection mode mismatch. mode=" + mode + ", crouching=" + crouching);
            helper.assertTrue(orbCollected == shouldCollect,
                    "AutoMagnet orb collection mode mismatch. mode=" + mode + ", crouching=" + crouching);
            helper.assertTrue(familiar.isCollectionBlocked() != shouldCollect,
                    "AutoMagnet collection blocked flag should match mode stop state");
            helper.succeed();
        });
    }

    private static void setCrouchingForAutoMagnetTest(FakePlayer player, boolean crouching) {
        player.setShiftKeyDown(crouching);
        player.setPose(crouching ? Pose.CROUCHING : Pose.STANDING);
    }
}
