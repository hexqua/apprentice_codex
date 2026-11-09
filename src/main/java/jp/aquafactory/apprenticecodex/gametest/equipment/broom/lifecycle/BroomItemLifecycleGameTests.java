package jp.aquafactory.apprenticecodex.gametest.equipment.broom.lifecycle;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.FloatmountBroomServerConfig;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.player;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomItemLifecycleGameTests {
    private BroomItemLifecycleGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void hoverridePlacementAndRecoveryKeepDedicatedItemAndCustomName(GameTestHelper helper) {
        var player = player(helper, "hoverride_broom_placement");
        var expectedName = Component.literal("Sidewinder").withStyle(ChatFormatting.AQUA);
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        stack.setHoverName(expectedName);
        installBroomCalibration(stack, 2);
        var broom = placeHoverrideBroomFromItem(helper, player, stack);

        helper.assertTrue(stack.isEmpty(), "Hoverride Broom placement should consume its item");
        helper.assertTrue(expectedName.equals(broom.getCustomName()),
                "Placed Hoverride Broom should copy the item name");

        player.setShiftKeyDown(true);
        broom.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(broom.isRemoved(), "Hoverride Broom should be recovered while unoccupied");
        var recovered = findBroomInInventory(helper, player, ItemRegistry.HOVERRIDE_BROOM.get());
        helper.assertTrue(recovered.is(ItemRegistry.HOVERRIDE_BROOM.get()),
                "Recovered Hoverride Broom must not turn into a Floatmount Broom");
        helper.assertTrue(expectedName.equals(recovered.getHoverName()),
                "Recovered Hoverride Broom should preserve its custom name");
        assertCalibrationContents(helper, recovered, 2, "Recovered Hoverride Broom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void defaultItemPlacementDoesNotCreateAVisibleCustomName(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_default_placement");
        var broom = placeBroomFromItem(helper, player, new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        helper.assertFalse(broom.hasCustomName(), "Default item name must not become a custom name");
        helper.assertFalse(broom.isCustomNameVisible(), "Default broom must not show a nameplate");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledEntityJoinDoesNotConsumeItem(GameTestHelper helper) {
        var player = player(helper, "floatmount_broom_cancelled_placement");
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Consumer<EntityJoinLevelEvent> cancelBroomJoin = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof FloatmountBroomEntity) {
                event.setCanceled(true);
            }
        };

        InteractionResult result;
        MinecraftForge.EVENT_BUS.addListener(cancelBroomJoin);
        try {
            result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancelBroomJoin);
        }

        helper.assertTrue(result == InteractionResult.FAIL,
                "Cancelled broom placement should report failure");
        helper.assertTrue(stack.getCount() == 1,
                "Cancelled broom placement must not consume the item");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                FloatmountBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        ).isEmpty(), "Cancelled broom placement must not add an entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void namedItemPlacementCopiesVisibleNameAndEntitySaveRetainsIt(GameTestHelper helper) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> {
            var player = player(helper, "floatmount_broom_named_placement");
            var expectedName = Component.literal("Zephyr").withStyle(ChatFormatting.AQUA);
            var stack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
            stack.setHoverName(expectedName);
            var broom = placeBroomFromItem(helper, player, stack);
            helper.assertTrue(expectedName.equals(broom.getCustomName()), "Placed broom should copy the item name");
            helper.assertTrue(broom.isCustomNameVisible(), "Named broom should show its nameplate");

            var saved = new CompoundTag();
            broom.saveWithoutId(saved);
            var loaded = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(expectedName.equals(loaded.getCustomName()), "Saved broom should retain its name");
            helper.assertTrue(loaded.isCustomNameVisible(), "Saved broom should retain nameplate visibility");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void onlyOnePlayerMayRideAndOccupiedBroomCannotBeRecovered(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var first = serverRider(helper);
        var second = serverRider(helper);

        helper.assertTrue(first.startRiding(broom, true), "First player should be able to ride the broom");
        broom.positionRider(first);
        helper.assertTrue(Math.abs(first.getY()
                        - (broom.getY() + FloatmountBroomEntity.RIDER_ATTACHMENT_Y)) < 1.0e-6D,
                "Rider vehicle attachment should match the configured broom model height");
        helper.assertTrue(broom.getControllingPassenger() == first,
                "The sole player passenger should control the broom");
        helper.assertFalse(second.startRiding(broom), "Second player should not be able to ride the occupied broom");

        second.setShiftKeyDown(true);
        broom.interact(second, InteractionHand.MAIN_HAND);
        helper.assertFalse(broom.isRemoved(), "A third party must not recover an occupied broom");
        helper.assertTrue(first.getVehicle() == broom,
                "Rejected third-party recovery must leave the rider mounted");
        helper.assertFalse(second.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Rejected third-party recovery must not grant a broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void thirdPartyCanRecoverUnoccupiedBroom(GameTestHelper helper) {
        var placer = player(helper, "floatmount_broom_placer");
        var broom = placeBroomFromItem(helper, placer, new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        var collector = player(helper, "floatmount_broom_third_party_collector");
        collector.setShiftKeyDown(true);

        broom.interact(collector, InteractionHand.MAIN_HAND);

        helper.assertTrue(broom.isRemoved(), "A third party should be able to recover an unoccupied broom");
        helper.assertTrue(collector.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "The third party who recovered the broom should receive its item");
        helper.assertFalse(placer.getInventory().contains(new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get())),
                "Recovering the broom must not return its item to the original placer");
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
    public static void sneakingRecoveryPreservesCalibrationAndRedeploysWithResetState(GameTestHelper helper) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, () -> {
            var config = new FloatmountBroomServerConfig.Values(1000, 50, 10, Set.of(), 100, 50, 1.0D, 1.0D, 1.5D);
            try (var ignored = ApprenticeCodexServerConfig.useFloatmountBroomConfigOverrideForGameTest(config)) {
                var expectedName = Component.literal("Restored Broom").withStyle(ChatFormatting.GOLD);
                var sourceStack = backCurioCalibratedBroomStack(ItemRegistry.FLOATMOUNT_BROOM.get(), 2);
                sourceStack.setHoverName(expectedName);
                var placer = player(helper, "floatmount_broom_calibrated_placer");
                var broom = placeBroomFromItem(helper, placer, sourceStack);
                broom.setCustomNameVisible(true);
                broom.hurt(helper.getLevel().damageSources().fellOutOfWorld(), 20.0F);
                var damagedState = new CompoundTag();
                broom.saveWithoutId(damagedState);
                damagedState.putBoolean("EmergencyLanding", true);
                broom.load(damagedState);
                helper.assertTrue(broom.isDamaged(), "Recovery setup should use a damaged broom");
                helper.assertTrue(broom.isManaEmergencyLanding(), "Recovery setup should use an emergency broom");

                var player = player(helper, "floatmount_broom_named_recovery");
                player.setShiftKeyDown(true);
                broom.interact(player, InteractionHand.MAIN_HAND);
                var recovered = findBroomInInventory(helper, player);
                helper.assertTrue(expectedName.equals(recovered.getHoverName()),
                        "Sneaking recovery should copy the entity custom name");
                assertCalibrationContents(helper, recovered, 2, "Recovered Floatmount Broom");
                helper.assertTrue(AbstractBroomItem.isBackCurioEnabled(recovered),
                        "Normal placement and recovery should preserve Curios calibration");

                var redeployStack = recovered.copy();
                player.getInventory().clearContent();
                player.setShiftKeyDown(false);
                var redeployed = placeBroomFromItem(helper, player, redeployStack);
                helper.assertTrue(expectedName.equals(redeployed.getCustomName()), "Redeployed broom should retain its name");
                helper.assertTrue(redeployed.getDamage() == 0, "Redeployed broom should reset damage");
                helper.assertFalse(redeployed.isDamaged(), "Redeployed broom should reset damaged state");
                helper.assertFalse(redeployed.isManaEmergencyLanding(), "Redeployed broom should reset emergency landing");
                assertCalibrationContents(helper, redeployed.getBroomItemStack(), 2, "Redeployed Floatmount Broom");
                helper.assertTrue(AbstractBroomItem.isBackCurioEnabled(redeployed.getBroomItemStack()),
                        "Redeployed broom should preserve Curios calibration");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void nameTagDoesNotRenameBroom(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        var player = player(helper, "floatmount_broom_name_tag");
        player.getAbilities().instabuild = true;
        var nameTag = new ItemStack(Items.NAME_TAG);
        nameTag.setHoverName(Component.literal("Rejected Name"));
        player.setItemInHand(InteractionHand.MAIN_HAND, nameTag);

        broom.interact(player, InteractionHand.MAIN_HAND);

        helper.assertFalse(broom.hasCustomName(), "Name tags must not rename Floatmount Broom entities");
        helper.assertTrue(nameTag.getCount() == 1, "Unsupported name tag use must not consume the item");
        helper.succeed();
    }

    static FloatmountBroomEntity placeBroomFromItem(GameTestHelper helper, Player player, ItemStack stack) {
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        var brooms = helper.getLevel().getEntitiesOfClass(
                FloatmountBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        );
        helper.assertTrue(brooms.size() == 1, "Broom item use should place exactly one broom entity");
        return brooms.get(0);
    }

    static HoverrideBroomEntity placeHoverrideBroomFromItem(
            GameTestHelper helper,
            Player player,
            ItemStack stack
    ) {
        var target = helper.absolutePos(TEST_POS);
        player.setPos(target.getX() + 0.5D, target.getY() + 2.5D, target.getZ() + 0.5D);
        player.setXRot(90.0F);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        var brooms = helper.getLevel().getEntitiesOfClass(
                HoverrideBroomEntity.class,
                new AABB(target).inflate(2.0D, 4.0D, 2.0D)
        );
        helper.assertTrue(brooms.size() == 1,
                "Hoverride Broom item use should place exactly one dedicated entity");
        return brooms.get(0);
    }

    static ItemStack findBroomInInventory(GameTestHelper helper, Player player) {
        return findBroomInInventory(helper, player, ItemRegistry.FLOATMOUNT_BROOM.get());
    }

    static ItemStack findBroomInInventory(GameTestHelper helper, Player player, Item broomItem) {
        for (var slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(broomItem)) {
                return stack;
            }
        }
        helper.fail("Recovered broom item was not found in the player inventory");
        return ItemStack.EMPTY;
    }
}
