package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private static final ResourceLocation EXPANDED_BACK_SLOT_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "gametest/accessory_case_expanded_back"
    );

    private SpellcasterAccessoryCaseGameTests() {
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
                helper.succeed();
            });
        });
    }
}
