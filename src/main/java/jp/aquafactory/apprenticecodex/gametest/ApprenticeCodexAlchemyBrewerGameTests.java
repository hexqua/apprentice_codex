package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import io.redspace.ironsspellbooks.fluids.PotionFluid;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerBlockEntity;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationBlockEntity;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.block.AlchemyBrewerServerConfig;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.recipe.alchemybrewer.AlchemyBrewerRecipe;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.capability.IFluidHandler;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexAlchemyBrewerGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String VANILLA_INCREMENT_CONFIG_BATCH =
            "apprenticecodex.alchemy_brewer_vanilla_increment_config";
    private static final String IRON_CAULDRON_CONFIG_BATCH =
            "apprenticecodex.alchemy_brewer_iron_cauldron_config";
    private static final String WATER_DISABLED_CONFIG_BATCH =
            "apprenticecodex.alchemy_brewer_water_disabled_config";
    private ApprenticeCodexAlchemyBrewerGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void initialRecipesAreLoaded(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        assertValueEqual(helper, manager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_RECIPE_TYPE.get()).size(), 12,
                "dedicated brewing recipe count");
        assertValueEqual(helper, manager.getAllRecipesFor(RecipeRegistry.ALCHEMY_BREWER_MODIFIER_RECIPE_TYPE.get()).size(), 8,
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

        assertValueEqual(helper, collision.max(Direction.Axis.Y), 10.0D / 16.0D, "walking surface height");
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
            assertValueEqual(helper, brewer.getDisplayPotionId(), ResourceLocation.tryParse("minecraft:swiftness"),
                    "preview potion");
            assertValueEqual(helper, brewer.getDisplayAmountMb(), 750, "preview amount");
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
            assertValueEqual(helper, brewer.getTankAmountMb(), 1000, "Nether Wart output amount");
            assertValueEqual(helper, brewer.getTankPotionId(), ResourceLocation.tryParse("minecraft:strong_swiftness"), "brewed potion");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void atelierStationCollectsMatchingBrewerTankWithOrFilters(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1), 0, Potions.REGENERATION);
        station.setFilter(0, potionStack(Potions.REGENERATION));
        station.setFilter(1, potionStack(Potions.SWIFTNESS));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.succeedWhen(() -> {
            assertValueEqual(helper, station.getStoredFluidAmount(), 1000, "collected Atelier Station amount");
            assertValueEqual(helper, brewer.getTankAmountMb(), 0, "drained Alchemy Brewer amount");
            helper.assertTrue(brewer.getTankPotionId() == null, "Empty Alchemy Brewer must clear its potion id");
            helper.assertTrue(station.getStoredFluidsForDisplay().stream().anyMatch(entry ->
                            entry.amountMb() == 1000
                                    && ItemStack.isSameItemSameTags(
                                    entry.representativeItem(), potionStack(Potions.SWIFTNESS))),
                    "Atelier Station must store the potion matched by either filter");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void atelierStationLeavesMismatchedBrewerTankUntouched(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1), 0, Potions.REGENERATION);
        station.setFilter(0, potionStack(Potions.REGENERATION));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.runAfterDelay(25, () -> {
            assertValueEqual(helper, station.getStoredFluidAmount(), 0, "mismatched Atelier Station amount");
            assertValueEqual(helper, brewer.getTankAmountMb(), 1000, "mismatched Alchemy Brewer amount");
            assertValueEqual(helper, brewer.getTankPotionId(), ResourceLocation.tryParse("minecraft:swiftness"),
                    "mismatched Alchemy Brewer potion id");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void atelierStationCollectsOnlyItsRemainingCapacityFromBrewer(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1), 15750, Potions.REGENERATION);
        station.setFilter(0, potionStack(Potions.SWIFTNESS));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.succeedWhen(() -> {
            assertValueEqual(helper, station.getStoredFluidAmount(), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT,
                    "capacity-limited Atelier Station amount");
            assertValueEqual(helper, brewer.getTankAmountMb(), 750, "capacity-limited Alchemy Brewer amount");
            assertValueEqual(helper, brewer.getTankPotionId(), ResourceLocation.tryParse("minecraft:swiftness"),
                    "remaining Alchemy Brewer potion id");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fullAtelierStationLeavesBrewerTankUntouched(GameTestHelper helper) {
        var station = placeAtelierStation(helper, new BlockPos(1, 1, 1),
                AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT, Potions.REGENERATION);
        station.setFilter(0, potionStack(Potions.SWIFTNESS));
        var brewer = placeAlchemyBrewer(helper, new BlockPos(2, 1, 1), "minecraft:swiftness", 1000);

        helper.runAfterDelay(25, () -> {
            assertValueEqual(helper, station.getStoredFluidAmount(), AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT,
                    "full Atelier Station amount");
            assertValueEqual(helper, brewer.getTankAmountMb(), 1000, "full-station Alchemy Brewer amount");
            assertValueEqual(helper, brewer.getTankPotionId(), ResourceLocation.tryParse("minecraft:swiftness"),
                    "full-station Alchemy Brewer potion id");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void glassBottleCollectsOneDoseFromBrewer(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", 250);
        var player = createPlayer(helper, "alchemy_brewer_single_bottle", pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));

        var result = useBrewer(helper, player, pos);

        helper.assertTrue(result.consumesAction(), "Glass bottle interaction must be consumed");
        helper.assertTrue(ItemStack.isSameItemSameTags(
                        player.getMainHandItem(), potionStack(Potions.SWIFTNESS)),
                "A single glass bottle must be replaced with the brewed potion");
        assertValueEqual(helper, brewer.getTankAmountMb(), 0, "remaining potion amount");
        helper.assertTrue(brewer.getTankPotionId() == null, "Empty tank must clear its potion id");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void processingTimeRejectsValuesOutsideContainerDataRange(GameTestHelper helper) {
        var result = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "processing_time_boundary");
        var maximumRecipe = new AlchemyBrewerRecipe(
                result,
                Ingredient.of(Items.NETHER_WART),
                Ingredient.of(Items.SUGAR),
                result,
                250,
                AlchemyBrewerRecipe.MAX_PROCESSING_TIME_TICKS,
                0
        );
        assertValueEqual(helper, maximumRecipe.processingTimeTicks(), AlchemyBrewerRecipe.MAX_PROCESSING_TIME_TICKS,
                "maximum synchronized processing time");

        var rejected = false;
        try {
            new AlchemyBrewerRecipe(
                    result,
                    Ingredient.of(Items.NETHER_WART),
                    Ingredient.of(Items.SUGAR),
                    result,
                    250,
                    AlchemyBrewerRecipe.MAX_PROCESSING_TIME_TICKS + 1,
                    0
            );
        } catch (IllegalArgumentException ignored) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Processing times above signed 16-bit ContainerData range must be rejected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stackedGlassBottleDropsPotionWhenInventoryIsFull(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", 500);
        var player = createPlayer(helper, "alchemy_brewer_full_inventory", pos);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); ++slot) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));

        useBrewer(helper, player, pos);

        assertValueEqual(helper, player.getMainHandItem().getCount(), 1, "remaining glass bottle count");
        assertValueEqual(helper, brewer.getTankAmountMb(), 250, "remaining potion amount");
        var droppedPotions = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(player.blockPosition()).inflate(2.0D),
                entity -> ItemStack.isSameItemSameTags(entity.getItem(), potionStack(Potions.SWIFTNESS))
        );
        assertValueEqual(helper, droppedPotions.size(), 1, "dropped potion count");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stackedGlassBottleInsertsPotionIntoInventory(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", 500);
        var player = createPlayer(helper, "alchemy_brewer_stacked_bottle", pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));

        useBrewer(helper, player, pos);

        assertValueEqual(helper, player.getMainHandItem().getCount(), 1, "remaining glass bottle count");
        helper.assertTrue(player.getInventory().contains(potionStack(Potions.SWIFTNESS)),
                "Brewed potion must be inserted into an available inventory slot");
        assertValueEqual(helper, brewer.getTankAmountMb(), 250, "remaining potion amount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void insufficientOrInvalidTankFallsBackToMenu(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var player = createPlayer(helper, "alchemy_brewer_invalid_tank", pos);
        for (int amountMb : new int[]{0, 1, 249}) {
            var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", amountMb);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));

            useBrewer(helper, player, pos);

            helper.assertTrue(player.wasMenuOpened(),
                    "A tank below one dose must open the Alchemy Brewer menu: " + amountMb);
            helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE),
                    "A tank below one dose must not consume the glass bottle: " + amountMb);
            assertValueEqual(helper, brewer.getTankAmountMb(), amountMb, "unchanged sub-dose tank amount");
            player.resetMenuTracking();
        }

        var brewer = placeAlchemyBrewer(helper, pos, "apprenticecodex:missing_potion", 250);
        helper.assertTrue(AlchemyBrewerBlockEntity.resolveRegisteredPotion(brewer.getTankPotionId()) == null,
                "Missing potion ids must not resolve to a default potion");
        useBrewer(helper, player, pos);

        helper.assertTrue(player.wasMenuOpened(),
                "An invalid potion id must open the Alchemy Brewer menu");
        assertValueEqual(helper, brewer.getTankAmountMb(), 250, "unchanged invalid potion amount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unsupportedContainersFallBackToMenu(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", 1000);
        var player = createPlayer(helper, "alchemy_brewer_unsupported_containers", pos);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        useBrewer(helper, player, pos);
        helper.assertTrue(player.wasMenuOpened(),
                "Water bucket must open the Alchemy Brewer menu");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "Water bucket must remain unchanged");
        player.resetMenuTracking();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        useBrewer(helper, player, pos);
        helper.assertTrue(player.wasMenuOpened(), "Empty bucket must open the Alchemy Brewer menu");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Empty bucket must remain unchanged");
        player.resetMenuTracking();

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void waterSupplyUsesDistanceThenYzxAndOnlyChangesOneTarget(GameTestHelper helper) {
        var brewerPos = new BlockPos(2, 1, 2);
        var lowerXCauldron = new BlockPos(1, 1, 2);
        var higherXCauldron = new BlockPos(3, 1, 2);
        helper.setBlock(brewerPos, BlockRegistry.ALCHEMY_BREWER.get());
        helper.setBlock(lowerXCauldron, Blocks.CAULDRON);
        helper.setBlock(higherXCauldron, Blocks.CAULDRON);

        helper.succeedWhen(() -> {
            var selected = helper.getBlockState(lowerXCauldron);
            helper.assertTrue(selected.is(Blocks.WATER_CAULDRON),
                    "The lower X cauldron must win the final tie-breaker");
            assertValueEqual(helper, selected.getValue(LayeredCauldronBlock.LEVEL), 1,
                    "selected vanilla cauldron water level");
            helper.assertTrue(helper.getBlockState(higherXCauldron).is(Blocks.CAULDRON),
                    "Only one cauldron may change per supply attempt");
        });
    }

    @GameTest(template = TEMPLATE, batch = VANILLA_INCREMENT_CONFIG_BATCH, timeoutTicks = 60)
    public static void vanillaCauldronSupplyUsesConfiguredIncrementAndDiscardsOverflow(GameTestHelper helper) {
        var override = useWaterConfigUntil(helper, new AlchemyBrewerServerConfig.Values(10, 2, 0), 55);
        var brewerPos = new BlockPos(2, 1, 2);
        var cauldronPos = new BlockPos(3, 1, 2);
        helper.setBlock(brewerPos, BlockRegistry.ALCHEMY_BREWER.get());
        helper.setBlock(cauldronPos, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 2));

        helper.succeedWhen(() -> {
            var state = helper.getBlockState(cauldronPos);
            assertValueEqual(helper, state.getValue(LayeredCauldronBlock.LEVEL), 3,
                    "configured vanilla supply must cap at a full cauldron");
            override.close();
        });
    }

    @GameTest(template = TEMPLATE, batch = IRON_CAULDRON_CONFIG_BATCH, timeoutTicks = 60)
    public static void alchemistCauldronSupplyFillsRemainingCapacityOnly(GameTestHelper helper) {
        var override = useWaterConfigUntil(helper, new AlchemyBrewerServerConfig.Values(10, 0, 250), 55);
        var brewerPos = new BlockPos(2, 1, 2);
        var cauldronPos = new BlockPos(3, 1, 2);
        helper.setBlock(brewerPos, BlockRegistry.ALCHEMY_BREWER.get());
        helper.setBlock(cauldronPos, io.redspace.ironsspellbooks.registries.BlockRegistry.ALCHEMIST_CAULDRON.get());
        var cauldron = (AlchemistCauldronTile) helper.getBlockEntity(cauldronPos);
        cauldron.fluidInventory.fill(
                new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 900),
                IFluidHandler.FluidAction.EXECUTE
        );

        helper.succeedWhen(() -> {
            assertValueEqual(helper, cauldron.getFluidAmount(), 1000,
                    "Alchemist Cauldron must accept only its remaining capacity");
            override.close();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void alchemistCauldronContainingPotionIsSkipped(GameTestHelper helper) {
        var brewerPos = new BlockPos(2, 1, 2);
        var potionCauldronPos = new BlockPos(1, 1, 2);
        var vanillaCauldronPos = new BlockPos(3, 1, 2);
        helper.setBlock(brewerPos, BlockRegistry.ALCHEMY_BREWER.get());
        helper.setBlock(potionCauldronPos,
                io.redspace.ironsspellbooks.registries.BlockRegistry.ALCHEMIST_CAULDRON.get());
        helper.setBlock(vanillaCauldronPos, Blocks.CAULDRON);
        var potionCauldron = (AlchemistCauldronTile) helper.getBlockEntity(potionCauldronPos);
        var potionFluid = PotionFluid.from(potionStack(Potions.SWIFTNESS));
        potionFluid.setAmount(250);
        potionCauldron.fluidInventory.fill(potionFluid, IFluidHandler.FluidAction.EXECUTE);

        helper.succeedWhen(() -> {
            assertValueEqual(helper, potionCauldron.getFluidAmount(), 250,
                    "A potion-containing Alchemist Cauldron must not receive water");
            helper.assertTrue(helper.getBlockState(vanillaCauldronPos).is(Blocks.WATER_CAULDRON),
                    "The next eligible cauldron must receive water");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void redstoneSignalStopsWaterSupplyUntilRemoved(GameTestHelper helper) {
        var brewerPos = new BlockPos(2, 1, 2);
        var cauldronPos = new BlockPos(3, 1, 2);
        var powerPos = new BlockPos(2, 1, 1);
        helper.setBlock(brewerPos, BlockRegistry.ALCHEMY_BREWER.get());
        helper.setBlock(cauldronPos, Blocks.CAULDRON);
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(25, () -> {
            helper.assertTrue(helper.getBlockState(cauldronPos).is(Blocks.CAULDRON),
                    "A powered Alchemy Brewer must not supply water");
            helper.setBlock(powerPos, Blocks.AIR);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 25, "Water supply must remain blocked before redstone is removed");
            helper.assertTrue(helper.getBlockState(cauldronPos).is(Blocks.WATER_CAULDRON),
                    "Water supply must resume after redstone is removed");
        });
    }

    @GameTest(template = TEMPLATE, batch = WATER_DISABLED_CONFIG_BATCH, timeoutTicks = 50)
    public static void zeroSupplyAmountsDisableWaterSupply(GameTestHelper helper) {
        var override = useWaterConfigUntil(helper, new AlchemyBrewerServerConfig.Values(10, 0, 0), 45);
        var brewerPos = new BlockPos(2, 1, 2);
        var cauldronPos = new BlockPos(3, 1, 2);
        helper.setBlock(brewerPos, BlockRegistry.ALCHEMY_BREWER.get());
        helper.setBlock(cauldronPos, Blocks.CAULDRON);

        helper.runAfterDelay(25, () -> {
            try {
                helper.assertTrue(helper.getBlockState(cauldronPos).is(Blocks.CAULDRON),
                        "Zero supply amounts must disable water supply");
                helper.succeed();
            } finally {
                override.close();
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskCollectsAsManyDosesAsPossible(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", AlchemyBrewerBlockEntity.TANK_CAPACITY_MB);
        var player = createPlayer(helper, "alchemy_brewer_empty_spellcasters_flask", pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()));

        useBrewer(helper, player, pos);

        assertValueEqual(helper, AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()), 4,
                "stored dose count");
        helper.assertTrue(ItemStack.isSameItemSameTags(
                        AbstractPotionFlaskItem.getStoredItem(player.getMainHandItem()),
                        potionStack(Potions.SWIFTNESS)),
                "stored potion");
        assertValueEqual(helper, brewer.getTankAmountMb(), 0, "remaining potion amount");
        helper.assertFalse(player.isUsingItem(), "Alchemy Brewer interaction must not start drinking the flask");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void alchemistsFlaskCollectsRegularPotionWithMismatchPenalty(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", AlchemyBrewerBlockEntity.TANK_CAPACITY_MB);
        var player = createPlayer(helper, "alchemy_brewer_empty_alchemists_flask", pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()));

        useBrewer(helper, player, pos);

        assertValueEqual(helper, AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()), 4,
                "stored Alchemist's Flask dose count");
        helper.assertTrue(ItemStack.isSameItemSameTags(
                        AbstractPotionFlaskItem.getStoredItem(player.getMainHandItem()),
                        potionStack(Potions.SWIFTNESS)),
                "stored Alchemist's Flask potion");
        helper.assertTrue(AbstractPotionFlaskItem.isStoredVanillaPotionTypeMismatched(player.getMainHandItem()),
                "Regular potion in an Alchemist's Flask must retain the mismatch penalty");
        assertValueEqual(helper, AbstractPotionFlaskItem.getStoredDoseConsumptionCount(player.getMainHandItem()), 2,
                "mismatched Alchemist's Flask dose consumption");
        assertValueEqual(helper, brewer.getTankAmountMb(), 0, "remaining potion amount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void alchemistsFlaskAutomaticFillMovesFullFlaskToOutput(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", AlchemyBrewerBlockEntity.DOSE_AMOUNT_MB);
        var nearlyFull = AbstractPotionFlaskItem.copyWithAddedDoses(
                new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                potionStack(Potions.SWIFTNESS),
                AbstractPotionFlaskItem.getMaxDoseCapacity(new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get())) - 1
        );
        brewer.getInventory().setStackInSlot(AlchemyBrewerBlockEntity.INPUT_SLOT, nearlyFull);

        helper.succeedWhen(() -> {
            var output = brewer.getInventory().getStackInSlot(AlchemyBrewerBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(brewer.getInventory().getStackInSlot(AlchemyBrewerBlockEntity.INPUT_SLOT).isEmpty(),
                    "Filled Alchemist's Flask must leave the input slot");
            assertValueEqual(helper, AbstractPotionFlaskItem.getStoredDoseCount(output),
                    AbstractPotionFlaskItem.getMaxDoseCapacity(output), "automatic Alchemist's Flask dose count");
            helper.assertTrue(AbstractPotionFlaskItem.isStoredVanillaPotionTypeMismatched(output),
                    "Automatically filled Alchemist's Flask must retain the mismatch penalty");
            assertValueEqual(helper, brewer.getTankAmountMb(), 0, "automatic fill remaining potion amount");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskRespectsCapacityAndPotionMatch(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var brewer = placeAlchemyBrewer(helper, pos, "minecraft:swiftness", 1000);
        var player = createPlayer(helper, "alchemy_brewer_partial_spellcasters_flask", pos);
        var nearlyFull = AbstractPotionFlaskItem.copyWithAddedDoses(
                new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                potionStack(Potions.SWIFTNESS),
                AbstractPotionFlaskItem.getMaxDoseCapacity(new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get())) - 1
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, nearlyFull);

        useBrewer(helper, player, pos);

        assertValueEqual(helper,
                AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()),
                AbstractPotionFlaskItem.getMaxDoseCapacity(player.getMainHandItem()),
                "filled dose count"
        );
        assertValueEqual(helper, brewer.getTankAmountMb(), 750, "capacity-limited remaining potion amount");

        useBrewer(helper, player, pos);
        helper.assertTrue(player.wasMenuOpened(),
                "A full Spellcaster's Flask must open the Alchemy Brewer menu");
        assertValueEqual(helper, brewer.getTankAmountMb(), 750, "unchanged full-flask potion amount");
        player.resetMenuTracking();

        var mismatched = AbstractPotionFlaskItem.copyWithAddedDoses(
                new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                potionStack(Potions.REGENERATION),
                1
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, mismatched);
        useBrewer(helper, player, pos);

        helper.assertTrue(player.wasMenuOpened(),
                "A mismatched Spellcaster's Flask must open the Alchemy Brewer menu");
        assertValueEqual(helper, AbstractPotionFlaskItem.getStoredDoseCount(player.getMainHandItem()), 1,
                "unchanged mismatched dose count");
        assertValueEqual(helper, brewer.getTankAmountMb(), 750, "unchanged mismatched potion amount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void spellcastersFlaskKeepsPriorityOnOtherBlocks(GameTestHelper helper) {
        var brewerPos = new BlockPos(1, 1, 1);
        var stonePos = new BlockPos(2, 1, 1);
        placeAlchemyBrewer(helper, brewerPos, "minecraft:swiftness", 250);
        helper.setBlock(stonePos, net.minecraft.world.level.block.Blocks.STONE);
        var player = createPlayer(helper, "alchemy_brewer_flask_priority", stonePos);
        var flask = AbstractPotionFlaskItem.copyWithAddedDoses(
                new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                potionStack(Potions.SWIFTNESS),
                1
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, flask);

        var result = player.gameMode.useItemOn(
                player,
                helper.getLevel(),
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                hitResult(helper, stonePos)
        );

        helper.assertTrue(result.consumesAction(), "Filled Spellcaster's Flask must keep priority on ordinary blocks");
        helper.assertTrue(player.isUsingItem(), "Ordinary block interaction must start drinking the flask");
        helper.succeed();
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
        storedFluid.put("Item", potionStack(storedPotion).save(new CompoundTag()));
        storedFluid.putInt("Amount", storedAmountMb);
        var storedFluids = new ListTag();
        storedFluids.add(storedFluid);
        var tag = new CompoundTag();
        tag.put("StoredFluids", storedFluids);
        station.load(tag);
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
        brewer.load(tag);
        return brewer;
    }

    private static ApprenticeCodexServerConfig.GameTestConfigOverride useWaterConfigUntil(
            GameTestHelper helper,
            AlchemyBrewerServerConfig.Values values,
            int restoreTick
    ) {
        var override = ApprenticeCodexServerConfig.useAlchemyBrewerConfigOverrideForGameTest(values);
        helper.runAtTickTime(restoreTick, override::close);
        return override;
    }

    private static MenuTrackingFakePlayer createPlayer(GameTestHelper helper, String name, BlockPos localPos) {
        var player = new MenuTrackingFakePlayer(helper, name);
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var position = helper.absoluteVec(Vec3.atCenterOf(localPos));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static net.minecraft.world.InteractionResult useBrewer(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos localPos
    ) {
        return player.gameMode.useItemOn(
                player,
                helper.getLevel(),
                player.getMainHandItem(),
                InteractionHand.MAIN_HAND,
                hitResult(helper, localPos)
        );
    }

    private static BlockHitResult hitResult(GameTestHelper helper, BlockPos localPos) {
        var absolutePos = helper.absolutePos(localPos);
        return new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
    }

    private static final class MenuTrackingFakePlayer extends FakePlayer {
        private MenuTrackingFakePlayer(GameTestHelper helper, String name) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        }

        private boolean wasMenuOpened() {
            // Forge 1.20.1 の NetworkHooks.openScreen は ServerPlayer#openMenu を経由しないため、
            // 実際に切り替わったcontainerでメニューopenを観測する。
            return containerMenu != inventoryMenu;
        }

        private void resetMenuTracking() {
            closeContainer();
        }
    }

    private static ItemStack potionStack(Potion potion) {
        return PotionContentsHelper.createPotionStack(Items.POTION, potion);
    }

    private static void assertValueEqual(GameTestHelper helper, Object actual, Object expected, String message) {
        helper.assertTrue(java.util.Objects.equals(actual, expected),
                message + ": expected=" + expected + ", actual=" + actual);
    }
}
