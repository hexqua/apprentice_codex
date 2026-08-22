package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase.SpellcasterAccessoryCaseBlock;
import jp.aquafactory.apprenticecodex.block.spellcasteraccessorycase.SpellcasterAccessoryCaseBlockEntity;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.event.CuriosEventHandler;
import top.theillusivec4.curios.common.inventory.CurioSlot;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellcasterAccessoryCaseGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String RING_SLOT = "ring";
    private static final ResourceLocation EXPANDED_BACK_SLOT_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "gametest/accessory_case_expanded_back"
    );
    private static final ResourceLocation OVERSIZED_BACK_SLOT_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "gametest/accessory_case_oversized_back"
    );

    private SpellcasterAccessoryCaseGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseSneakUsePlacesWithoutOpeningMenu(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "accessory_case_place_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        caseStack.set(DataComponents.CUSTOM_NAME, Component.literal("Placed Accessory Case"));
        var itemInventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        helper.assertTrue(itemInventory.insertItem(
                0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()), false
        ).isEmpty(), "Accessory case placement test should store its ring");
        player.setItemInHand(InteractionHand.MAIN_HAND, caseStack);
        player.setShiftKeyDown(true);

        var clickedFloor = helper.absolutePos(new BlockPos(1, 1, 1));
        var placedPos = clickedFloor.above();
        var hit = new BlockHitResult(
                Vec3.atCenterOf(clickedFloor), net.minecraft.core.Direction.UP, clickedFloor, false
        );
        var result = ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get().useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit)
        );

        helper.assertTrue(result.consumesAction(), "Sneaking accessory case use should place the block");
        helper.assertTrue(helper.getLevel().getBlockState(placedPos).is(BlockRegistry.SPELLCASTER_ACCESSORY_CASE.get()),
                "Sneaking accessory case use should place an accessory case block");
        helper.assertTrue(!(player.containerMenu instanceof SpellcasterAccessoryCaseMenu),
                "Successful sneak placement must not open the accessory case menu");
        helper.assertTrue(helper.getLevel().getBlockState(placedPos).getValue(SpellcasterAccessoryCaseBlock.FACING)
                        == player.getDirection().getOpposite(),
                "Placed accessory case should face its placer");

        var placedBlockEntity = (SpellcasterAccessoryCaseBlockEntity) helper.getLevel().getBlockEntity(placedPos);
        helper.assertTrue(placedBlockEntity != null, "Placed accessory case should create its block entity");
        helper.assertTrue(placedBlockEntity.getInventory().getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Placement should transfer stored Curios items to the block entity");
        helper.assertTrue(placedBlockEntity.getCaseStack().getHoverName().getString().equals("Placed Accessory Case"),
                "Placement should preserve the accessory case custom name");

        var saved = placedBlockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        var reloaded = BlockEntity.loadStatic(
                placedPos,
                helper.getLevel().getBlockState(placedPos),
                saved,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(reloaded instanceof SpellcasterAccessoryCaseBlockEntity,
                "Saved accessory case should reload as the registered block entity type");
        helper.assertTrue(((SpellcasterAccessoryCaseBlockEntity) reloaded)
                        .getInventory().getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Accessory case block entity should persist stored Curios items");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        var blockedTarget = helper.absolutePos(new BlockPos(2, 2, 1));
        helper.getLevel().setBlockAndUpdate(blockedTarget, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var blockedFloor = blockedTarget.below();
        var failed = ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(blockedFloor), net.minecraft.core.Direction.UP, blockedFloor, false
                )
        ));
        helper.assertFalse(failed.consumesAction(), "Blocked sneak placement should fail");
        helper.assertTrue(!(player.containerMenu instanceof SpellcasterAccessoryCaseMenu),
                "Failed sneak placement must not open the accessory case menu");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseBlockMenuSharesItsInventory(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "accessory_case_block_menu_test"
        );
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var blockEntity = placeAccessoryCaseBlock(
                helper, pos, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get())
        );
        var firstMenu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), pos);
        var secondMenu = new SpellcasterAccessoryCaseMenu(2, player.getInventory(), pos);
        var ring = new ItemStack(ItemRegistry.ATTACKCAST_RING.get());

        helper.assertTrue(firstMenu.getSlot(0).mayPlace(ring),
                "Placed accessory case should keep Curios validation in its storage slots");
        firstMenu.getSlot(0).set(ring.copy());
        helper.assertTrue(secondMenu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Two block menus should share one block entity inventory");
        helper.assertTrue(blockEntity.getInventory().getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Block menu changes should update the block entity inventory");
        helper.assertTrue(firstMenu.stillValid(player),
                "Placed accessory case menu should remain valid within container range");

        var hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
        var interactionResult = helper.getLevel().getBlockState(pos)
                .useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(interactionResult.consumesAction(),
                "Right-clicking a placed accessory case should consume the interaction");
        // NeoForge FakePlayer は openMenu を意図的に無視するため、provider が block source menu を作ることを別に確認する。
        helper.assertTrue(blockEntity.createMenu(3, player.getInventory(), player)
                        instanceof SpellcasterAccessoryCaseMenu,
                "Placed accessory case should provide its shared block menu");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseDefersInventoryLoadUntilLevelIsAvailable(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "accessory_case_detached_block_entity_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        new SpellcasterAccessoryCase.CaseInventory(caseStack, player)
                .insertItem(0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()), false);
        var blockEntity = new SpellcasterAccessoryCaseBlockEntity(
                helper.absolutePos(new BlockPos(1, 2, 1)),
                BlockRegistry.SPELLCASTER_ACCESSORY_CASE.get().defaultBlockState()
        );

        blockEntity.setCaseStack(caseStack);
        helper.assertTrue(blockEntity.getInventory().getStackInSlot(0).isEmpty(),
                "Detached accessory case should defer registry-dependent inventory loading");
        var detachedSaved = blockEntity.saveWithFullMetadata(helper.getLevel().registryAccess());
        var detachedReloaded = BlockEntity.loadStatic(
                blockEntity.getBlockPos(),
                blockEntity.getBlockState(),
                detachedSaved,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(detachedReloaded instanceof SpellcasterAccessoryCaseBlockEntity reloaded
                        && reloaded.getInventory().getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Saving a detached accessory case should preserve its deferred inventory data");
        blockEntity.setLevel(helper.getLevel());
        helper.assertTrue(blockEntity.getInventory().getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Accessory case should load deferred inventory after receiving its level");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCasePlayerBreakReturnsOnePreservedCase(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "accessory_case_pickup_test"
        );
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        caseStack.set(DataComponents.CUSTOM_NAME, Component.literal("Recovered Accessory Case"));
        new SpellcasterAccessoryCase.CaseInventory(caseStack, player)
                .insertItem(0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()), false);
        placeAccessoryCaseBlock(helper, pos, caseStack);

        helper.assertTrue(helper.getLevel().getBlockState(pos)
                        .getDestroyProgress(player, helper.getLevel(), pos) >= 1.0F,
                "Accessory case should break instantly regardless of the held item");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(player.gameMode.destroyBlock(pos), "Player should be able to remove the accessory case");
        var recovered = player.getMainHandItem();
        helper.assertTrue(recovered.is(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()),
                "An empty main hand should receive the removed accessory case directly");
        helper.assertTrue(recovered.getHoverName().getString().equals("Recovered Accessory Case"),
                "Direct recovery should preserve the custom name");
        helper.assertTrue(new SpellcasterAccessoryCase.CaseInventory(recovered, player)
                        .getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Direct recovery should preserve stored Curios items");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                ItemEntity.class, new AABB(pos).inflate(2.0D)
        ).isEmpty(), "Direct player recovery should not create a duplicate item drop");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseBreakFallsBackToInventoryThenDrop(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "accessory_case_pickup_fallback_test"
        );
        var inventoryPos = helper.absolutePos(new BlockPos(1, 2, 1));
        placeAccessoryCaseBlock(
                helper, inventoryPos, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get())
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        helper.assertTrue(player.gameMode.destroyBlock(inventoryPos),
                "Held-item removal should break the accessory case");
        helper.assertTrue(playerInventoryContains(player.getInventory(), ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()),
                "Held-item removal should insert the accessory case into player inventory");

        for (var slot = 0; slot < Inventory.INVENTORY_SIZE; ++slot) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, Items.STONE.getDefaultMaxStackSize()));
        }
        var dropPos = helper.absolutePos(new BlockPos(2, 2, 1));
        placeAccessoryCaseBlock(helper, dropPos, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        helper.assertTrue(player.gameMode.destroyBlock(dropPos),
                "Full-inventory removal should still break the accessory case");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class, AABB.ofSize(player.position(), 4.0D, 4.0D, 4.0D)
                ).stream().anyMatch(entity -> entity.getItem().is(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get())),
                "Full-inventory removal should drop the accessory case from the player");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseEnvironmentalBreakPreservesContents(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "accessory_case_environment_drop_test"
        );
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        new SpellcasterAccessoryCase.CaseInventory(caseStack, player)
                .insertItem(0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()), false);
        placeAccessoryCaseBlock(helper, pos, caseStack);

        helper.assertTrue(helper.getLevel().destroyBlock(pos, true),
                "Environmental block removal should remove the accessory case");
        var droppedCase = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.0D)).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        helper.assertFalse(droppedCase.isEmpty(),
                "Environmental block removal should drop the accessory case");
        helper.assertTrue(new SpellcasterAccessoryCase.CaseInventory(droppedCase, player)
                        .getStackInSlot(0).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Environmental block removal should preserve stored Curios items");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseStoresOnlyCuriosAndPersists(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_storage_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var inventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        var ring = new ItemStack(ItemRegistry.ATTACKCAST_RING.get());

        helper.assertTrue(inventory.getSlots() == SpellcasterAccessoryCase.SLOT_COUNT,
                "Accessory case should expose exactly 27 storage slots");
        helper.assertTrue(inventory.insertItem(0, ring.copy(), false).isEmpty(),
                "Accessory case should accept a Curios item");
        helper.assertTrue(inventory.insertItem(1, new ItemStack(Items.STONE), false).is(Items.STONE),
                "Accessory case should reject a non-Curios item");
        helper.assertTrue(inventory.insertItem(1, caseStack.copy(), false).getItem()
                        instanceof SpellcasterAccessoryCase,
                "Accessory case should reject nested accessory cases");

        var broom = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(inventory.insertItem(1, broom.copy(), false).getItem() == broom.getItem(),
                "Accessory case should reject an uncalibrated broom without a valid Curios slot");
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        broom, 2, new ItemStack(Items.SADDLE)),
                "Accessory case test broom should accept its Curios calibration");
        helper.assertTrue(inventory.insertItem(1, broom.copy(), false).isEmpty(),
                "Accessory case should accept a saddle-calibrated broom with a valid Curios slot");

        var reloaded = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        helper.assertTrue(ItemStack.isSameItemSameComponents(reloaded.getStackInSlot(0), ring),
                "Stored Curios item should persist on the accessory case stack");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseMenuLocksSourceAndPrioritizesStorage(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_menu_test"
        );
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        player.getInventory().setItem(9, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()));
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);

        var sourceMenuSlot = SpellcasterAccessoryCase.SLOT_COUNT + 27;
        helper.assertFalse(menu.getSlot(sourceMenuSlot).mayPickup(player),
                "The accessory case opening this menu must stay locked in its hotbar slot");
        var moved = menu.quickMoveStack(player, SpellcasterAccessoryCase.SLOT_COUNT);
        helper.assertFalse(moved.isEmpty(),
                "Shift-clicking a Curios item from player inventory should move it");
        helper.assertTrue(menu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Shift-clicking from player inventory should prioritize accessory case storage");
        var slotCount = menu.slots.size();
        menu.setItem(slotCount + 7, 0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()));
        helper.assertTrue(menu.slots.size() == slotCount,
                "A stale client slot update should be ignored while Curios slots are rebuilding");
        var expandedContents = NonNullList.withSize(slotCount + 1, ItemStack.EMPTY);
        expandedContents.set(slotCount, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()));
        menu.initializeContents(1, expandedContents, ItemStack.EMPTY);
        helper.assertTrue(menu.slots.size() == slotCount,
                "A stale full content update should be tolerated while Curios slots are rebuilding");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseMenuRejectsSourceHotbarSwapAndDetectsReplacement(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_source_swap_test"
        );
        var sourceCase = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var otherCase = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        player.getInventory().setItem(0, sourceCase);
        player.getInventory().setItem(9, otherCase);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);

        menu.clicked(SpellcasterAccessoryCase.SLOT_COUNT, 0, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(0) == sourceCase,
                "Number-key swapping must not move the accessory case that opened the menu");
        helper.assertTrue(player.getInventory().getItem(9) == otherCase,
                "Rejected source-case swapping must leave the target inventory slot unchanged");
        helper.assertTrue(menu.stillValid(player),
                "Rejecting a source-case swap should keep the menu valid");

        player.getInventory().setItem(1, new ItemStack(Items.STONE));
        player.getInventory().setItem(10, new ItemStack(Items.DIRT));
        menu.clicked(SpellcasterAccessoryCase.SLOT_COUNT + 1, 1, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(1).is(Items.DIRT),
                "Unrelated number-key swaps should remain available");
        helper.assertTrue(player.getInventory().getItem(10).is(Items.STONE),
                "Unrelated number-key swaps should update the hovered inventory slot");

        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        helper.assertFalse(menu.stillValid(player),
                "The server menu must become invalid when its source case instance is replaced");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseMenuRejectsSourceOffhandSwap(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_offhand_swap_test"
        );
        var sourceCase = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var otherCase = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        player.getInventory().setItem(Inventory.SLOT_OFFHAND, sourceCase);
        player.getInventory().setItem(9, otherCase);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), Inventory.SLOT_OFFHAND);

        menu.clicked(SpellcasterAccessoryCase.SLOT_COUNT, Inventory.SLOT_OFFHAND, ClickType.SWAP, player);
        helper.assertTrue(player.getInventory().getItem(Inventory.SLOT_OFFHAND) == sourceCase,
                "Offhand swapping must not move the accessory case that opened the menu");
        helper.assertTrue(player.getInventory().getItem(9) == otherCase,
                "Rejected offhand source-case swapping must leave the target inventory slot unchanged");
        helper.assertTrue(menu.stillValid(player),
                "Rejecting an offhand source-case swap should keep the menu valid");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseQuickMoveTransfersBetweenStorageAndCurios(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_quick_move_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var ring = new ItemStack(ItemRegistry.ATTACKCAST_RING.get());
        var caseInventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        helper.assertTrue(caseInventory.insertItem(0, ring.copy(), false).isEmpty(),
                "Accessory case should accept the quick-move test ring");
        player.getInventory().setItem(0, caseStack);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        var curioMenuSlot = findFirstAvailableCurioMenuSlot(menu, ring);

        helper.assertTrue(curioMenuSlot >= 0,
                "Accessory case menu should expose an available Curios slot for the test ring");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "Shift-clicking from accessory case storage should remove the stored ring");
        helper.assertTrue(menu.getSlot(curioMenuSlot).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Shift-clicking from accessory case storage should equip the ring");

        menu.clicked(curioMenuSlot, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(curioMenuSlot).getItem().isEmpty(),
                "Shift-clicking from a Curios slot should unequip the ring");
        helper.assertTrue(menu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Shift-clicking from a Curios slot should prioritize accessory case storage");

        fillCompatibleCurioSlots(menu, ring);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "A stored ring should stay in the accessory case when every compatible Curios slot is full");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseHidesOversizedCuriosPanelAndAllowsUnlimitedOverride(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_oversized_layout_test"
        );
        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        expandCuriosBeyondDefaultColumnLimit(player);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);

        helper.assertTrue(
                menu.getVisibleCuriosColumnCount()
                        > SpellcasterAccessoryCaseMenu.DEFAULT_MAX_VISIBLE_CURIOS_COLUMNS,
                "Oversized Curios test should require more columns than the default client limit"
        );
        helper.assertFalse(menu.isCuriosPanelVisible(),
                "Curios panel should be hidden when its columns exceed the configured limit");
        helper.assertTrue(menu.getCuriosPanelWidth() == 0,
                "Hidden Curios panel should not extend the standard menu width");

        menu.configureMaxVisibleCuriosColumns(0);
        helper.assertTrue(menu.isCuriosPanelVisible(),
                "Zero Curios column limit should always show every slot");
        helper.assertTrue(menu.getCuriosPanelWidth() > 0,
                "Unlimited Curios panel should expose its layout width");

        menu.configureMaxVisibleCuriosColumns(SpellcasterAccessoryCaseMenu.DEFAULT_MAX_VISIBLE_CURIOS_COLUMNS);
        helper.assertFalse(menu.isCuriosPanelVisible(),
                "Restoring the default limit should hide the oversized Curios panel again");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseHiddenPanelQuickMoveOnlyTransfersStorageAndPlayerInventory(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_hidden_quick_move_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var caseInventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        helper.assertTrue(caseInventory.insertItem(
                0,
                new ItemStack(ItemRegistry.ATTACKCAST_RING.get()),
                false
        ).isEmpty(), "Accessory case should accept the hidden-panel quick-move test ring");
        player.getInventory().setItem(0, caseStack);
        expandCuriosBeyondDefaultColumnLimit(player);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);

        helper.assertFalse(menu.isCuriosPanelVisible(),
                "Hidden-panel quick-move test requires the Curios panel to be hidden");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "Shift-clicking hidden-panel storage should remove the stored ring");
        helper.assertTrue(player.getInventory().getItem(9).is(ItemRegistry.ATTACKCAST_RING.get()),
                "Shift-clicking hidden-panel storage should move the ring into player inventory");
        helper.assertTrue(menu.slots.stream()
                        .filter(CurioSlot.class::isInstance)
                        .noneMatch(slot -> slot.getItem().is(ItemRegistry.ATTACKCAST_RING.get())),
                "Hidden-panel shift-clicking must not equip the ring into an invisible Curios slot");

        menu.clicked(SpellcasterAccessoryCase.SLOT_COUNT, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Shift-clicking player inventory should return the ring to accessory case storage");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(),
                "Returning the ring to storage should clear its player inventory slot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseHiddenPanelRejectsCurioOriginQuickMove(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_hidden_curio_quick_move_test"
        );
        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        var curios = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for hidden-panel test"));
        curios.setEquippedCurio(RING_SLOT, 0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()));
        expandCuriosBeyondDefaultColumnLimit(player);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        var curioMenuSlot = findCurioMenuSlot(menu, RING_SLOT);

        helper.assertFalse(menu.isCuriosPanelVisible(),
                "Curio-origin quick-move rejection requires the panel to be hidden");
        helper.assertTrue(curioMenuSlot >= 0,
                "Hidden Curios slots must remain in the menu to preserve slot indices");
        var hiddenCurioSlot = menu.getSlot(curioMenuSlot);
        helper.assertFalse(menu.canTakeItemForPickAll(hiddenCurioSlot.getItem(), hiddenCurioSlot),
                "Pick-all must not unequip items from hidden Curios slots");
        menu.clicked(curioMenuSlot, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(curioMenuSlot).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Quick-moving from a hidden Curios slot should leave the equipped ring untouched");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "Rejected hidden Curios quick-move must not place the ring into storage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseHiddenPanelKeepsStorageWhenPlayerInventoryIsFull(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_hidden_full_inventory_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var caseInventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        helper.assertTrue(caseInventory.insertItem(
                0,
                new ItemStack(ItemRegistry.ATTACKCAST_RING.get()),
                false
        ).isEmpty(), "Accessory case should accept the full-inventory test ring");
        player.getInventory().setItem(0, caseStack);
        for (var slot = 1; slot < Inventory.INVENTORY_SIZE; ++slot) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, Items.STONE.getDefaultMaxStackSize()));
        }
        expandCuriosBeyondDefaultColumnLimit(player);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);

        helper.assertFalse(menu.isCuriosPanelVisible(),
                "Full-inventory test requires the Curios panel to be hidden");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(0).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Hidden-panel shift-click should keep the item in storage when player inventory is full");
        helper.assertTrue(menu.slots.stream()
                        .filter(CurioSlot.class::isInstance)
                        .noneMatch(slot -> slot.getItem().is(ItemRegistry.ATTACKCAST_RING.get())),
                "A full player inventory must not make hidden-panel shift-click fall back to Curios");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseUnlimitedPanelKeepsCuriosQuickMove(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_unlimited_quick_move_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        var caseInventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, player);
        var ring = new ItemStack(ItemRegistry.ATTACKCAST_RING.get());
        helper.assertTrue(caseInventory.insertItem(0, ring.copy(), false).isEmpty(),
                "Accessory case should accept the unlimited-panel quick-move test ring");
        player.getInventory().setItem(0, caseStack);
        expandCuriosBeyondDefaultColumnLimit(player);
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        menu.configureMaxVisibleCuriosColumns(0);
        var curioMenuSlot = findFirstAvailableCurioMenuSlot(menu, ring);

        helper.assertTrue(menu.isCuriosPanelVisible(),
                "Zero column limit should show the oversized Curios panel");
        helper.assertTrue(curioMenuSlot >= 0,
                "Unlimited oversized panel should expose a compatible Curios slot");
        var visibleCurioSlot = menu.getSlot(curioMenuSlot);
        helper.assertTrue(menu.canTakeItemForPickAll(ring, visibleCurioSlot),
                "Pick-all should keep including visible Curios slots");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(curioMenuSlot).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "Visible unlimited panel should keep storage-to-Curios quick move behavior");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseCurioQuickMoveFallsBackToPlayerInventory(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_quick_move_fallback_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        fillAccessoryCase(caseStack, player);
        player.getInventory().setItem(0, caseStack);
        var curios = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for quick-move fallback test"));
        curios.setEquippedCurio(RING_SLOT, 0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()));
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        var curioMenuSlot = findCurioMenuSlot(menu, RING_SLOT);

        helper.assertTrue(curioMenuSlot >= 0,
                "Accessory case menu should expose the equipped ring slot");
        menu.clicked(curioMenuSlot, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(curioMenuSlot).getItem().isEmpty(),
                "Shift-clicking an equipped ring should unequip it when player inventory has room");
        helper.assertTrue(playerInventoryContains(player.getInventory(), ItemRegistry.ATTACKCAST_RING.get()),
                "A Curios item should fall back to player inventory when accessory case storage is full");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void accessoryCaseCurioQuickMoveStaysWhenAllDestinationsAreFull(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_quick_move_full_test"
        );
        var caseStack = new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get());
        fillAccessoryCase(caseStack, player);
        player.getInventory().setItem(0, caseStack);
        for (var slot = 1; slot < Inventory.INVENTORY_SIZE; ++slot) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, Items.STONE.getDefaultMaxStackSize()));
        }
        var curios = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for full destination test"));
        curios.setEquippedCurio(RING_SLOT, 0, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()));
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        var curioMenuSlot = findCurioMenuSlot(menu, RING_SLOT);

        helper.assertTrue(curioMenuSlot >= 0,
                "Accessory case menu should expose the equipped ring slot");
        menu.clicked(curioMenuSlot, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(curioMenuSlot).getItem().is(ItemRegistry.ATTACKCAST_RING.get()),
                "An equipped ring should stay in its Curios slot when storage and player inventory are full");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void accessoryCaseMenuRebuildsWhenCuriosSlotCountChanges(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_dynamic_slot_test"
        );
        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        player.containerMenu = menu;
        var oldMenuSlotCount = menu.slots.size();
        var curios = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for accessory case test"));
        var oldBackSlots = curios.getCurios().get(CuriosSlotConstants.BACK).getStacks().getSlots();

        curios.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                EXPANDED_BACK_SLOT_ID,
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        );

        helper.runAfterDelay(2, () -> {
            var newBackSlots = curios.getCurios().get(CuriosSlotConstants.BACK).getStacks().getSlots();
            helper.assertTrue(newBackSlots == oldBackSlots + 1,
                    "Curios back slot modifier should add one slot");
            helper.assertTrue(menu.slots.size() == oldMenuSlotCount + 1,
                    "Accessory case menu should rebuild after a Curios slot count change");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void accessoryCaseHonorsMalumBroochAndGeasContracts(GameTestHelper helper) {
        if (!ModList.get().isLoaded("malum")) {
            helper.succeed();
            return;
        }

        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "accessory_case_malum_test"
        );
        player.getInventory().setItem(0, new ItemStack(ItemRegistry.SPELLCASTER_ACCESSORY_CASE.get()));
        var menu = new SpellcasterAccessoryCaseMenu(1, player.getInventory(), 0);
        player.containerMenu = menu;
        var curios = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for Malum accessory case test"));
        var runicBrooch = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("malum", "runic_brooch")
        ).orElseThrow(() -> new IllegalStateException("Missing Malum runic brooch"));
        var geas = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("malum", "geas")
        ).orElseThrow(() -> new IllegalStateException("Missing Malum geas"));

        helper.assertTrue(SpellcasterAccessoryCase.accepts(new ItemStack(runicBrooch), player),
                "Accessory case should accept Malum's runic brooch");
        helper.assertFalse(SpellcasterAccessoryCase.accepts(new ItemStack(geas), player),
                "Accessory case should reject Malum's Geas because it has no equippable Curios slot");

        var oldRingSlots = curios.getCurios().get("ring").getStacks().getSlots();
        var oldRuneSlots = curios.getCurios().get("rune").getStacks().getSlots();
        curios.setEquippedCurio("brooch", 0, new ItemStack(runicBrooch));

        // FakePlayerはlevelのentity tick対象外なので、実ゲームと同じCuriosのPost tick処理を明示的に進める。
        ++player.tickCount;
        new CuriosEventHandler().tick(new EntityTickEvent.Post(player));

        helper.runAfterDelay(3, () -> {
            var newRingSlots = curios.getCurios().get("ring").getStacks().getSlots();
            var newRuneSlots = curios.getCurios().get("rune").getStacks().getSlots();
            helper.assertTrue(newRingSlots == Math.max(0, oldRingSlots - 1),
                    "Malum's runic brooch should remove one ring slot: " + oldRingSlots + " -> " + newRingSlots);
            helper.assertTrue(newRuneSlots == oldRuneSlots + 2,
                    "Malum's runic brooch should add two rune slots: " + oldRuneSlots + " -> " + newRuneSlots);

            curios.addTransientSlotModifier(
                    "geas",
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/accessory_case_geas"),
                    1.0D,
                    AttributeModifier.Operation.ADD_VALUE
            );
            curios.setEquippedCurio("geas", 0, new ItemStack(geas));

            helper.runAfterDelay(2, () -> {
                var geasSlot = menu.slots.stream()
                        .filter(CurioSlot.class::isInstance)
                        .map(CurioSlot.class::cast)
                        .filter(slot -> slot.getIdentifier().equals("geas"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Accessory case menu did not expose the Geas slot"));
                helper.assertFalse(geasSlot.mayPickup(player),
                        "Accessory case menu must preserve Malum Geas' unequip prohibition");
                menu.clicked(menu.slots.indexOf(geasSlot), 0, ClickType.QUICK_MOVE, player);
                helper.assertTrue(geasSlot.getItem().is(geas),
                        "Shift-clicking must not bypass Malum Geas' unequip prohibition");
                helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                        "A prohibited Malum Geas must not move into accessory case storage");
                helper.succeed();
            });
        });
    }

    private static SpellcasterAccessoryCaseBlockEntity placeAccessoryCaseBlock(
            GameTestHelper helper,
            BlockPos pos,
            ItemStack caseStack
    ) {
        helper.getLevel().setBlockAndUpdate(pos, BlockRegistry.SPELLCASTER_ACCESSORY_CASE.get().defaultBlockState());
        var blockEntity = helper.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof SpellcasterAccessoryCaseBlockEntity accessoryCase)) {
            throw new IllegalStateException("Placed accessory case test block is missing its block entity");
        }
        accessoryCase.setCaseStack(caseStack);
        return accessoryCase;
    }

    private static int findFirstAvailableCurioMenuSlot(SpellcasterAccessoryCaseMenu menu, ItemStack stack) {
        for (var menuSlot = 0; menuSlot < menu.slots.size(); ++menuSlot) {
            var slot = menu.getSlot(menuSlot);
            if (slot instanceof CurioSlot && !slot.hasItem() && slot.mayPlace(stack)) {
                return menuSlot;
            }
        }
        return -1;
    }

    private static int findCurioMenuSlot(SpellcasterAccessoryCaseMenu menu, String identifier) {
        for (var menuSlot = 0; menuSlot < menu.slots.size(); ++menuSlot) {
            if (menu.getSlot(menuSlot) instanceof CurioSlot curioSlot
                    && curioSlot.getIdentifier().equals(identifier)
                    && curioSlot.getSlotIndex() == 0) {
                return menuSlot;
            }
        }
        return -1;
    }

    private static void fillCompatibleCurioSlots(SpellcasterAccessoryCaseMenu menu, ItemStack stack) {
        for (var slot : menu.slots) {
            if (slot instanceof CurioSlot && !slot.hasItem() && slot.mayPlace(stack)) {
                slot.set(stack.copy());
            }
        }
    }

    private static void expandCuriosBeyondDefaultColumnLimit(
            net.minecraft.world.entity.LivingEntity wearer
    ) {
        var curios = CuriosApi.getCuriosInventory(wearer)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for oversized-panel test"));
        var currentVisibleSlots = curios.getCurios().values().stream()
                .filter(top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler::isVisible)
                .mapToInt(handler -> handler.getStacks().getSlots())
                .sum();
        var firstUnsupportedSlotCount =
                SpellcasterAccessoryCaseMenu.DEFAULT_MAX_VISIBLE_CURIOS_COLUMNS * 8 + 1;
        var addedSlots = Math.max(1, firstUnsupportedSlotCount - currentVisibleSlots);
        curios.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                OVERSIZED_BACK_SLOT_ID,
                addedSlots,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    private static void fillAccessoryCase(ItemStack caseStack, net.minecraft.world.entity.LivingEntity wearer) {
        var inventory = new SpellcasterAccessoryCase.CaseInventory(caseStack, wearer);
        for (var slot = 0; slot < SpellcasterAccessoryCase.SLOT_COUNT; ++slot) {
            var remainder = inventory.insertItem(slot, new ItemStack(ItemRegistry.ATTACKCAST_RING.get()), false);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("Failed to fill accessory case storage for quick-move test");
            }
        }
    }

    private static boolean playerInventoryContains(Inventory inventory, net.minecraft.world.item.Item item) {
        for (var slot = 0; slot < Inventory.INVENTORY_SIZE; ++slot) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }
}
