package jp.aquafactory.apprenticecodex.gametest.equipment.broom.lifecycle;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.BroomSpellSelectionEvents;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import jp.aquafactory.apprenticecodex.item.broom.BroomDeploymentState;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentEvents;
import jp.aquafactory.apprenticecodex.spell.callbroom.CallBroomDeploymentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.assertCalibrationContents;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.backCurioBroomStack;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.backCurioCalibratedBroomStack;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.calibration.BroomCalibrationTestSupport.installBroomCalibration;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.equipmentPlayer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.magicData;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CallBroomGameTests {
    private CallBroomGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void equippedBroomAddsCallBroomSpellSelection(GameTestHelper helper) {
        var player = equipmentPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "call_broom_spell_selection_test"
        );
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for spell selection test"));

        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Call Broom should not be selected without an equipped broom");
        var floatmount = backCurioCalibratedBroomStack(ItemRegistry.FLOATMOUNT_BROOM.get(), 1);
        curiosInventory.setEquippedCurio(
                CuriosSlotConstants.BACK,
                0,
                floatmount
        );
        assertSingleCallBroomSelection(helper, player, "Floatmount Broom");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                "A calibrated Floatmount Broom should not expose scrolls while only equipped as a Curio");
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        floatmount, 2, ItemStack.EMPTY),
                "Equipped broom should allow removing its Curios calibration");
        helper.assertTrue(BroomCurioSupport.findUniqueEquippedBroom(player).isEmpty(),
                "Broom should stop being a valid casting source after Curios calibration is removed");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Call Broom should disappear on the next spell selection update");
        helper.assertFalse(SpellRegistry.CALL_BROOM.get().checkPreCastConditions(
                        helper.getLevel(), 1, player, magicData(helper, player)),
                "Call Broom should reject an equipped broom whose Curios calibration was removed");

        var hoverride = backCurioCalibratedBroomStack(ItemRegistry.HOVERRIDE_BROOM.get(), 1);
        curiosInventory.setEquippedCurio(
                CuriosSlotConstants.BACK,
                0,
                hoverride
        );
        assertSingleCallBroomSelection(helper, player, "Hoverride Broom");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomSpellSelectionEvents.SPELL_SELECTION_SLOT).isEmpty(),
                "A calibrated Hoverride Broom should not expose scrolls while only equipped as a Curio");
        curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, ItemStack.EMPTY);
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Call Broom should be removed after unequipping the broom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidBroomEquipmentDoesNotAddCallBroomSelection(GameTestHelper helper) {
        var player = equipmentPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "invalid_broom_spell_selection_test"
        );
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for invalid broom test"));
        curiosInventory.addTransientSlotModifier(
                CuriosSlotConstants.BACK,
                ResourceLocation.fromNamespaceAndPath("apprenticecodex", "gametest/invalid_broom_back"),
                1.0D,
                AttributeModifier.Operation.ADD_VALUE
        );
        var backHandler = curiosInventory.getCurios().get(CuriosSlotConstants.BACK);
        helper.assertTrue(backHandler != null && backHandler.getStacks().getSlots() >= 2,
                "Invalid broom test should provide two back slots");
        backHandler.getStacks().setStackInSlot(0, backCurioBroomStack(ItemRegistry.FLOATMOUNT_BROOM.get()));
        backHandler.getStacks().setStackInSlot(1, backCurioBroomStack(ItemRegistry.HOVERRIDE_BROOM.get()));

        helper.assertTrue(BroomCurioSupport.findUniqueEquippedBroom(player).isEmpty(),
                "Multiple equipped brooms should not resolve as a valid casting source");
        helper.assertTrue(new SpellSelectionManager(player)
                        .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT).isEmpty(),
                "Invalid duplicate broom equipment should not add Call Broom");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void callBroomRequiresEquippedBroomAndIgnoresFloatmountMountThreshold(GameTestHelper helper) {
        var player = equipmentPlayer(
                helper,
                new BlockPos(0, 2, 0),
                "call_broom_precondition_test"
        );
        var magicData = magicData(helper, player);
        magicData.setMana(20.0F);
        helper.assertFalse(SpellRegistry.CALL_BROOM.get().checkPreCastConditions(
                        helper.getLevel(), 1, player, magicData),
                "Call Broom obtained directly must fail without an equipped broom");

        var stack = equipBroom(player, ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(SpellRegistry.CALL_BROOM.get().checkPreCastConditions(
                        helper.getLevel(), 1, player, magicData),
                "Call Broom should ignore the higher Floatmount mount threshold");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                "Precondition checks must not mutate deployment state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void callBroomDeploysAndRecallsBothTypesWithoutItemizing(GameTestHelper helper) {
        assertCallBroomDeployAndRecall(helper, ItemRegistry.FLOATMOUNT_BROOM.get(), 0, "Floatmount");
        assertCallBroomDeployAndRecall(helper, ItemRegistry.HOVERRIDE_BROOM.get(), 2, "Hoverride");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calledBroomRejectsThirdPartyAndRecallsBeyondEightBlocks(GameTestHelper helper) {
        var owner = calledBroomPlayer(helper, new BlockPos(0, 2, 0), "called_broom_owner_test");
        var thirdParty = calledBroomPlayer(helper, new BlockPos(1, 2, 0), "called_broom_third_party_test");
        var stack = equipBroom(owner, ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(CallBroomDeploymentManager.execute(owner), "Call Broom should deploy for its owner");
        var broom = (AbstractBroomEntity) owner.getVehicle();
        owner.stopRiding();

        thirdParty.setShiftKeyDown(true);
        helper.assertTrue(broom.interact(thirdParty, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
                "A third party should not recover a called broom");
        thirdParty.setShiftKeyDown(false);
        helper.assertTrue(broom.interact(thirdParty, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME,
                "A third party should not mount a called broom");
        helper.assertFalse(thirdParty.isPassenger(), "Rejected third party must remain dismounted");
        helper.assertFalse(broom.isRemoved(), "Rejected interaction must leave the called broom deployed");

        broom.setPos(owner.getX() + 8.01D, owner.getY(), owner.getZ());
        broom.tick();
        helper.assertTrue(broom.isRemoved(), "An unoccupied called broom should recall beyond eight blocks");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                "Distance recall should restore the equipped broom rendering state");
        helper.assertTrue(countBroomItems(thirdParty) == 0,
                "Distance recall and rejected recovery must not create a broom item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calledBroomUnequipAndLogoutPreserveOnlyMountedRootVehicle(GameTestHelper helper) {
        var player = calledBroomPlayer(helper, new BlockPos(0, 2, 0), "called_broom_logout_test");
        var stack = equipBroom(player, ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(CallBroomDeploymentManager.execute(player), "Call Broom should deploy before unequip");
        var first = (AbstractBroomEntity) player.getVehicle();
        ((AbstractBroomItem) stack.getItem()).onUnequip(
                new SlotContext(CuriosSlotConstants.BACK, player, 0, false, true),
                ItemStack.EMPTY,
                stack
        );
        helper.assertTrue(first.isRemoved(), "Unequipping should recall the called broom even while mounted");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack), "Unequipping should clear deployment state");

        helper.assertTrue(CallBroomDeploymentManager.execute(player), "Call Broom should redeploy after cleanup");
        var mounted = (AbstractBroomEntity) player.getVehicle();
        CallBroomDeploymentEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertFalse(mounted.isRemoved(), "Mounted logout should leave the broom for vanilla RootVehicle saving");
        helper.assertTrue(BroomDeploymentState.matches(stack, mounted.getUUID()),
                "Mounted logout should preserve the RootVehicle deployment UUID");

        player.stopRiding();
        CallBroomDeploymentEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.assertTrue(mounted.isRemoved(), "Unmounted logout should recall the called broom");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                "Unmounted logout should clear deployment state before player saving");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void calledBroomDamageAndExternalNameNeverCreateReplacementItem(GameTestHelper helper) {
        var player = calledBroomPlayer(helper, new BlockPos(0, 2, 0), "called_broom_damage_test");
        var originalName = Component.literal("Original Broom");
        var stack = equipBroom(player, ItemRegistry.FLOATMOUNT_BROOM.get());
        stack.set(DataComponents.CUSTOM_NAME, originalName);
        helper.assertTrue(CallBroomDeploymentManager.execute(player), "Named called broom should deploy");
        var broom = (AbstractBroomEntity) player.getVehicle();
        broom.setCustomName(Component.literal("External Entity Name"));
        broom.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
        player.stopRiding();

        helper.assertTrue(broom.isRemoved(), "A damaged called broom should recall after dismount");
        helper.assertTrue(originalName.equals(stack.get(DataComponents.CUSTOM_NAME)),
                "Entity-side renaming must not overwrite the original equipped item name");
        helper.assertTrue(countBroomItems(player) == 0,
                "Damaged called broom recall must not create a replacement item");
        helper.succeed();
    }

    private static void assertSingleCallBroomSelection(GameTestHelper helper, Player player, String broomName) {
        var selections = new SpellSelectionManager(player)
                .getSpellsForSlot(BroomCurioSupport.SPELL_SELECTION_SLOT);
        helper.assertTrue(selections.size() == 1,
                broomName + " should add exactly one Call Broom selection");
        helper.assertTrue(selections.getFirst().spellData.getSpell() == SpellRegistry.CALL_BROOM.get(),
                broomName + " should add the Call Broom spell");
    }

    private static ItemStack equipBroom(ServerPlayer player, Item item) {
        var stack = backCurioBroomStack(item);
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for called broom test"));
        curiosInventory.setEquippedCurio(CuriosSlotConstants.BACK, 0, stack);
        return stack;
    }

    private static void assertCallBroomDeployAndRecall(
            GameTestHelper helper,
            Item item,
            int xOffset,
            String broomName
    ) {
        var player = calledBroomPlayer(
                helper,
                new BlockPos(xOffset, 2, 0),
                "called_" + broomName.toLowerCase() + "_test"
        );
        var originalName = Component.literal(broomName + " Original");
        var stack = equipBroom(player, item);
        stack.set(DataComponents.CUSTOM_NAME, originalName);
        installBroomCalibration(stack, 2);

        helper.assertTrue(CallBroomDeploymentManager.execute(player), broomName + " should deploy");
        helper.assertTrue(player.getVehicle() instanceof AbstractBroomEntity,
                broomName + " should mount its dedicated broom immediately");
        var broom = (AbstractBroomEntity) player.getVehicle();
        helper.assertTrue(broom.isOwnedBy(player), broomName + " should retain its owner UUID");
        helper.assertTrue(broom.matchesBroomItem(stack), broomName + " should retain its entity type");
        helper.assertTrue(BroomDeploymentState.matches(stack, broom.getUUID()),
                broomName + " stack should store the deployed entity UUID");
        helper.assertTrue(originalName.equals(broom.getCustomName()),
                broomName + " entity should initially copy the equipped item name");
        assertCalibrationContents(helper, broom.getBroomItemStack(), 2,
                broomName + " called broom entity");
        helper.assertFalse(BroomDeploymentState.isDeployed(broom.getBroomItemStack()),
                broomName + " entity copy must not retain the Curio deployment UUID");

        player.stopRiding();
        broom.setCustomName(Component.literal(broomName + " External"));
        helper.assertTrue(CallBroomDeploymentManager.execute(player), broomName + " should recall while unoccupied");
        helper.assertTrue(broom.isRemoved(), broomName + " recall should remove its entity");
        helper.assertFalse(BroomDeploymentState.isDeployed(stack),
                broomName + " recall should clear deployment state");
        helper.assertTrue(originalName.equals(stack.get(DataComponents.CUSTOM_NAME)),
                broomName + " recall must preserve the original item name");
        assertCalibrationContents(helper, stack, 2, broomName + " recalled Curio stack");
        helper.assertTrue(countBroomItems(player) == 0,
                broomName + " recall must not grant a replacement item");
    }

    private static int countBroomItems(Player player) {
        var count = 0;
        for (var slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (BroomCurioSupport.isBroom(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static ServerPlayer calledBroomPlayer(GameTestHelper helper, BlockPos relativePos, String profileName) {
        var player = serverRider(helper, profileName);
        var position = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos));
        player.setPos(position.x, position.y, position.z);
        return player;
    }
}
