package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexAlchemyBrewerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private ApprenticeCodexAlchemyBrewerGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void initialRecipesAreLoaded(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        helper.assertValueEqual(manager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get()).size(), 12,
                "dedicated brewing recipe count");
        helper.assertValueEqual(manager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get()).size(), 8,
                "potion modifier recipe count");
        helper.assertTrue(new ItemStack(Items.NETHER_WART).is(TagRegistry.Items.ALCHEMY_BREWER_HIGH_EFFICIENCY_BASES),
                "Nether Wart must be included in the high-efficiency base tag");
        helper.assertTrue(new ItemStack(Items.GLOW_LICHEN).is(TagRegistry.Items.ALCHEMY_BREWER_FAST_BASES),
                "Glow Lichen must be included in the fast base tag");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void collisionShapeKeepsAFlatTenPixelWalkingSurface(GameTestHelper helper) {
        var state = BlockRegistry.ALCHEMY_BREWER.get().defaultBlockState();
        var worldPos = helper.absolutePos(new BlockPos(1, 1, 1));
        var collision = state.getCollisionShape(helper.getLevel(), worldPos, CollisionContext.empty());
        var outline = state.getShape(helper.getLevel(), worldPos, CollisionContext.empty());

        helper.assertValueEqual(collision.max(Direction.Axis.Y), 10.0D / 16.0D, "walking surface height");
        helper.assertTrue(outline.max(Direction.Axis.Y) > collision.max(Direction.Axis.Y),
                "Outline shape must include the detailed upper geometry");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void previewIsCalculatedWhileAutoBrewingIsOff(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.ALCHEMY_BREWER.get());
        var brewer = (AlchemyBrewerBlockEntity) helper.getBlockEntity(pos);
        brewer.getInventory().setStackInSlot(1, new ItemStack(Items.GLOW_LICHEN));
        brewer.getInventory().setStackInSlot(2, new ItemStack(Items.SUGAR));

        helper.runAfterDelay(2, () -> {
            helper.assertFalse(brewer.isAutoBrewing(), "Auto brewing must be disabled when placed");
            helper.assertFalse(brewer.isProcessing(), "Processing must not begin while auto brewing is disabled");
            helper.assertTrue(brewer.isDisplayPreview(), "Result preview must be visible while auto brewing is disabled");
            helper.assertValueEqual(brewer.getDisplayPotionId(), ResourceLocation.parse("minecraft:swiftness"),
                    "preview potion");
            helper.assertValueEqual(brewer.getDisplayAmountMb(), 750, "preview amount");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 280)
    public static void autoModeReservesAndProducesModifiedPotion(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BlockRegistry.ALCHEMY_BREWER.get());
        var brewer = (AlchemyBrewerBlockEntity) helper.getBlockEntity(pos);
        brewer.getInventory().setStackInSlot(1, new ItemStack(Items.GLOWSTONE_DUST));
        brewer.getInventory().setStackInSlot(2, new ItemStack(Items.NETHER_WART));
        brewer.getInventory().setStackInSlot(3, new ItemStack(Items.SUGAR));

        helper.runAfterDelay(25, () -> helper.assertFalse(brewer.isProcessing(), "Auto brewing must be disabled initially"));
        helper.runAfterDelay(26, brewer::toggleAutoBrewing);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(brewer.isProcessing(), "Processing must begin after the preview remains valid for 20 ticks");
            helper.assertTrue(brewer.getInventory().getStackInSlot(1).isEmpty(), "Modifier must be reserved and consumed");
            helper.assertTrue(brewer.getInventory().getStackInSlot(2).isEmpty(), "Base ingredient must be reserved and consumed");
            helper.assertTrue(brewer.getInventory().getStackInSlot(3).isEmpty(), "Potion ingredient must be reserved and consumed");
        });
        helper.runAfterDelay(250, () -> {
            helper.assertFalse(brewer.isProcessing(), "Processing must stop after completion");
            helper.assertValueEqual(brewer.getTankAmountMb(), 1000, "Nether Wart output amount");
            helper.assertValueEqual(brewer.getTankPotionId(), ResourceLocation.parse("minecraft:strong_swiftness"), "brewed potion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void atelierStationCollectsMatchingBrewerTankWithOrFilters(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1), 0, Potions.REGENERATION.value());
        station.setFilter(0, potionStack(Potions.REGENERATION.value()));
        station.setFilter(1, potionStack(Potions.SWIFTNESS.value()));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(station.getStoredFluidAmount(), 1000, "collected Atelier Station amount");
            helper.assertValueEqual(brewer.getTankAmountMb(), 0, "drained Alchemy Brewer amount");
            helper.assertTrue(brewer.getTankPotionId() == null, "Empty Alchemy Brewer must clear its potion id");
            helper.assertTrue(station.getStoredFluidsForDisplay().stream().anyMatch(entry ->
                            entry.amountMb() == 1000
                                    && ItemStack.isSameItemSameComponents(
                                    entry.representativeItem(), potionStack(Potions.SWIFTNESS.value()))),
                    "Atelier Station must store the potion matched by either filter");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void atelierStationLeavesMismatchedBrewerTankUntouched(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1), 0, Potions.REGENERATION.value());
        station.setFilter(0, potionStack(Potions.REGENERATION.value()));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.runAfterDelay(25, () -> {
            helper.assertValueEqual(station.getStoredFluidAmount(), 0, "mismatched Atelier Station amount");
            helper.assertValueEqual(brewer.getTankAmountMb(), 1000, "mismatched Alchemy Brewer amount");
            helper.assertValueEqual(brewer.getTankPotionId(), ResourceLocation.parse("minecraft:swiftness"),
                    "mismatched Alchemy Brewer potion id");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void atelierStationCollectsOnlyItsRemainingCapacityFromBrewer(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1), 15750, Potions.REGENERATION.value());
        station.setFilter(0, potionStack(Potions.SWIFTNESS.value()));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(station.getStoredFluidAmount(), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT,
                    "capacity-limited Atelier Station amount");
            helper.assertValueEqual(brewer.getTankAmountMb(), 750, "capacity-limited Alchemy Brewer amount");
            helper.assertValueEqual(brewer.getTankPotionId(), ResourceLocation.parse("minecraft:swiftness"),
                    "remaining Alchemy Brewer potion id");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fullAtelierStationLeavesBrewerTankUntouched(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1),
                AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT, Potions.REGENERATION.value());
        station.setFilter(0, potionStack(Potions.SWIFTNESS.value()));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.runAfterDelay(25, () -> {
            helper.assertValueEqual(station.getStoredFluidAmount(), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT,
                    "full Atelier Station amount");
            helper.assertValueEqual(brewer.getTankAmountMb(), 1000, "full-station Alchemy Brewer amount");
            helper.assertValueEqual(brewer.getTankPotionId(), ResourceLocation.parse("minecraft:swiftness"),
                    "full-station Alchemy Brewer potion id");
            helper.succeed();
        });
    }

    private static AtelierStationBlockEntity placeAtelierStation(
            GameTestHelper helper,
            BlockPos pos,
            int storedAmountMb,
            Potion storedPotion
    ) {
        helper.setBlock(pos, BlockRegistry.ATELIER_STATION.get());
        var station = (AtelierStationBlockEntity) helper.getBlockEntity(pos);
        if (storedAmountMb <= 0) {
            return station;
        }

        var storedFluid = new CompoundTag();
        storedFluid.put("Item", potionStack(storedPotion).saveOptional(helper.getLevel().registryAccess()));
        storedFluid.putInt("Amount", storedAmountMb);
        var storedFluids = new ListTag();
        storedFluids.add(storedFluid);
        var tag = new CompoundTag();
        tag.put("StoredFluids", storedFluids);
        station.loadWithComponents(tag, helper.getLevel().registryAccess());
        return station;
    }

    private static AlchemyBrewerBlockEntity placeAlchemyBrewer(
            GameTestHelper helper,
            BlockPos pos,
            String potionId,
            int amountMb
    ) {
        helper.setBlock(pos, BlockRegistry.ALCHEMY_BREWER.get());
        var brewer = (AlchemyBrewerBlockEntity) helper.getBlockEntity(pos);
        var tag = new CompoundTag();
        tag.putString("TankPotion", potionId);
        tag.putInt("TankAmountMb", amountMb);
        brewer.loadWithComponents(tag, helper.getLevel().registryAccess());
        return brewer;
    }

    private static ItemStack potionStack(Potion potion) {
        return PotionContentsHelper.createPotionStack(Items.POTION, potion);
    }
}
