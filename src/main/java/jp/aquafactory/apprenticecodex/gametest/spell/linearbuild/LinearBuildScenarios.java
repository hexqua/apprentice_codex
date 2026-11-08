package jp.aquafactory.apprenticecodex.gametest.spell.linearbuild;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.List;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.spell.LinearBuildServerConfig;
import jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumPouchGameTestHelper;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.network.packet.SyncLinearBuildConfigPacket;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuild;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildConfigState;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.CREATE_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.invokeCreateGameTestHookItemStack;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.invokeCreateGameTestHookVoid;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

public final class LinearBuildScenarios {
    private LinearBuildScenarios() {}

    static void linearBuildPlacesUntilPlayerAxis(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_axis_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(1, 3, 2));
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Linear Build should consume exactly the blocks it placed from main hand");
        });
    }

    static void linearBuildUsesBaseAndCraftsmansDelightRanges(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = (LinearBuild) SpellRegistry.LINEAR_BUILD.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_range_test");

            var configBuffer = new FriendlyByteBuf(Unpooled.buffer());
            SyncLinearBuildConfigPacket.encode(new SyncLinearBuildConfigPacket(17), configBuffer);
            var decodedConfig = SyncLinearBuildConfigPacket.decode(configBuffer);
            helper.assertTrue(decodedConfig.manaCostPerBlock() == 17,
                    "Linear Build config sync should preserve the per-block mana cost");

            try {
                LinearBuildConfigState.set(decodedConfig.manaCostPerBlock());
                var uniqueInfo = spell.getUniqueInfo(1, null);
                helper.assertTrue(uniqueInfo.size() == 2,
                        "Linear Build unique info should accept a null caster and include the additional placement cost");
                helper.assertTrue(
                        uniqueInfo.get(1).getContents() instanceof TranslatableContents contents
                                && "ui.apprenticecodex.linear_build.additional_place_cost".equals(contents.getKey())
                                && contents.getArgs().length == 1
                                && contents.getArgs()[0] instanceof Number cost
                                && cost.intValue() == 17,
                        "Linear Build unique info should use the synchronized per-block mana cost"
                );
            } finally {
                LinearBuildConfigState.reset();
            }
            helper.assertTrue(spell.getClientBlockTargetingRange(1, player) == 24.0D,
                    "Linear Build should use a 24-block base range");
            helper.assertTrue(CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spell.getSpellId()),
                    "Linear Build should be a Craftsman's Delight mana discount target");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            helper.assertTrue(spell.getClientBlockTargetingRange(1, player) == 32.0D,
                    "Linear Build should use a 32-block range with Craftsman's Delight");
            var equippedInfo = spell.getUniqueInfo(1, player);
            helper.assertTrue(
                    equippedInfo.get(1).getContents() instanceof TranslatableContents contents
                            && contents.getArgs()[0] instanceof Number cost
                            && cost.intValue() == 3,
                    "Linear Build unique info should show the Craftsman's Delight per-block mana discount"
            );
        });
    }

    static void linearBuildConsumesConfiguredManaPerPlacedBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_mana_cost_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 2));
                helper.setBlock(targetPos, Blocks.STONE);
                var magicData = MagicData.getPlayerMagicData(player);
                magicData.setMana(20.0F);

                castLinearBuildWithCurrentMana(helper, player, targetPos, Direction.WEST);

                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
                helper.assertTrue(Math.abs(magicData.getMana() - 10.0F) < 1e-4F,
                        "Linear Build should consume configured mana for each successfully placed block");
            }
        });
    }

    static void linearBuildCraftsmansDelightDiscountsManaPerBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_discount_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 2));
                equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
                helper.setBlock(targetPos, Blocks.STONE);
                var magicData = MagicData.getPlayerMagicData(player);
                magicData.setMana(20.0F);

                castLinearBuildWithCurrentMana(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(Math.abs(magicData.getMana() - 14.0F) < 1e-4F,
                        "Craftsman's Delight should reduce Linear Build's per-block mana cost from 5 to 3");
            }
        });
    }

    static void linearBuildStopsPartwayWhenManaIsDepleted(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(6, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_deplete_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
                helper.setBlock(targetPos, Blocks.STONE);
                helper.setBlock(new BlockPos(5, 3, 2), Blocks.AIR);
                helper.setBlock(new BlockPos(4, 3, 2), Blocks.AIR);
                helper.setBlock(new BlockPos(3, 3, 2), Blocks.AIR);
                var magicData = MagicData.getPlayerMagicData(player);
                magicData.setMana(11.0F);

                castLinearBuildWithCurrentMana(helper, player, targetPos, Direction.WEST);

                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 3, 2));
                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
                helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
                helper.assertTrue(player.getMainHandItem().getCount() == 2,
                        "Linear Build should retain blocks that could not be placed after mana depletion");
                helper.assertTrue(Math.abs(magicData.getMana() - 1.0F) < 1e-4F,
                        "Linear Build should retain mana that is insufficient for the next placement");
            }
        });
    }

    static void linearBuildZeroManaCostConfigAllowsPlacement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(0, false, true, true)
            )) {
                var targetPos = new BlockPos(4, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_zero_mana_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
                helper.setBlock(targetPos, Blocks.STONE);
                var magicData = MagicData.getPlayerMagicData(player);
                magicData.setMana(0.0F);

                castLinearBuildWithCurrentMana(helper, player, targetPos, Direction.WEST);

                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
                helper.assertTrue(magicData.getMana() == 0.0F,
                        "Linear Build should not consume mana when its per-block cost is configured to zero");
            }
        });
    }

    static void linearBuildScrollCastDoesNotConsumeAdditionalMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_scroll_mana_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 2));
                helper.setBlock(targetPos, Blocks.STONE);
                var magicData = MagicData.getPlayerMagicData(player);
                magicData.setMana(0.0F);

                castLinearBuildWithCurrentMana(helper, player, targetPos, Direction.WEST, CastSource.SCROLL);

                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
                helper.assertTrue(magicData.getMana() == 0.0F,
                        "Scroll-cast Linear Build should not consume additional mana per placed block");
            }
        });
    }

    static void linearBuildTriesOneBlockWhenFirstPlacementTouchesPlayerAxis(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(4, 2, 2);
            var placePos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_initial_axis_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.UP);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, placePos);
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Linear Build should try and consume one block even when the first placement touches the player Y axis");
        });
    }

    static void linearBuildUpwardFromPlayerYBlockPlacesOnlyOne(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(4, 3, 2);
            var firstPlacePos = new BlockPos(4, 4, 2);
            var secondPlacePos = new BlockPos(4, 5, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_upward_same_y_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
            helper.setBlock(targetPos, Blocks.STONE);
            helper.setBlock(firstPlacePos, Blocks.AIR);
            helper.setBlock(secondPlacePos, Blocks.AIR);

            castLinearBuild(helper, player, targetPos, Direction.UP);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, firstPlacePos);
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, secondPlacePos);
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                    "Linear Build should place only one block when extending upward from a block on the player's Y axis");
        });
    }

    static void linearBuildPrefersOffhandBlockTemplate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_offhand_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 3));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
            helper.assertTrue(player.getMainHandItem().is(Items.DIRT) && player.getMainHandItem().getCount() == 4,
                    "Linear Build should not use the main hand block when the offhand holds a block");
            helper.assertTrue(player.getOffhandItem().is(Items.OAK_PLANKS) && player.getOffhandItem().getCount() == 1,
                    "Linear Build should consume the selected offhand block template");
        });
    }

    static void linearBuildUsesOffhandLuminousDeviceBeforeShulkerSource(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_luminous_device_source_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 2));
            player.setItemInHand(InteractionHand.OFF_HAND, deviceStack);
            player.getInventory().setItem(10, createShulkerWithItem(new ItemStack(Items.GLOWSTONE, 2)));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(3, 3, 2));
            helper.assertTrue(LuminousDevice.getStoredCount(deviceStack, new ItemStack(Items.GLOWSTONE)) == 0,
                    "Linear Build should consume matching Luminous Device contents before Shulker contents");
            helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.GLOWSTONE),
                    "Linear Build consumption should preserve the zero-count selected template");
            var shulkerStack = getNestedContainerItem(player.getInventory().getItem(10), 0);
            helper.assertTrue(shulkerStack.is(Items.GLOWSTONE) && shulkerStack.getCount() == 2,
                    "Linear Build should leave lower-priority Shulker contents untouched");
            helper.assertTrue(player.getMainHandItem().is(Items.DIRT) && player.getMainHandItem().getCount() == 4,
                    "An offhand Luminous Device selection should take priority over the main-hand block");
        });
    }

    static void linearBuildConsumesSoulwovenPouchContents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_soulwoven_pouch_source_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            var pouchStack = MalumPouchGameTestHelper.createPouch("soulwoven_pouch", List.of(
                    new ItemStack(Items.OAK_PLANKS, 2),
                    new ItemStack(Items.DIRT, 3)
            ));
            player.getInventory().setItem(10, pouchStack);
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertTrue(MalumPouchGameTestHelper.getStoredCount(
                            pouchStack, new ItemStack(Items.OAK_PLANKS)) == 0,
                    "Linear Build should consume all matching Soulwoven Pouch contents");
            helper.assertTrue(MalumPouchGameTestHelper.getStoredCount(
                            pouchStack, new ItemStack(Items.DIRT)) == 3,
                    "Linear Build should preserve non-matching Soulwoven Pouch contents");
            helper.assertTrue(MalumPouchGameTestHelper.getWeight(pouchStack) > 0.0D,
                    "Soulwoven Pouch weight should be recalculated from remaining contents");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS)
                            && player.getMainHandItem().getCount() == 1,
                    "Linear Build should use Soulwoven Pouch contents before the held stack");
        });
    }

    static void linearBuildConsumesLastRavenousPouchItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var targetPos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_ravenous_pouch_source_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            var pouchStack = MalumPouchGameTestHelper.createPouch(
                    "ravenous_pouch",
                    List.of(new ItemStack(Items.OAK_PLANKS))
            );
            player.getInventory().setItem(10, pouchStack);
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertTrue(MalumPouchGameTestHelper.getItemsCopy(pouchStack).isEmpty(),
                    "Linear Build should consume the final Ravenous Pouch item");
        });
    }

    static void linearBuildUsesMalumPouchBeforeLuminousDevice(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_malum_pouch_priority_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 2));
            player.setItemInHand(InteractionHand.OFF_HAND, deviceStack);
            var pouchStack = MalumPouchGameTestHelper.createPouch(
                    "soulwoven_pouch",
                    List.of(new ItemStack(Items.GLOWSTONE))
            );
            player.getInventory().setItem(10, pouchStack);
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertTrue(MalumPouchGameTestHelper.getItemsCopy(pouchStack).isEmpty(),
                    "Linear Build should consume matching Malum Pouch contents before Luminous Device contents");
            helper.assertTrue(LuminousDevice.getStoredCount(deviceStack, new ItemStack(Items.GLOWSTONE)) == 1,
                    "Linear Build should use Luminous Device only after the Malum Pouch is exhausted");
        });
    }

    static void linearBuildMalumPouchRequiresMatchingComponents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var targetPos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_malum_pouch_components_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE));
            player.setItemInHand(InteractionHand.OFF_HAND, deviceStack);
            var namedGlowstone = new ItemStack(Items.GLOWSTONE);
            namedGlowstone.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct Glowstone"));
            var pouchStack = MalumPouchGameTestHelper.createPouch("ravenous_pouch", List.of(namedGlowstone));
            player.getInventory().setItem(10, pouchStack);
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertTrue(MalumPouchGameTestHelper.getStoredCount(pouchStack, namedGlowstone) == 1,
                    "Linear Build should preserve Malum Pouch contents with different components");
            helper.assertTrue(LuminousDevice.getStoredCount(deviceStack, new ItemStack(Items.GLOWSTONE)) == 0,
                    "Linear Build should fall through to an exactly matching lower-priority source");
        });
    }

    static void linearBuildResolvesZeroCountLuminousDeviceSelectionAndEmptyFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var zeroTargetPos = new BlockPos(5, 3, 2);
            var zeroSelectionPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2),
                    "linear_build_zero_luminous_selection_test");
            zeroSelectionPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
            var zeroSelectionDevice = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(zeroSelectionDevice, new ItemStack(Items.GLOWSTONE));
            LuminousDevice.consumeOneStored(zeroSelectionDevice, new ItemStack(Items.GLOWSTONE));
            zeroSelectionPlayer.setItemInHand(InteractionHand.OFF_HAND, zeroSelectionDevice);
            zeroSelectionPlayer.getInventory().setItem(10, new ItemStack(Items.GLOWSTONE, 2));
            helper.setBlock(zeroTargetPos, Blocks.STONE);

            castLinearBuild(helper, zeroSelectionPlayer, zeroTargetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(3, 3, 2));
            helper.assertTrue(zeroSelectionPlayer.getMainHandItem().is(Items.DIRT),
                    "A zero-count selected item should still be resolved instead of falling back to main hand");
            helper.assertTrue(zeroSelectionPlayer.getInventory().getItem(10).isEmpty(),
                    "A zero-count selection should use matching blocks from another source");

            var fallbackTargetPos = new BlockPos(5, 3, 4);
            var fallbackPlayer = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 4),
                    "linear_build_empty_luminous_fallback_test");
            fallbackPlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE, 2));
            fallbackPlayer.setItemInHand(
                    InteractionHand.OFF_HAND,
                    new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get())
            );
            helper.setBlock(fallbackTargetPos, Blocks.STONE);

            castLinearBuild(helper, fallbackPlayer, fallbackTargetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(4, 3, 4));
            helper.assertBlockPresent(Blocks.GLOWSTONE, new BlockPos(3, 3, 4));
            helper.assertTrue(fallbackPlayer.getMainHandItem().isEmpty(),
                    "An empty offhand Luminous Device should fall back to the main-hand block");
        });
    }

    static void linearBuildConsumesReplaceablePlacedBlockTemplate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var placePos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(3, 3, 2), "linear_build_replaceable_block_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FERN));
            helper.setBlock(targetPos, Blocks.STONE);
            helper.setBlock(placePos.below(), Blocks.GRASS_BLOCK);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.FERN, placePos);
            helper.assertTrue(player.getMainHandItem().isEmpty(),
                    "Linear Build should consume replaceable block templates that stay replaceable after placement");
        });
    }

    static void linearBuildRejectsLargeAndDenylistedTemplates(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertLinearBuildRejectsTemplate(
                    helper,
                    new ItemStack(Items.OAK_DOOR),
                    "linear_build_oak_door_reject_test",
                    "Linear Build should reject door templates"
            );
            assertLinearBuildRejectsTemplate(
                    helper,
                    new ItemStack(Items.WHITE_BED),
                    "linear_build_white_bed_reject_test",
                    "Linear Build should reject bed templates"
            );

            var inscriptionTable = BuiltInRegistries.BLOCK.get(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "inscription_table")
            );
            helper.assertTrue(inscriptionTable != null, "irons_spellbooks:inscription_table is not registered");
            assertLinearBuildRejectsTemplate(
                    helper,
                    new ItemStack(inscriptionTable.asItem()),
                    "linear_build_inscription_table_reject_test",
                    "Linear Build should reject denylisted Inscription Table templates"
            );
        });
    }

    static void linearBuildRejectsOffhandTemplateWithoutMainHandFallback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var placePos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_offhand_reject_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_DOOR));
            helper.setBlock(targetPos, Blocks.STONE);

            assertLinearBuildPreCastRejected(helper, player, targetPos, Direction.WEST,
                    "Linear Build should reject the offhand template before checking main hand fallback");

            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, placePos);
            helper.assertBlockNotPresent(Blocks.OAK_DOOR, placePos);
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should not consume the main hand fallback block");
            helper.assertTrue(player.getOffhandItem().is(Items.OAK_DOOR) && player.getOffhandItem().getCount() == 1,
                    "Linear Build should not consume the rejected offhand template");
        });
    }

    static void linearBuildSkipsBlockedPositionsByDefault(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(6, 3, 2);
            var blockedPos = new BlockPos(4, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_skip_blocked_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
            helper.setBlock(targetPos, Blocks.STONE);
            helper.setBlock(new BlockPos(5, 3, 2), Blocks.AIR);
            helper.setBlock(blockedPos, Blocks.COBBLESTONE);
            helper.setBlock(new BlockPos(3, 3, 2), Blocks.AIR);
            helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 3, 2));
            helper.assertBlockPresent(Blocks.COBBLESTONE, blockedPos);
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
            helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(1, 3, 2));
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should skip blocked positions without consuming a block for them");
        });
    }

    static void linearBuildAbortOnFailedPlacementConfigStopsAtBlockedPosition(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, true, true, true)
            )) {
                var targetPos = new BlockPos(6, 3, 2);
                var blockedPos = new BlockPos(4, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(1, 3, 2), "linear_build_abort_blocked_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 4));
                helper.setBlock(targetPos, Blocks.STONE);
                helper.setBlock(new BlockPos(5, 3, 2), Blocks.AIR);
                helper.setBlock(blockedPos, Blocks.COBBLESTONE);
                helper.setBlock(new BlockPos(3, 3, 2), Blocks.AIR);
                helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 3, 2));
                helper.assertBlockPresent(Blocks.COBBLESTONE, blockedPos);
                helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
                helper.assertBlockNotPresent(Blocks.OAK_PLANKS, new BlockPos(2, 3, 2));
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                        "Linear Build should keep the legacy stop-on-failure behavior when configured");
            }
        });
    }

    static void linearBuildCopiesTopSlabAndConsumesCompanionTrunkFirst(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_trunk_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_SLAB));
            var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
            helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
            trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_SLAB, 2));
            helper.setBlock(targetPos, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_SLAB, new BlockPos(4, 3, 2));
            helper.assertBlockProperty(new BlockPos(4, 3, 2), SlabBlock.TYPE, SlabType.TOP);
            helper.assertBlockPresent(Blocks.OAK_SLAB, new BlockPos(3, 3, 2));
            helper.assertBlockProperty(new BlockPos(3, 3, 2), SlabBlock.TYPE, SlabType.TOP);
            helper.assertTrue(trunkInventory.getHandler().getStackInSlot(0).isEmpty(),
                    "Linear Build should consume matching blocks from Companion Trunk before the held stack");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_SLAB) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave the held stack untouched while Companion Trunk has enough blocks");
        });
    }

    static void linearBuildCopiesBottomSlabType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_SLAB),
                    Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM),
                    "linear_build_bottom_slab_test"
            );

            helper.assertBlockPresent(Blocks.OAK_SLAB, placePos);
            helper.assertBlockProperty(placePos, SlabBlock.TYPE, SlabType.BOTTOM);
        });
    }

    static void linearBuildDoesNotCopyDoubleSlabType(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_SLAB),
                    Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                    "linear_build_double_slab_test"
            );

            helper.assertBlockPresent(Blocks.OAK_SLAB, placePos);
            helper.assertTrue(helper.getBlockState(placePos).getValue(SlabBlock.TYPE) != SlabType.DOUBLE,
                    "Linear Build should not create a double slab from one slab item");
        });
    }

    static void linearBuildCopiesFurnaceFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.FURNACE),
                    Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.EAST),
                    "linear_build_furnace_facing_test"
            );

            helper.assertBlockPresent(Blocks.FURNACE, placePos);
            helper.assertBlockProperty(placePos, AbstractFurnaceBlock.FACING, Direction.EAST);
        });
    }

    static void linearBuildCopiesLogAxis(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_LOG),
                    Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
                    "linear_build_log_axis_test"
            );

            helper.assertBlockPresent(Blocks.OAK_LOG, placePos);
            helper.assertBlockProperty(placePos, RotatedPillarBlock.AXIS, Direction.Axis.X);
        });
    }

    static void linearBuildCopiesPistonFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.PISTON),
                    Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.UP),
                    "linear_build_piston_facing_test"
            );

            helper.assertBlockPresent(Blocks.PISTON, placePos);
            helper.assertBlockProperty(placePos, DirectionalBlock.FACING, Direction.UP);
        });
    }

    static void linearBuildCopiesSpellDispenserFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(ItemRegistry.SPELL_DISPENSER.get()),
                    BlockRegistry.SPELL_DISPENSER.get().defaultBlockState().setValue(SpellDispenser.FACING, Direction.EAST),
                    "linear_build_spell_dispenser_facing_test"
            );

            helper.assertBlockPresent(BlockRegistry.SPELL_DISPENSER.get(), placePos);
            helper.assertBlockProperty(placePos, SpellDispenser.FACING, Direction.EAST);
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(placePos)) != null,
                    "Linear Build should keep Spell Dispenser block entity initialization");
        });
    }

    static void linearBuildKeepsShulkerBlockEntityTagContents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    createShulkerWithItem(new ItemStack(Items.DIAMOND, 3)),
                    Blocks.SHULKER_BOX.defaultBlockState(),
                    "linear_build_shulker_contents_test"
            );

            helper.assertBlockPresent(Blocks.SHULKER_BOX, placePos);
            var blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(placePos));
            helper.assertTrue(blockEntity instanceof ShulkerBoxBlockEntity,
                    "Linear Build should place a Shulker Box block entity");
            var storedStack = ((ShulkerBoxBlockEntity) blockEntity).getItem(0);
            helper.assertTrue(storedStack.is(Items.DIAMOND) && storedStack.getCount() == 3,
                    "Linear Build should preserve Shulker Box BlockEntityTag contents");
        });
    }

    static void linearBuildCopiesStairFacingAndHalfOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var placePos = placeSingleLinearBuildBlock(
                    helper,
                    new ItemStack(Items.OAK_STAIRS),
                    Blocks.OAK_STAIRS.defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.SOUTH)
                            .setValue(StairBlock.HALF, Half.TOP)
                            .setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT),
                    "linear_build_stair_shape_test"
            );

            helper.assertBlockPresent(Blocks.OAK_STAIRS, placePos);
            helper.assertBlockProperty(placePos, StairBlock.FACING, Direction.SOUTH);
            helper.assertBlockProperty(placePos, StairBlock.HALF, Half.TOP);
            helper.assertTrue(helper.getBlockState(placePos).getValue(StairBlock.SHAPE) != StairsShape.INNER_LEFT,
                    "Linear Build should not copy stair connection shape");
        });
    }

    static void linearBuildRecalculatesLastStairShape(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(2, 3, 5);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 1), "linear_build_last_stair_shape_test");
            player.setYRot(0.0F);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_STAIRS, 3));
            helper.setBlock(targetPos, Blocks.OAK_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.EAST));

            castLinearBuild(helper, player, targetPos, Direction.NORTH);

            // 設置時は南向きで角になりうるが、東向きのコピー後は最終位置も直線になる。
            for (var z = 2; z <= 4; ++z) {
                var placePos = new BlockPos(2, 3, z);
                helper.assertBlockPresent(Blocks.OAK_STAIRS, placePos);
                helper.assertBlockProperty(placePos, StairBlock.FACING, Direction.EAST);
                helper.assertBlockProperty(placePos, StairBlock.SHAPE, StairsShape.STRAIGHT);
            }
        });
    }

    static void linearBuildCreativeCopiesHeldBlockWithoutConsumingStorage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_creative_test");
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 1));
            var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
            helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
            trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
            helper.setBlock(targetPos, Blocks.STONE);
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(0.0F);

            castLinearBuildWithCurrentMana(helper, player, targetPos, Direction.WEST);

            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 3, 2));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3, 3, 2));
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should not consume the held block in creative mode");
            var trunkStack = trunkInventory.getHandler().getStackInSlot(0);
            helper.assertTrue(trunkStack.is(Items.OAK_PLANKS) && trunkStack.getCount() == 2,
                    "Linear Build should not retrieve blocks from storage in creative mode");
            helper.assertTrue(magicData.getMana() == 0.0F,
                    "Linear Build should not consume additional mana in creative mode");
        });
    }

    static void linearBuildConsumesPersonalShelfWithoutNearbyShelf(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_personal_shelf_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            var personalInventory = Capabilities.getPersonalInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing personal inventory for Linear Build GameTest"));
            personalInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
            helper.setBlock(targetPos, Blocks.STONE);

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertTrue(personalInventory.getHandler().getStackInSlot(0).isEmpty(),
                    "Linear Build should consume Personal Shelf inventory without requiring a nearby shelf block");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Personal Shelf has enough blocks");
        });
    }

    static void linearBuildPrefersEnderChestBeforeCreateToolbox(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        helper.succeedIf(() -> {
            var targetPos = new BlockPos(5, 3, 2);
            var toolboxPos = new BlockPos(0, 2, 0);
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_ender_before_toolbox_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
            player.getEnderChestInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
            helper.setBlock(targetPos, Blocks.STONE);
            invokeCreateGameTestHookVoid(
                    "placeToolboxWithItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                    helper.getLevel(),
                    helper.absolutePos(toolboxPos),
                    new ItemStack(Items.OAK_PLANKS, 2)
            );

            castLinearBuild(helper, player, targetPos, Direction.WEST);

            helper.assertTrue(player.getEnderChestInventory().getItem(0).isEmpty(),
                    "Linear Build should consume Ender Chest blocks before Create Toolbox blocks");
            var toolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, int.class},
                    helper.getLevel(),
                    helper.absolutePos(toolboxPos),
                    0
            );
            helper.assertTrue(toolboxStack.is(Items.OAK_PLANKS) && toolboxStack.getCount() == 2,
                    "Linear Build consumed Create Toolbox before Ender Chest");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Ender Chest has enough blocks");
        });
    }

    static void linearBuildConsumesPlacedCreateToolboxBeforeCompanionTrunk(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(5, 3, 2);
        var toolboxPos = new BlockPos(0, 2, 0);
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_toolbox_before_trunk_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
        helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
        trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.setBlock(targetPos, Blocks.STONE);
        invokeCreateGameTestHookVoid(
                "placeToolboxWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(toolboxPos),
                new ItemStack(Items.OAK_PLANKS, 2)
        );

        helper.runAtTickTime(2, () -> {
            castLinearBuild(helper, player, targetPos, Direction.WEST);

            var toolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, int.class},
                    helper.getLevel(),
                    helper.absolutePos(toolboxPos),
                    0
            );
            helper.assertTrue(toolboxStack.isEmpty(),
                    "Linear Build should consume matching blocks from placed Create Toolbox before Companion Trunk");
            var trunkStack = trunkInventory.getHandler().getStackInSlot(0);
            helper.assertTrue(trunkStack.is(Items.OAK_PLANKS) && trunkStack.getCount() == 2,
                    "Linear Build consumed Companion Trunk before placed Create Toolbox");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Create Toolbox has enough blocks");
            helper.succeed();
        });
    }

    static void linearBuildIgnoresInventoryCreateToolboxAfterPlacedToolboxMisses(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(5, 3, 2);
        var placedToolboxPos = new BlockPos(0, 2, 0);
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_inventory_toolbox_test");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
        var trunkInventory = Capabilities.getCompanionTrunkInventoryOrNull(player);
        helper.assertTrue(trunkInventory != null, "Linear Build test could not resolve Companion Trunk inventory");
        trunkInventory.getHandler().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.setBlock(targetPos, Blocks.STONE);
        invokeCreateGameTestHookVoid(
                "placeToolboxWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(placedToolboxPos),
                new ItemStack(Items.DIRT, 2)
        );
        var inventoryToolbox = invokeCreateGameTestHookItemStack(
                "createToolboxStackWithItem",
                new Class<?>[]{ItemStack.class},
                new ItemStack(Items.OAK_PLANKS, 2)
        );
        player.getInventory().setItem(10, inventoryToolbox);

        helper.runAtTickTime(2, () -> {
            castLinearBuild(helper, player, targetPos, Direction.WEST);

            var placedToolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxItem",
                    new Class<?>[]{ServerLevel.class, BlockPos.class, int.class},
                    helper.getLevel(),
                    helper.absolutePos(placedToolboxPos),
                    0
            );
            helper.assertTrue(placedToolboxStack.is(Items.DIRT) && placedToolboxStack.getCount() == 2,
                    "Linear Build should skip placed Create Toolbox when it lacks the matching block");
            var inventoryToolboxStack = invokeCreateGameTestHookItemStack(
                    "getToolboxStackItem",
                    new Class<?>[]{ItemStack.class, int.class},
                    player.getInventory().getItem(10),
                    0
            );
            helper.assertTrue(inventoryToolboxStack.is(Items.OAK_PLANKS) && inventoryToolboxStack.getCount() == 2,
                    "Linear Build should ignore inventory Create Toolbox stacks");
            var trunkStack = trunkInventory.getHandler().getStackInSlot(0);
            helper.assertTrue(trunkStack.isEmpty(),
                    "Linear Build should fall back to Companion Trunk after placed Create Toolbox misses");
            helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                    "Linear Build should leave held stack untouched while Companion Trunk has enough blocks");
            helper.succeed();
        });
    }

    static void linearBuildShulkerSourceFollowsServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, false, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_shulker_config_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createShulkerWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                        "Linear Build should use held blocks when Shulker sources are disabled");
                var disabledShulkerStack = getNestedContainerItem(player.getInventory().getItem(10), 0);
                helper.assertTrue(disabledShulkerStack.is(Items.OAK_PLANKS) && disabledShulkerStack.getCount() == 2,
                        "Linear Build should not consume Shulker contents while Shulker sources are disabled");
            }

            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 4);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 4), "linear_build_shulker_enabled_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createShulkerWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(getNestedContainerItem(player.getInventory().getItem(10), 0).isEmpty(),
                        "Linear Build should consume Shulker contents while Shulker sources are enabled");
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                        "Linear Build should leave held stack untouched while enabled Shulker has enough blocks");
            }
        });
    }

    static void linearBuildShulkerSourceKeepsSlotAfterPartialConsume(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_shulker_slot_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
                player.getInventory().setItem(10, createShulkerWithItem(10, new ItemStack(Items.OAK_PLANKS, 64)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                var shulker = player.getInventory().getItem(10);
                var movedStack = getShulkerSlotItem(shulker, 0);
                helper.assertTrue(movedStack.isEmpty(),
                        "Linear Build should not move partially consumed Shulker contents to slot 0");
                var remainingStack = getShulkerSlotItem(shulker, 10);
                helper.assertTrue(remainingStack.is(Items.OAK_PLANKS) && remainingStack.getCount() == 62,
                        "Linear Build should keep partially consumed Shulker contents in their original slot");
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                        "Linear Build should leave held stack untouched while Shulker has enough blocks");
            }
        });
    }

    static void linearBuildBundleSourceFollowsServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, false)
            )) {
                var targetPos = new BlockPos(5, 3, 2);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), "linear_build_bundle_config_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createBundleWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 1,
                        "Linear Build should use held blocks when Bundle sources are disabled");
                var disabledBundleStack = getNestedContainerItem(player.getInventory().getItem(10), 0);
                helper.assertTrue(disabledBundleStack.is(Items.OAK_PLANKS) && disabledBundleStack.getCount() == 2,
                        "Linear Build should not consume Bundle contents while Bundle sources are disabled");
            }

            try (var ignored = ApprenticeCodexServerConfig.useLinearBuildConfigOverrideForGameTest(
                    new LinearBuildServerConfig.Values(5, false, true, true)
            )) {
                var targetPos = new BlockPos(5, 3, 4);
                var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 4), "linear_build_bundle_enabled_test");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 3));
                player.getInventory().setItem(10, createBundleWithItem(new ItemStack(Items.OAK_PLANKS, 2)));
                helper.setBlock(targetPos, Blocks.STONE);

                castLinearBuild(helper, player, targetPos, Direction.WEST);

                helper.assertTrue(getNestedContainerItem(player.getInventory().getItem(10), 0).isEmpty(),
                        "Linear Build should consume Bundle contents while Bundle sources are enabled");
                helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS) && player.getMainHandItem().getCount() == 3,
                        "Linear Build should leave held stack untouched while enabled Bundle has enough blocks");
            }
        });
    }

    private static BlockPos placeSingleLinearBuildBlock(
            GameTestHelper helper,
            ItemStack blockStack,
            BlockState targetState,
            String profileName
    ) {
        var targetPos = new BlockPos(5, 3, 2);
        var placePos = new BlockPos(4, 3, 2);
        var player = createEquipmentTestPlayer(helper, new BlockPos(3, 3, 2), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, blockStack);
        helper.setBlock(targetPos, targetState);

        castLinearBuild(helper, player, targetPos, Direction.WEST);

        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "Linear Build should consume exactly one block for the single placement");
        return placePos;
    }

    private static void assertLinearBuildRejectsTemplate(
            GameTestHelper helper,
            ItemStack blockStack,
            String profileName,
            String message
    ) {
        var targetPos = new BlockPos(5, 3, 2);
        var placePos = new BlockPos(4, 3, 2);
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 3, 2), profileName);
        player.setItemInHand(InteractionHand.MAIN_HAND, blockStack.copy());
        helper.setBlock(targetPos, Blocks.STONE);

        assertLinearBuildPreCastRejected(helper, player, targetPos, Direction.WEST, message);

        helper.assertBlockNotPresent(Block.byItem(blockStack.getItem()), placePos);
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(player.getMainHandItem(), blockStack)
                        && player.getMainHandItem().getCount() == blockStack.getCount(),
                message + " without consuming the template"
        );
    }

    private static void assertLinearBuildPreCastRejected(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos targetPos,
            Direction hitFace,
            String message
    ) {
        var spell = (LinearBuild) SpellRegistry.LINEAR_BUILD.get();
        var absoluteTargetPos = helper.absolutePos(targetPos);
        var targetData = new BlockTargetData();
        targetData.setTarget(
                absoluteTargetPos,
                hitFace,
                Vec3.atCenterOf(absoluteTargetPos),
                absoluteTargetPos.relative(hitFace),
                hitFace.getOpposite()
        );
        BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Linear Build rejection test could not resolve player magic data");
        helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData), message);
    }

    private static ItemStack createShulkerWithItem(ItemStack stack) {
        return createShulkerWithItem(0, stack);
    }

    private static ItemStack createShulkerWithItem(int slot, ItemStack stack) {
        var shulker = new ItemStack(Items.SHULKER_BOX);
        var items = NonNullList.withSize(27, ItemStack.EMPTY);
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack.copy());
        }
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return shulker;
    }

    private static ItemStack createBundleWithItem(ItemStack stack) {
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(stack.copy())));
        return bundle;
    }

    private static ItemStack getNestedContainerItem(ItemStack containerStack, int slot) {
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        if (containerStack.is(Items.BUNDLE)) {
            var index = 0;
            for (var stack : containerStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemsCopy()) {
                if (index == slot) {
                    return stack.copy();
                }
                ++index;
            }
            return ItemStack.EMPTY;
        }
        return getShulkerSlotItem(containerStack, slot);
    }

    private static ItemStack getShulkerSlotItem(ItemStack shulker, int slot) {
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        var contents = shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (slot >= contents.getSlots()) {
            return ItemStack.EMPTY;
        }
        return contents.getStackInSlot(slot).copy();
    }

    static void castLinearBuild(GameTestHelper helper, FakePlayer player, BlockPos targetPos, Direction hitFace) {
        MagicData.getPlayerMagicData(player).setMana(100000.0F);
        castLinearBuildWithCurrentMana(helper, player, targetPos, hitFace);
    }

    public static void castLinearBuildWithCurrentMana(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos targetPos,
            Direction hitFace
    ) {
        castLinearBuildWithCurrentMana(helper, player, targetPos, hitFace, CastSource.SPELLBOOK);
    }

    public static void castLinearBuildWithCurrentMana(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos targetPos,
            Direction hitFace,
            CastSource castSource
    ) {
        var spell = (LinearBuild) SpellRegistry.LINEAR_BUILD.get();
        var absoluteTargetPos = helper.absolutePos(targetPos);
        var targetData = new BlockTargetData();
        targetData.setTarget(
                absoluteTargetPos,
                hitFace,
                Vec3.atCenterOf(absoluteTargetPos),
                absoluteTargetPos.relative(hitFace),
                hitFace.getOpposite()
        );
        BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Linear Build test could not resolve player magic data");
        helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                "Linear Build pre-cast check should accept the prepared block target");
        spell.onCast(helper.getLevel(), 1, player, castSource, magicData);
        spell.onServerCastComplete(helper.getLevel(), 1, player, magicData, false);
    }
}
