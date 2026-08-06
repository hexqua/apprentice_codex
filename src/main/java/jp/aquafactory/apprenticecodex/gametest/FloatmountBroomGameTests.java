package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomSurfaceScanner;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FloatmountBroomGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    private FloatmountBroomGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void registrationsResolveDedicatedTypes(GameTestHelper helper) {
        helper.assertTrue(ItemRegistry.FLOATMOUNT_BROOM.get() instanceof FloatmountBroomItem,
                "Floatmount Broom item should use its dedicated implementation");
        var broom = EntityRegistry.FLOATMOUNT_BROOM.get().create(helper.getLevel());
        helper.assertTrue(broom instanceof FloatmountBroomEntity,
                "Floatmount Broom entity type should create its dedicated entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void onlyOnePlayerMayRideAndOccupiedBroomCannotBeRecovered(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var first = player(helper, "floatmount_broom_first_rider");
        var second = player(helper, "floatmount_broom_second_rider");

        helper.assertTrue(first.startRiding(broom, true), "First player should be able to ride the broom");
        broom.positionRider(first);
        var riderAttachmentY = first.getY() + first.getVehicleAttachmentPoint(broom).y;
        helper.assertTrue(Math.abs(riderAttachmentY
                        - (broom.getY() + FloatmountBroomEntity.RIDER_ATTACHMENT_Y)) < 1.0e-6D,
                "Rider vehicle attachment should match the configured broom model height");
        helper.assertTrue(broom.getControllingPassenger() == first,
                "The sole player passenger should control the broom");
        helper.assertFalse(second.startRiding(broom), "Second player should not be able to ride the occupied broom");

        first.setShiftKeyDown(true);
        broom.interact(first, InteractionHand.MAIN_HAND);
        helper.assertFalse(broom.isRemoved(), "Occupied broom must not be recovered");
        helper.assertFalse(first.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Occupied recovery must not grant an item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingRecoveryReturnsFreshItemEvenInCreative(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_recovery");
        player.getAbilities().instabuild = true;
        player.setShiftKeyDown(true);

        broom.interact(player, InteractionHand.MAIN_HAND);

        helper.assertTrue(broom.isRemoved(), "Recovered broom should be removed");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Creative recovery should grant a fresh broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void damageAccumulatesDecaysAndBreaksIntoItem(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_damage");
        var source = helper.getLevel().damageSources().playerAttack(player);

        broom.hurt(source, 1.0F);
        helper.assertTrue(broom.getHurtTime() == 10, "Damage should start ten hurt ticks");
        helper.assertTrue(Math.abs(broom.getDamage() - 10.0F) < 1.0e-4F,
                "Damage should use the boat-like x10 accumulation");
        broom.tick();
        helper.assertTrue(broom.getHurtTime() == 9 && Math.abs(broom.getDamage() - 9.0F) < 1.0e-4F,
                "Damage presentation values should decay each tick");

        broom.hurt(source, 4.0F);
        helper.assertTrue(broom.isRemoved(), "Damage above forty should break the broom");
        helper.assertTrue(countDroppedBrooms(helper) == 1, "Broken broom should drop exactly one fresh item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mountingRequiresConfiguredManaButCreativeIsExempt(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_mount_mana");
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
            helper.assertFalse(creativeBroom.isEmergencyLanding(),
                    "Creative broom movement must not enter emergency landing");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void movementInputsUseConfiguredManaCosts(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(100, 50, 1.0D, 2.0D, 3.5D);
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
    public static void depletionEmergencyRecoveryAndSavedStateFollowServerRules(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_emergency");
            var magicData = magicData(helper, player);
            magicData.setMana(0.5F);
            helper.assertTrue(player.startRiding(broom, true), "Emergency test player should mount directly");
            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);

            broom.tick();

            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0e-4F,
                    "A movement cost above the remaining mana should consume the remainder");
            helper.assertTrue(broom.isEmergencyLanding(), "Movement depletion should enter emergency landing");

            magicData.setMana(10.0F);
            broom.acceptServerInput(player, 0.0F, 1.0F, true, false);
            broom.tick();
            helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1.0e-4F,
                    "Emergency movement must not consume mana");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(loaded.isEmergencyLanding(), "Emergency landing must persist in entity NBT");

            magicData.setMana(100.0F);
            broom.tick();
            helper.assertFalse(broom.isEmergencyLanding(),
                    "Reaching the normal flight threshold should recover normal flight");
            helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                    "Recovery tick must not immediately consume mana");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalDepletionWaitsForPaidInputAndEmergencyForcesDismountWarning(GameTestHelper helper) {
        var config = new FloatmountBroomServerConfig.Values(100, 50, 1.0D, 1.0D, 1.5D);
        try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
            var broom = spawnBroom(helper, 1.5D);
            var player = player(helper, "floatmount_broom_external_depletion");
            var magicData = magicData(helper, player);
            magicData.setMana(0.0F);
            helper.assertTrue(player.startRiding(broom, true), "External depletion test player should mount directly");

            broom.tick();
            helper.assertFalse(broom.isEmergencyLanding(),
                    "External zero mana must not trigger emergency landing while idle");
            var warningState = new CompoundTag();
            broom.saveWithoutId(warningState);
            helper.assertTrue(warningState.getBoolean("LowManaWarningShown"),
                    "Low mana warning latch should be stored on the broom entity");

            broom.acceptServerInput(player, 0.0F, 1.0F, false, false);
            broom.tick();
            helper.assertTrue(broom.isEmergencyLanding(),
                    "Paid movement attempted at zero mana should trigger emergency landing");
            helper.assertTrue(broom.isDangerousDismount(),
                    "Emergency landing must force dismount confirmation even near the ground");

            FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
            var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
            FloatmountBroomDismountEvents.onDismount(first);
            helper.assertTrue(first.isCanceled(), "First emergency dismount should be canceled");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unoccupiedBroomUsesCoastDamping(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        broom.setDeltaMovement(0.2D, 0.0D, 0.0D);

        broom.tick();

        helper.assertTrue(Math.abs(broom.getDeltaMovement().x - 0.17D) < 1.0e-6D,
                "Unoccupied broom should retain eighty-five percent of horizontal speed per tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void surfaceScannerTreatsSolidWaterAndLavaAsHoverSurfaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var solid = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(2, 1, 1));
        var lava = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(lava.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());

        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                level, solid.getX() + 0.5D, solid.getY() + 2.0D, solid.getZ() + 0.5D, 3, true).isPresent(),
                "Solid collision shape should be a hover surface");
        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                level, water.getX() + 0.5D, water.getY() + 2.0D, water.getZ() + 0.5D, 3, true).isPresent(),
                "Water should be a hover surface");
        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, true).isPresent(),
                "Lava should be a hover surface");
        helper.assertTrue(FloatmountBroomSurfaceScanner.findSurfaceBelow(
                        level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, false).isEmpty(),
                "Lava must make the safe dismount scan fail even with solid ground beneath it");

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(lava.getX() + 0.5D, lava.getY() + 1.5D, lava.getZ() + 0.5D);
        helper.assertTrue(broom.isDangerousDismount(),
                "Lava below the broom must always be classified as a dangerous dismount surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountRequiresReleaseAndSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = player(helper, "floatmount_broom_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());
        helper.assertTrue(broom.isDangerousDismount(), "High broom should be classified as dangerous");

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        FloatmountBroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
        FloatmountBroomDismountEvents.onDismount(held);
        helper.assertTrue(held.isCanceled(), "Holding sneak must not confirm dismount");

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, false);
        helper.assertTrue(player.getVehicle() == broom, "Releasing sneak should not dismount by itself");
        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        helper.assertFalse(player.isPassenger(), "Second press within thirty ticks should dismount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dangerousDismountNeverTreatsHeldSneakAsSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = player(helper, "floatmount_broom_held_dismount");
        helper.assertTrue(player.startRiding(broom, true), "Held dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());

        FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        FloatmountBroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        helper.runAfterDelay(FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS + 2, () -> {
            FloatmountBroomDismountEvents.handleSneakInput(player, broom, true);
            var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
            FloatmountBroomDismountEvents.onDismount(held);
            helper.assertTrue(held.isCanceled(), "Held sneak must remain canceled after the confirmation window");
            helper.assertTrue(player.getVehicle() == broom, "Held sneak must never dismount the rider");
            helper.succeed();
        });
    }

    private static FloatmountBroomEntity spawnBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    private static Player player(GameTestHelper helper, String name) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static MagicData magicData(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Floatmount Broom test could not resolve player mana data");
        return magicData;
    }

    private static void assertMovementManaCost(
            GameTestHelper helper,
            String playerName,
            float expectedCost,
            float strafe,
            float forward,
            boolean ascending,
            boolean descending
    ) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, playerName);
        var magicData = magicData(helper, player);
        magicData.setMana(200.0F);
        helper.assertTrue(player.startRiding(broom, true), playerName + " should mount directly");
        broom.acceptServerInput(player, strafe, forward, ascending, descending);

        broom.tick();

        helper.assertTrue(Math.abs(magicData.getMana() - (200.0F - expectedCost)) < 1.0e-4F,
                playerName + " used an unexpected mana cost: " + magicData.getMana());
        player.stopRiding();
    }

    private static int countDroppedBrooms(GameTestHelper helper) {
        var center = helper.absolutePos(TEST_POS);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(4.0D),
                        item -> item.getItem().is(ItemRegistry.FLOATMOUNT_BROOM.get()))
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }
}
