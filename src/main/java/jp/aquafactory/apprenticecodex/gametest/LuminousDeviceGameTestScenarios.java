package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevicePickupEvent;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceTooltip;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import java.util.List;
import java.util.UUID;

final class LuminousDeviceGameTestScenarios {
    private LuminousDeviceGameTestScenarios() {
    }

    static void luminousDeviceStoresOnlyTaggedItemsAndCapsTotal(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(new ItemStack(Items.TORCH).is(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE),
                    "Torch should be tagged for Luminous Device storage");
            helper.assertTrue(new ItemStack(Items.LANTERN).is(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE),
                    "Lantern should be tagged for Luminous Device storage");
            helper.assertTrue(new ItemStack(Items.GLOWSTONE).is(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE),
                    "Glowstone should be tagged for Luminous Device storage");
            helper.assertFalse(new ItemStack(Items.DIRT).is(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE),
                    "Dirt should not be tagged for Luminous Device storage");

            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 64)) == 64,
                    "Luminous Device should accept the first tagged stack");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                    "The first inserted item should become selected");
            helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.LANTERN, 1000)) == 960,
                    "Luminous Device should only accept items up to its shared capacity");
            helper.assertTrue(LuminousDevice.getStoredItemCount(deviceStack) == LuminousDevice.MAX_STORED_ITEMS,
                    "Luminous Device should cap total storage at 1024");
            helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 1)) == 0,
                    "A full Luminous Device should reject additional items");
            helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.DIRT, 1)) == 0,
                    "Luminous Device should reject items outside its storage tag");
        });
    }

    static void luminousDeviceRemovalPrefersSelectionAndUpdatesSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 10));
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.LANTERN, 5));
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 5));
            var tooltip = deviceStack.getItem().getTooltipImage(deviceStack).orElseThrow();
            helper.assertTrue(tooltip instanceof LuminousDeviceTooltip luminousTooltip
                            && luminousTooltip.highlightedIndex() == 0,
                    "Tooltip should highlight the selected removal candidate instead of the smallest stack");

            var firstRemoved = LuminousDevice.removeStackForInventory(deviceStack);
            helper.assertTrue(firstRemoved.is(Items.TORCH) && firstRemoved.getCount() == 10,
                    "The selected item should be removed before a less numerous stored item");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.LANTERN),
                    "Removing all selected items should fall back to the first remaining stored item");

            var secondRemoved = LuminousDevice.removeStackForInventory(deviceStack);
            helper.assertTrue(secondRemoved.is(Items.LANTERN) && secondRemoved.getCount() == 5,
                    "The new selected item should be removed next");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.GLOWSTONE),
                    "Selection should again fall back to the first remaining stored item");

            var lastRemoved = LuminousDevice.removeStackForInventory(deviceStack);
            helper.assertTrue(lastRemoved.is(Items.GLOWSTONE) && lastRemoved.getCount() == 5,
                    "The last stored item should be removable");
            helper.assertTrue(LuminousDevice.getStoredItemCount(deviceStack) == 0,
                    "Manual extraction should be able to empty the device");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).isEmpty(),
                    "Manually emptying the device should clear its selection");

            var carriedDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(carriedDevice, new ItemStack(Items.TORCH, 10));
            var destinationSlot = new Slot(new SimpleContainer(1), 0, 0, 0);
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_carried_removal_test")
            );
            helper.assertTrue(
                    carriedDevice.getItem().overrideStackedOnOther(
                            carriedDevice,
                            destinationSlot,
                            ClickAction.SECONDARY,
                            player
                    ),
                    "Right-clicking an empty slot with a carried device should extract its contents"
            );
            helper.assertTrue(destinationSlot.getItem().is(Items.TORCH)
                            && destinationSlot.getItem().getCount() == 10,
                    "The extracted contents should be inserted into the empty slot");
            helper.assertTrue(LuminousDevice.getStoredItemCount(carriedDevice) == 0,
                    "Extracting into an empty slot should consume the stored contents");
        });
    }

    static void luminousDeviceUsePlacesSelectedBlocksAndKeepsEmptySelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_place_test")
            );
            var placements = List.of(
                    new PlacementCase(Items.TORCH, Blocks.TORCH, new BlockPos(1, 1, 1)),
                    new PlacementCase(Items.LANTERN, Blocks.LANTERN, new BlockPos(3, 1, 1)),
                    new PlacementCase(Items.GLOWSTONE, Blocks.GLOWSTONE, new BlockPos(5, 1, 1))
            );

            for (var placement : placements) {
                var supportPos = placement.targetPos().below();
                helper.setBlock(supportPos, Blocks.STONE);
                helper.setBlock(placement.targetPos(), Blocks.AIR);

                var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
                LuminousDevice.addToDevice(deviceStack, new ItemStack(placement.item(), 1));
                player.setItemInHand(InteractionHand.MAIN_HAND, deviceStack);

                var absoluteSupportPos = helper.absolutePos(supportPos);
                var hitResult = new BlockHitResult(
                        Vec3.atCenterOf(absoluteSupportPos).add(0.0D, 0.5D, 0.0D),
                        Direction.UP,
                        absoluteSupportPos,
                        false
                );
                var result = deviceStack.getItem().useOn(
                        new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult)
                );

                helper.assertTrue(result.consumesAction(),
                        placement.item() + " should report a successful delegated placement");
                helper.assertBlockPresent(placement.block(), placement.targetPos());
                helper.assertTrue(LuminousDevice.getStoredItemCount(deviceStack) == 0,
                        "Successful placement should consume exactly one stored item");
                helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(placement.item()),
                        "Use consumption should preserve the empty selected item");

                var views = LuminousDevice.getSelectionViews(deviceStack);
                helper.assertTrue(views.size() == 1
                                && views.get(0).currentSelection()
                                && "0".equals(views.get(0).badgeText()),
                        "Selection UI data should retain the selected zero-count item");
                helper.assertTrue(deviceStack.getItem().useOn(
                                new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult)
                        ) == InteractionResult.FAIL,
                        "Using a zero-count selection should fail");
            }

            var creativeSupportPos = new BlockPos(7, 0, 3);
            helper.setBlock(creativeSupportPos, Blocks.STONE);
            var creativeDeviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(creativeDeviceStack, new ItemStack(Items.TORCH, 1));
            player.getAbilities().instabuild = true;
            player.setItemInHand(InteractionHand.MAIN_HAND, creativeDeviceStack);
            var absoluteCreativeSupportPos = helper.absolutePos(creativeSupportPos);
            var creativeResult = creativeDeviceStack.getItem().useOn(new UseOnContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                            Vec3.atCenterOf(absoluteCreativeSupportPos).add(0.0D, 0.5D, 0.0D),
                            Direction.UP,
                            absoluteCreativeSupportPos,
                            false
                    )
            ));
            helper.assertTrue(creativeResult.consumesAction(),
                    "Creative placement should still delegate to the selected item");
            helper.assertBlockPresent(Blocks.TORCH, creativeSupportPos.above());
            helper.assertTrue(LuminousDevice.getStoredCount(creativeDeviceStack, new ItemStack(Items.TORCH)) == 1,
                    "Creative placement should not consume stored items");
        });
    }

    static void luminousDeviceEmptyTooltipAndManualEmptyClearGhostSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            var tooltip = deviceStack.getItem().getTooltipImage(deviceStack).orElseThrow();
            helper.assertTrue(tooltip instanceof LuminousDeviceTooltip luminousTooltip
                            && luminousTooltip.items().isEmpty(),
                    "An empty Luminous Device should still provide empty-grid tooltip data");

            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 1));
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_empty_selection_test")
            );
            player.setItemInHand(InteractionHand.MAIN_HAND, deviceStack);
            var supportPos = new BlockPos(7, 0, 1);
            helper.setBlock(supportPos, Blocks.STONE);
            var absoluteSupportPos = helper.absolutePos(supportPos);
            deviceStack.getItem().useOn(new UseOnContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                            Vec3.atCenterOf(absoluteSupportPos).add(0.0D, 0.5D, 0.0D),
                            Direction.UP,
                            absoluteSupportPos,
                            false
                    )
            ));
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                    "Use should leave a zero-count torch selection");

            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 5));
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.LANTERN, 1));
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                    "Inserting another item should not replace an existing zero-count selection");
            var removed = LuminousDevice.removeStackForInventory(deviceStack);
            helper.assertTrue(removed.is(Items.LANTERN) && removed.getCount() == 1,
                    "A missing selected item should fall back to the least numerous stored item");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                    "Fallback extraction should preserve the unavailable current selection");
            var lastRemoved = LuminousDevice.removeStackForInventory(deviceStack);
            helper.assertTrue(lastRemoved.is(Items.GLOWSTONE) && lastRemoved.getCount() == 5,
                    "Fallback extraction should continue until storage is empty");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).isEmpty(),
                    "Manually emptying storage should clear a zero-count ghost selection");
        });
    }

    static void luminousDeviceAutoStoresOnlyKnownPickedUpItemsAcrossInventoryDevices(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_pickup_test")
            );
            var firstDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            var secondDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(firstDevice, new ItemStack(Items.TORCH, 1020));
            LuminousDevice.addToDevice(secondDevice, new ItemStack(Items.TORCH, 1));
            helper.assertTrue(LuminousDevice.consumeOneStored(secondDevice, new ItemStack(Items.TORCH)),
                    "Test setup should leave a zero-count selected torch in the second device");
            player.getInventory().setItem(10, firstDevice);
            player.setItemInHand(InteractionHand.OFF_HAND, secondDevice);

            var torchEntity = new ItemEntity(
                    helper.getLevel(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    new ItemStack(Items.TORCH, 10)
            );
            LuminousDevicePickupEvent.onEntityItemPickup(new EntityItemPickupEvent(player, torchEntity));

            helper.assertTrue(LuminousDevice.getStoredCount(firstDevice, new ItemStack(Items.TORCH)) == 1024,
                    "Pickup storage should fill the first matching device before continuing");
            helper.assertTrue(LuminousDevice.getStoredCount(secondDevice, new ItemStack(Items.TORCH)) == 6,
                    "Pickup storage should refill a matching zero-count selection in the next device");
            helper.assertTrue(torchEntity.isRemoved(),
                    "Pickup handling should finish the ItemEntity when every item was stored");

            var partialPlayer = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_partial_pickup_test")
            );
            var partialDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(partialDevice, new ItemStack(Items.TORCH, 1022));
            partialPlayer.getInventory().setItem(10, partialDevice);
            var partialTorchEntity = new ItemEntity(
                    helper.getLevel(),
                    partialPlayer.getX(),
                    partialPlayer.getY(),
                    partialPlayer.getZ(),
                    new ItemStack(Items.TORCH, 5)
            );
            LuminousDevicePickupEvent.onEntityItemPickup(
                    new EntityItemPickupEvent(partialPlayer, partialTorchEntity)
            );

            helper.assertTrue(LuminousDevice.getStoredCount(partialDevice, new ItemStack(Items.TORCH)) == 1024,
                    "Pickup storage should fill the remaining device capacity");
            helper.assertTrue(partialPlayer.getInventory().countItem(Items.TORCH) == 3,
                    "Items beyond device capacity should continue into the normal inventory");
            helper.assertTrue(partialTorchEntity.isRemoved(),
                    "Pickup handling should finish the ItemEntity when the normal inventory accepts the remainder");

            var untouchedPlayer = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_unknown_pickup_test")
            );
            var emptyDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            untouchedPlayer.getInventory().setItem(10, emptyDevice);
            var lanternEntity = new ItemEntity(
                    helper.getLevel(),
                    untouchedPlayer.getX(),
                    untouchedPlayer.getY(),
                    untouchedPlayer.getZ(),
                    new ItemStack(Items.LANTERN, 3)
            );
            var lanternPickupEvent = new EntityItemPickupEvent(untouchedPlayer, lanternEntity);
            LuminousDevicePickupEvent.onEntityItemPickup(lanternPickupEvent);

            helper.assertTrue(LuminousDevice.getStoredItemCount(emptyDevice) == 0,
                    "A tagged item that was never stored or selected should not be auto-stored");
            helper.assertFalse(lanternPickupEvent.isCanceled(),
                    "Unknown tagged items should remain available to vanilla pickup handling");
            helper.assertFalse(lanternEntity.isRemoved(),
                    "Unknown tagged items should remain in their ItemEntity until vanilla pickup runs");
        });
    }

    private record PlacementCase(net.minecraft.world.item.Item item, Block block, BlockPos targetPos) {
    }
}
