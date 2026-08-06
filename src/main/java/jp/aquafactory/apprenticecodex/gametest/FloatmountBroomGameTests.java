package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomSurfaceScanner;
import jp.aquafactory.apprenticecodex.item.FloatmountBroomItem;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
    public static void surfaceScannerTreatsSolidWaterAndLavaAsHoverSurfaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var solid = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(2, 1, 1));
        var lava = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
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
                        level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 2, false).isEmpty(),
                "Lava surface must be skipped by the safe dismount scan");
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

    private static int countDroppedBrooms(GameTestHelper helper) {
        var center = helper.absolutePos(TEST_POS);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(4.0D),
                        item -> item.getItem().is(ItemRegistry.FLOATMOUNT_BROOM.get()))
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }
}
