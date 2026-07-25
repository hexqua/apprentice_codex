package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceConfigState;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevicePickupEvent;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceTooltip;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceUpgrade;
import jp.aquafactory.apprenticecodex.network.packet.ClientConfirmLuminousDeviceSelectionPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncLuminousDeviceConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMageLightConfigPacket;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightCastProfile;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
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

    static void luminousDeviceWorkbenchUpgradesAreIndependentAndPreserveState(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipeId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    jp.aquafactory.apprenticecodex.ApprenticeCodex.MODID,
                    "spellcaster_workbench/luminous_device_mage_light_upgrade"
            );
            var recipe = helper.getLevel().getRecipeManager().byKey(recipeId)
                    .filter(candidate -> candidate.getType() == RecipeRegistry.SPELLCASTER_WORKBENCH_RECIPE_TYPE.get())
                    .map(candidate -> (jp.aquafactory.apprenticecodex.recipe.spellcasterworkbench.SpellcasterWorkbenchRecipe) candidate)
                    .orElseThrow();
            helper.assertTrue(
                    LuminousDevice.hasUpgrade(
                            recipe.getResultTemplates().get(0),
                            LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT
                    ),
                    "JEI-facing recipe output should include the upgrade NBT"
            );

            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 3));
            LuminousDevice.setStoredMana(deviceStack, 777);
            LuminousDevice.addUpgrade(deviceStack, LuminousDeviceUpgrade.CLEAN);

            var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(SpellRegistry.MAGE_LIGHT.get(), 2, scroll);
            var inputs = new SimpleContainer(
                    deviceStack,
                    scroll,
                    new ItemStack(Items.SPYGLASS)
            );
            helper.assertTrue(recipe.matches(inputs, helper.getLevel()),
                    "Mage Light upgrade recipe should accept a scroll above level one");

            var result = recipe.assemble(inputs, helper.getLevel().registryAccess());
            helper.assertTrue(LuminousDevice.hasUpgrade(result, LuminousDeviceUpgrade.CLEAN)
                            && LuminousDevice.hasUpgrade(result, LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT)
                            && !LuminousDevice.hasUpgrade(result, LuminousDeviceUpgrade.MANA_WIZARDLAMP),
                    "Workbench upgrade should add only the requested independent feature");
            helper.assertTrue(LuminousDevice.getStoredItemCount(result) == 3
                            && LuminousDevice.getStoredMana(result) == 777,
                    "Workbench upgrade should preserve storage and mana NBT");

            inputs.setItem(0, result);
            helper.assertFalse(recipe.matches(inputs, helper.getLevel()),
                    "A Luminous Device should reject an upgrade it already contains");
        });
    }

    static void luminousDeviceWorkbenchMenuCraftsEveryUpgrade(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertWorkbenchMenuUpgrade(
                    helper,
                    LuminousDeviceUpgrade.CLEAN,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get()),
                    new ItemStack(Items.BRUSH)
            );

            var mageLightScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(SpellRegistry.MAGE_LIGHT.get(), 1, mageLightScroll);
            assertWorkbenchMenuUpgrade(
                    helper,
                    LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT,
                    mageLightScroll,
                    new ItemStack(Items.SPYGLASS)
            );

            var wizardlampScroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(SpellRegistry.WIZARDLAMP.get(), 1, wizardlampScroll);
            assertWorkbenchMenuUpgrade(
                    helper,
                    LuminousDeviceUpgrade.MANA_WIZARDLAMP,
                    wizardlampScroll,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())
            );
        });
    }

    private static void assertWorkbenchMenuUpgrade(
            GameTestHelper helper,
            LuminousDeviceUpgrade upgrade,
            ItemStack secondIngredient,
            ItemStack thirdIngredient
    ) {
        var player = new FakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "luminous_device_workbench_" + upgrade.name().toLowerCase())
        );
        var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
        LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 3));
        LuminousDevice.setStoredMana(deviceStack, 777);

        var manualMenu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        manualMenu.getSlot(0).set(deviceStack.copy());
        manualMenu.getSlot(1).set(secondIngredient.copy());
        manualMenu.getSlot(2).set(thirdIngredient.copy());
        var manualPreview = manualMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(LuminousDevice.hasUpgrade(manualPreview, upgrade),
                "Workbench should resolve manually placed or JEI-transferred inputs for " + upgrade.id());
        helper.assertTrue(LuminousDevice.getStoredItemCount(manualPreview) == 3
                        && LuminousDevice.getStoredMana(manualPreview) == 777,
                "Manual Workbench preview should preserve populated Luminous Device NBT");

        helper.assertFalse(manualMenu.getSelectableIcons().stream()
                        .anyMatch(icon -> LuminousDevice.hasUpgrade(icon, upgrade)),
                "Luminous Device upgrades should not appear in the mass-production recipe buttons");

        var freshMenu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        freshMenu.getSlot(0).set(new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get()));
        freshMenu.getSlot(1).set(secondIngredient.copy());
        freshMenu.getSlot(2).set(thirdIngredient.copy());
        var preview = freshMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(LuminousDevice.hasUpgrade(preview, upgrade),
                "Workbench should preview a manually supplied fresh Luminous Device for " + upgrade.id());
        var crafted = freshMenu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
        helper.assertTrue(LuminousDevice.hasUpgrade(crafted, upgrade),
                "Workbench should craft the upgraded Luminous Device for " + upgrade.id());
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

            try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(1024, 2000)) {
                var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
                helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 64)) == 64,
                        "Luminous Device should accept the first tagged stack");
                helper.assertTrue(LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                        "The first inserted item should become selected");
                helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.LANTERN, 1000)) == 960,
                        "Luminous Device should only accept items up to its shared capacity");
                helper.assertTrue(LuminousDevice.getStoredItemCount(deviceStack) == 1024,
                        "Luminous Device should cap total storage at its configured capacity");
                helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE, 1)) == 0,
                        "A full Luminous Device should reject additional items");
                helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.DIRT, 1)) == 0,
                        "Luminous Device should reject items outside its storage tag");
            }
        });
    }

    static void luminousDeviceUsesConfiguredItemCapacityWithoutTruncatingContents(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(10, 2000)) {
                helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 8)) == 8,
                        "Luminous Device should accept items below its configured capacity");
                helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.LANTERN, 5)) == 2,
                        "Luminous Device should partially insert up to its configured capacity");

                try (var lowered = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(5, 2000)) {
                    helper.assertTrue(LuminousDevice.getStoredItemCount(deviceStack) == 10,
                            "Lowering capacity should not truncate existing contents");
                    helper.assertTrue(LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.GLOWSTONE)) == 0,
                            "Contents above the lowered capacity should reject new items");
                }
            }
        });
    }

    static void luminousDeviceRefillsManaFromSupportedPotionContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(1024, 2000)) {
                var player = new FakePlayer(
                        helper.getLevel(),
                        new GameProfile(UUID.randomUUID(), "luminous_device_mana_refill_test")
                );
                var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
                var manaPotion = createInstantManaPotion(
                        io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(),
                        Items.POTION
                );

                var potionResult = rightClickDevice(deviceStack, manaPotion, player);
                helper.assertTrue(potionResult.handled(), "Luminous Device should accept a regular mana potion");
                helper.assertTrue(potionResult.remainingStack().is(Items.GLASS_BOTTLE),
                        "Consumed regular mana potion should leave a glass bottle");
                helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 125,
                        "Mana I should recover 25 + 5% of the configured 2000 maximum");

                LuminousDevice.setStoredMana(deviceStack, 0);
                var flask = AbstractPotionFlaskItem.copyWithAddedDoses(
                        new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()),
                        manaPotion,
                        2
                );
                if (EnchantmentRegistry.GLOW_ENERGY.isPresent()) {
                    flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), 2);
                }
                var flaskResult = rightClickDevice(deviceStack, flask, player);
                helper.assertTrue(flaskResult.handled(),
                        "Luminous Device should accept a Spellcaster's Flask containing mana potion");
                helper.assertTrue(AbstractPotionFlaskItem.getStoredDoseCount(flaskResult.remainingStack()) == 1,
                        "Spellcaster's Flask should consume exactly one dose");
                var expectedFlaskRecovery = EnchantmentRegistry.GLOW_ENERGY.isPresent() ? 375 : 125;
                helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == expectedFlaskRecovery,
                        "Spellcaster's Flask recovery should include Glow Energy amplifier levels");

                LuminousDevice.setStoredMana(deviceStack, 1999);
                var overflowResult = rightClickDevice(deviceStack, manaPotion, player);
                helper.assertTrue(overflowResult.handled(),
                        "A Luminous Device with one mana of space should still consume one dose");
                helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 2000,
                        "Recovery beyond capacity should overflow instead of exceeding the maximum");

                var fullResult = rightClickDevice(deviceStack, manaPotion, player);
                helper.assertFalse(fullResult.handled(), "A full Luminous Device should reject mana potion");
                helper.assertTrue(fullResult.remainingStack().is(Items.POTION),
                        "Rejected mana potion should not be consumed");
            }
        });
    }

    static void luminousDeviceRejectsUnsupportedPotionContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(1024, 2000)) {
                var player = new FakePlayer(
                        helper.getLevel(),
                        new GameProfile(UUID.randomUUID(), "luminous_device_mana_rejection_test")
                );
                var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
                var manaPotion = createInstantManaPotion(
                        io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(),
                        Items.POTION
                );
                var alchemistsFlask = AbstractPotionFlaskItem.copyWithAddedDose(
                        new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get()),
                        manaPotion
                );
                var rejectedStacks = List.of(
                        createInstantManaPotion(
                                io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(),
                                Items.SPLASH_POTION
                        ),
                        createInstantManaPotion(
                                io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get(),
                                Items.LINGERING_POTION
                        ),
                        PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.HEALING),
                        alchemistsFlask
                );

                for (var rejectedStack : rejectedStacks) {
                    var result = rightClickDevice(deviceStack, rejectedStack, player);
                    helper.assertFalse(result.handled(),
                            "Luminous Device accepted an unsupported potion container: " + rejectedStack);
                    helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 0,
                            "Rejected potion container should not add mana");
                }
            }
        });
    }

    static void luminousDeviceTooltipUsesSyncedCapacityAndCyanManaValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDeviceConfigState.set(12, 345);
            try {
                try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(12, 345)) {
                    LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 3));
                }
                LuminousDevice.setStoredMana(deviceStack, 67);
                var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
                deviceStack.getItem().appendHoverText(deviceStack, helper.getLevel(), lines, TooltipFlag.NORMAL);

                helper.assertTrue(lines.size() == 6, "Selected Luminous Device should append upgrade and mode tooltip lines");
                helper.assertTrue("(3/12)".equals(lines.get(0).getString()),
                        "Tooltip should display literal item count and synced capacity");
                helper.assertTrue(lines.get(3).getString().contains("67")
                                && lines.get(3).getString().contains("345"),
                        "Tooltip should display stored and maximum mana without decimals");
                var manaContents = (net.minecraft.network.chat.contents.TranslatableContents) lines.get(3).getContents();
                var args = manaContents.getArgs();
                helper.assertTrue(args.length == 2
                                && args[0] instanceof net.minecraft.network.chat.Component currentMana
                                && args[1] instanceof net.minecraft.network.chat.Component maxMana
                                && currentMana.getStyle().getColor() != null
                                && maxMana.getStyle().getColor() != null
                                && currentMana.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor()
                                && maxMana.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(),
                        "Both mana values should always use cyan formatting");
                helper.assertTrue(lines.get(5).getContents()
                                instanceof net.minecraft.network.chat.contents.TranslatableContents modeContents
                                && "item.apprenticecodex.luminous_device.mode".equals(modeContents.getKey()),
                        "The selected placement mode should be displayed on the final tooltip line");
            } finally {
                LuminousDeviceConfigState.reset();
            }
        });
    }

    static void luminousDeviceManaBarUsesSyncedCapacityWithoutItemDamage(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            LuminousDeviceConfigState.set(1024, 2000);
            try {
                helper.assertFalse(deviceStack.getItem().isBarVisible(deviceStack),
                        "Luminous Device should hide its mana bar when empty");

                LuminousDevice.setStoredMana(deviceStack, 1);
                helper.assertTrue(deviceStack.getItem().isBarVisible(deviceStack),
                        "Luminous Device should show its mana bar when mana is stored");
                helper.assertTrue(deviceStack.getItem().getBarWidth(deviceStack) == 1,
                        "Any positive mana should render at least one bar pixel");
                helper.assertTrue(deviceStack.getItem().getBarColor(deviceStack) == 0x4F88E8,
                        "Luminous Device mana bar should use the fixed flask blue");

                LuminousDevice.setStoredMana(deviceStack, 1000);
                helper.assertTrue(deviceStack.getItem().getBarWidth(deviceStack) == 7,
                        "Half of the configured mana capacity should round to seven bar pixels");

                LuminousDevice.setStoredMana(deviceStack, 2000);
                helper.assertTrue(deviceStack.getItem().getBarWidth(deviceStack) == 13,
                        "Full mana should render the complete bar");

                LuminousDevice.setStoredMana(deviceStack, 2500);
                helper.assertTrue(deviceStack.getItem().getBarWidth(deviceStack) == 13,
                        "Mana above a lowered capacity should clamp to the complete bar");
                LuminousDeviceConfigState.set(1024, 0);
                helper.assertTrue(deviceStack.getItem().getBarWidth(deviceStack) == 13,
                        "Stored mana should remain visible when configured capacity is zero");
                helper.assertFalse(deviceStack.isDamageableItem(),
                        "Luminous Device mana bar should not make the item damageable");
                helper.assertTrue(deviceStack.getDamageValue() == 0,
                        "Luminous Device mana bar should not use the item damage value");
            } finally {
                LuminousDeviceConfigState.reset();
            }
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
                                && views.get(0).mode() == LuminousDevice.Mode.PLACE
                                && "0".equals(views.get(0).badgeText()),
                        "Selection UI data should retain the selected zero-count item without locked functions");
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

    static void luminousDeviceModesUpdateSelectionNameAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var configBuffer = new FriendlyByteBuf(Unpooled.buffer());
            SyncLuminousDeviceConfigPacket.encode(
                    new SyncLuminousDeviceConfigPacket(12, 345, 2, 47.5D),
                    configBuffer
            );
            var decodedConfig = SyncLuminousDeviceConfigPacket.decode(configBuffer);
            helper.assertTrue(decodedConfig.maxStoredItems() == 12
                            && decodedConfig.maxStoredMana() == 345
                            && decodedConfig.upgradedMaxStoredMana() == 5000
                            && decodedConfig.cleanRadius() == 2
                            && Math.abs(decodedConfig.mageLightExtendedRange() - 47.5D) < 1.0E-9D,
                    "Luminous Device config sync should preserve the cleanup radius and Mage Light range");

            var mageLightConfigBuffer = new FriendlyByteBuf(Unpooled.buffer());
            SyncMageLightConfigPacket.encode(new SyncMageLightConfigPacket(48.0D), mageLightConfigBuffer);
            var decodedMageLightConfig = SyncMageLightConfigPacket.decode(mageLightConfigBuffer);
            helper.assertTrue(Math.abs(decodedMageLightConfig.maxRange() - 48.0D) < 1.0E-9D,
                    "Mage Light config sync should preserve the configured maximum range");

            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            var emptyViews = LuminousDevice.getSelectionViews(deviceStack);
            helper.assertTrue(emptyViews.isEmpty(),
                    "An unupgraded empty Luminous Device should not expose locked functions");

            var emptyLines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            deviceStack.getItem().appendHoverText(deviceStack, helper.getLevel(), emptyLines, TooltipFlag.NORMAL);
            helper.assertTrue(emptyLines.size() == 5,
                    "Empty placement mode should show all upgrades and omit the mode tooltip line");
            var lockedUpgradeContents = (net.minecraft.network.chat.contents.TranslatableContents)
                    emptyLines.get(4).getContents();
            helper.assertTrue(java.util.Arrays.stream(lockedUpgradeContents.getArgs()).allMatch(argument ->
                            argument instanceof net.minecraft.network.chat.Component component
                                    && component.getStyle().getColor() != null
                                    && component.getStyle().getColor().getValue() == ChatFormatting.GRAY.getColor()),
                    "Locked upgrades should all be gray");

            for (var upgrade : LuminousDeviceUpgrade.values()) {
                helper.assertTrue(LuminousDevice.addUpgrade(deviceStack, upgrade),
                        "Each independent Luminous Device upgrade should be applicable");
            }
            var upgradedViews = LuminousDevice.getSelectionViews(deviceStack);
            helper.assertTrue(upgradedViews.size() == 3
                            && upgradedViews.get(0).mode() == LuminousDevice.Mode.CLEAN
                            && upgradedViews.get(1).spellId().equals(SpellRegistry.MAGE_LIGHT.get().getSpellResource())
                            && upgradedViews.get(2).spellId().equals(SpellRegistry.WIZARDLAMP.get().getSpellResource()),
                    "An upgraded empty Luminous Device should expose all unlocked functions");
            var upgradedLines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
            deviceStack.getItem().appendHoverText(deviceStack, helper.getLevel(), upgradedLines, TooltipFlag.NORMAL);
            var unlockedUpgradeContents = (net.minecraft.network.chat.contents.TranslatableContents)
                    upgradedLines.get(4).getContents();
            helper.assertTrue(java.util.Arrays.stream(unlockedUpgradeContents.getArgs()).allMatch(argument ->
                            argument instanceof net.minecraft.network.chat.Component component
                                    && component.getStyle().getColor() != null
                                    && component.getStyle().getColor().getValue() == ChatFormatting.GREEN.getColor()),
                    "Unlocked upgrades should all be green");
            helper.assertTrue(upgradedLines.get(3).getString().contains("5000"),
                    "Mana upgrade should use the synced upgraded capacity");

            LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 2));
            helper.assertTrue(LuminousDevice.getMode(deviceStack) == LuminousDevice.Mode.PLACE
                            && LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                    "Existing and newly populated devices should default to placement mode");

            LuminousDeviceConfigState.set(1024, 2000, 2);
            try {
                helper.assertTrue(LuminousDevice.setCleanMode(deviceStack),
                        "A valid Luminous Device should accept clean mode");
                helper.assertTrue(LuminousDevice.getMode(deviceStack) == LuminousDevice.Mode.CLEAN
                                && LuminousDevice.getSelectedStack(deviceStack).isEmpty(),
                        "Clean mode should make the retained item selection inactive");

                var cleanViews = LuminousDevice.getSelectionViews(deviceStack);
                helper.assertTrue(cleanViews.size() == 4
                                && cleanViews.stream().noneMatch(view ->
                                view.mode() == LuminousDevice.Mode.PLACE && view.currentSelection())
                                && cleanViews.get(1).mode() == LuminousDevice.Mode.CLEAN
                                && cleanViews.get(1).currentSelection()
                                && cleanViews.get(2).mode() == LuminousDevice.Mode.SPELL
                                && cleanViews.get(3).mode() == LuminousDevice.Mode.SPELL,
                        "Only the clean entry should be current while cleaning");

                var cleanName = deviceStack.getItem().getName(deviceStack);
                var cleanNameContents = (net.minecraft.network.chat.contents.TranslatableContents)
                        cleanName.getContents();
                helper.assertTrue("item.apprenticecodex.luminous_device.with_select"
                                .equals(cleanNameContents.getKey())
                                && cleanNameContents.getArgs().length == 2
                                && cleanNameContents.getArgs()[1] instanceof net.minecraft.network.chat.Component modeName
                                && modeName.getContents()
                                instanceof net.minecraft.network.chat.contents.TranslatableContents modeNameContents
                                && "item.apprenticecodex.luminous_device.mode.clean"
                                .equals(modeNameContents.getKey()),
                        "Clean mode should use the clean label in the item display name");

                var cleanLines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
                deviceStack.getItem().appendHoverText(
                        deviceStack,
                        helper.getLevel(),
                        cleanLines,
                        TooltipFlag.NORMAL
                );
                helper.assertTrue(cleanLines.size() == 6,
                        "Clean mode should append its mode tooltip line");
                var modeContents = (net.minecraft.network.chat.contents.TranslatableContents)
                        cleanLines.get(5).getContents();
                var cleanSize = (net.minecraft.network.chat.Component) modeContents.getArgs()[1];
                var cleanSizeContents = (net.minecraft.network.chat.contents.TranslatableContents)
                        cleanSize.getContents();
                helper.assertTrue(cleanSizeContents.getArgs().length == 3
                                && Integer.valueOf(5).equals(cleanSizeContents.getArgs()[0])
                                && Integer.valueOf(5).equals(cleanSizeContents.getArgs()[1])
                                && Integer.valueOf(5).equals(cleanSizeContents.getArgs()[2]),
                        "Clean tooltip size should be calculated as 1 + radius * 2");

                helper.assertTrue(LuminousDevice.setSelectedStack(deviceStack, new ItemStack(Items.TORCH)),
                        "Selecting a stored item should leave clean mode");
                helper.assertTrue(LuminousDevice.getMode(deviceStack) == LuminousDevice.Mode.PLACE
                                && LuminousDevice.getSelectedStack(deviceStack).is(Items.TORCH),
                        "Item selection should reactivate placement mode and the retained item");

                var mageLight = SpellRegistry.MAGE_LIGHT.get();
                helper.assertTrue(LuminousDevice.setSelectedSpell(deviceStack, mageLight.getSpellResource()),
                        "Luminous Device should accept Mage Light as a fixed spell");
                helper.assertTrue(LuminousDevice.getMode(deviceStack) == LuminousDevice.Mode.SPELL
                                && LuminousDevice.getSelectedStack(deviceStack).isEmpty()
                                && LuminousDevice.getSelectedSpellData(deviceStack).getSpell() == mageLight,
                        "Spell mode should retain but deactivate the selected item");

                var spellName = deviceStack.getItem().getName(deviceStack);
                var spellNameContents = (net.minecraft.network.chat.contents.TranslatableContents)
                        spellName.getContents();
                var itemNameSpell = (net.minecraft.network.chat.Component) spellNameContents.getArgs()[1];
                helper.assertTrue(itemNameSpell.getString().equals(mageLight.getDisplayName(null).getString())
                                && itemNameSpell.getStyle().getColor() == null,
                        "Spell mode item name should omit school color and level");

                var spellLines = new java.util.ArrayList<net.minecraft.network.chat.Component>();
                deviceStack.getItem().appendHoverText(
                        deviceStack,
                        helper.getLevel(),
                        spellLines,
                        TooltipFlag.NORMAL
                );
                var spellModeContents = (net.minecraft.network.chat.contents.TranslatableContents)
                        spellLines.get(5).getContents();
                var spellModeName = (net.minecraft.network.chat.Component) spellModeContents.getArgs()[0];
                var tooltipSpell = (net.minecraft.network.chat.Component) spellModeContents.getArgs()[1];
                helper.assertTrue(spellModeName.getContents()
                                instanceof net.minecraft.network.chat.contents.TranslatableContents spellModeNameContents
                                && "item.apprenticecodex.luminous_device.mode.spell"
                                .equals(spellModeNameContents.getKey()),
                        "Spell tooltip should use the shared spell mode translation");
                helper.assertTrue(tooltipSpell.getString().endsWith(" 1")
                                && tooltipSpell.getStyle().getColor() != null
                                && tooltipSpell.getStyle().getColor().equals(
                                mageLight.getSchoolType().getDisplayName().getStyle().getColor()
                        ),
                        "Spell tooltip should show level one in the school color");

                var packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
                ClientConfirmLuminousDeviceSelectionPacket.encode(
                        new ClientConfirmLuminousDeviceSelectionPacket(
                                InteractionHand.MAIN_HAND,
                                LuminousDevice.Mode.SPELL,
                                ItemStack.EMPTY,
                                mageLight.getSpellResource()
                        ),
                        packetBuffer
                );
                var decodedSelection = ClientConfirmLuminousDeviceSelectionPacket.decode(packetBuffer);
                helper.assertTrue(decodedSelection.mode() == LuminousDevice.Mode.SPELL
                                && mageLight.getSpellResource().equals(decodedSelection.selectedSpellId()),
                        "Luminous Device selection packet should preserve the selected spell id");
            } finally {
                LuminousDeviceConfigState.reset();
            }
        });
    }

    static void luminousDeviceSpellModeUsesStoredManaWithoutSpellContainer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "luminous_device_spell_test")
            );
            player.setPos(
                    helper.absolutePos(new BlockPos(1, 1, 1)).getX() + 0.5D,
                    helper.absolutePos(new BlockPos(1, 1, 1)).getY(),
                    helper.absolutePos(new BlockPos(1, 1, 1)).getZ() + 0.5D
            );
            var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, deviceStack);
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(0.0F);

            helper.assertTrue(deviceStack.getItem() instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Luminous Device should block external spell imbuement as a UniqueItem");
            helper.assertFalse(ISpellContainer.isSpellContainer(deviceStack),
                    "Luminous Device should not expose its fixed spells through a SpellContainer");

            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            LuminousDevice.addUpgrade(deviceStack, LuminousDeviceUpgrade.ENHANCED_MAGE_LIGHT);
            LuminousDevice.addUpgrade(deviceStack, LuminousDeviceUpgrade.MANA_WIZARDLAMP);
            LuminousDevice.setSelectedSpell(deviceStack, mageLight.getSpellResource());
            LuminousDevice.setStoredMana(deviceStack, 100);
            var resolvedSpell = RightClickSpellResolver.resolve(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(resolvedSpell.isPresent()
                            && resolvedSpell.get().spellData().getSpell() == mageLight
                            && "luminous_device_selected".equals(resolvedSpell.get().resolutionPath()),
                    "Right-click targeting should resolve the selected fixed spell without a spell wheel entry");

            var mageLightPos = helper.absolutePos(new BlockPos(3, 1, 1));
            helper.setBlock(new BlockPos(3, 0, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 1, 1), Blocks.AIR);
            setPendingTarget(player, mageLight, mageLightPos);
            magicData.setMana(mageLight.getManaCost(1));
            helper.assertTrue(mageLight.canBeCastedBy(
                            1,
                            CastSource.SWORD,
                            magicData,
                            player
                    ).isSuccess(),
                    "Mage Light should pass the standard pre-cast eligibility checks");
            magicData.setMana(0.0F);
            helper.assertTrue(BlockTargetingHelper.peekValidatedPendingTarget(
                            helper.getLevel(),
                            player,
                            mageLight.getSpellResource(),
                            8.0D
                    ).isPresent(),
                    "Mage Light should receive a valid pending block target");
            var mageLightResult = deviceStack.getItem().use(
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND
            );
            helper.assertTrue(mageLightResult.getResult().consumesAction(),
                    "Mage Light should initiate from Luminous Device");
            helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 80,
                    "Mage Light should consume its configured mana cost from Luminous Device");
            mageLight.castSpell(helper.getLevel(), 1, player, CastSource.SWORD, true);
            mageLight.onServerCastComplete(helper.getLevel(), 1, player, magicData, false);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0E-4F,
                    "Mage Light should leave player mana unchanged after its temporary validation loan");

            var wizardlamp = SpellRegistry.WIZARDLAMP.get();
            LuminousDevice.setSelectedSpell(deviceStack, wizardlamp.getSpellResource());
            var wizardlampPos = helper.absolutePos(new BlockPos(4, 2, 1));
            helper.setBlock(new BlockPos(4, 2, 1), Blocks.AIR);
            var wizardlampTarget = setPendingTarget(player, wizardlamp, wizardlampPos);
            helper.assertTrue(jp.aquafactory.apprenticecodex.spell.wizardlamp.Wizardlamp.resolveClientPlacePos(
                            helper.getLevel(),
                            player,
                            wizardlampTarget,
                            6.0D
                    ).isPresent(),
                    "Wizardlamp should accept its pending block target");
            var wizardlampResult = deviceStack.getItem().use(
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND
            );
            helper.assertTrue(wizardlampResult.getResult().consumesAction(),
                    "Wizardlamp should initiate from Luminous Device");
            helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 30,
                    "Wizardlamp should consume its configured mana cost from Luminous Device");
            wizardlamp.castSpell(helper.getLevel(), 1, player, CastSource.SWORD, true);
            wizardlamp.onServerCastComplete(helper.getLevel(), 1, player, magicData, false);
            helper.assertTrue(Math.abs(magicData.getMana()) < 1.0E-4F,
                    "Wizardlamp should also leave player mana unchanged");

            magicData.getPlayerCooldowns().removeCooldown(mageLight.getSpellId());
            LuminousDevice.setSelectedSpell(deviceStack, mageLight.getSpellResource());
            LuminousDevice.setStoredMana(deviceStack, 19);
            magicData.setMana(500.0F);
            var playerManaBeforeInsufficientCast = magicData.getMana();
            setPendingTarget(player, mageLight, helper.absolutePos(new BlockPos(5, 1, 1)));
            var insufficientResult = deviceStack.getItem().use(
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND
            );
            helper.assertTrue(insufficientResult.getResult() == InteractionResult.FAIL,
                    "Luminous Device should reject a spell when its own mana is insufficient");
            helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 19
                            && Math.abs(magicData.getMana() - playerManaBeforeInsufficientCast) < 1.0E-4F,
                    "Insufficient device mana should not fall back to or consume player mana");
            helper.assertFalse(ISpellContainer.isSpellContainer(deviceStack),
                    "Selecting and casting fixed spells should never create a SpellContainer");
        });
    }

    static void luminousDeviceMageLightProfileScalesManaAndDisablesRedundantExtension(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var extended = new MageLightCastProfile(8.0D, 32.0D, 20);
            helper.assertTrue(extended.extendsRange(),
                    "A larger Luminous Device range should enable Mage Light extension");
            helper.assertTrue(extended.manaCostAt(8.0D) == 20
                            && extended.manaCostAt(24.0D) == 60
                            && extended.manaCostAt(32.0D) == 80,
                    "Extended Mage Light mana should scale with distance");
            helper.assertTrue(extended.manaCostAt(8.01D) == 21,
                    "Fractional extended Mage Light mana should round up");

            var redundant = new MageLightCastProfile(32.0D, 16.0D, 20);
            helper.assertFalse(redundant.extendsRange(),
                    "A Luminous Device range below the normal range should disable extension");
            helper.assertTrue(Math.abs(redundant.effectiveRange() - 32.0D) < 1.0E-9D
                            && redundant.maximumManaCost() == 20,
                    "Disabled extension should retain normal Mage Light range and mana");
        });
    }

    static void luminousDeviceCleanModeRemovesLightsAndRecoversConfiguredMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceCleanConfigOverrideForGameTest(
                    1,
                    7,
                    11
            )) {
                var player = new FakePlayer(
                        helper.getLevel(),
                        new GameProfile(UUID.randomUUID(), "luminous_device_clean_test")
                );
                var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
                LuminousDevice.addUpgrade(deviceStack, LuminousDeviceUpgrade.CLEAN);
                LuminousDevice.setCleanMode(deviceStack);
                LuminousDevice.setStoredMana(deviceStack, 100);
                player.setItemInHand(InteractionHand.MAIN_HAND, deviceStack);

                var center = new BlockPos(2, 1, 2);
                helper.setBlock(center, BlockRegistry.MAGE_LIGHT_TORCH.get());
                helper.setBlock(center.offset(1, 1, 1), BlockRegistry.WIZARDLAMP_LANTERN.get());
                helper.setBlock(center.offset(2, 0, 0), BlockRegistry.MAGE_LIGHT_TORCH.get());

                var result = useDeviceOn(helper, player, center);
                helper.assertTrue(result.consumesAction(),
                        "Clean mode should consume a targeted block interaction");
                helper.assertBlockNotPresent(BlockRegistry.MAGE_LIGHT_TORCH.get(), center);
                helper.assertBlockNotPresent(BlockRegistry.WIZARDLAMP_LANTERN.get(), center.offset(1, 1, 1));
                helper.assertBlockPresent(BlockRegistry.MAGE_LIGHT_TORCH.get(), center.offset(2, 0, 0));
                helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 118,
                        "Clean mode should recover the configured mana for both removed light types");
                helper.assertTrue(player.getCooldowns().isOnCooldown(ItemRegistry.LUMINOUS_DEVICE.get()),
                        "Successful cleaning should apply a one-second item cooldown");

                player.getCooldowns().removeCooldown(ItemRegistry.LUMINOUS_DEVICE.get());
                LuminousDevice.setStoredMana(deviceStack, 1998);
                var cappedCenter = new BlockPos(6, 1, 2);
                helper.setBlock(cappedCenter, BlockRegistry.MAGE_LIGHT_TORCH.get());
                useDeviceOn(helper, player, cappedCenter);
                helper.assertTrue(LuminousDevice.getStoredMana(deviceStack) == 2000,
                        "Recovered mana should be discarded beyond the configured capacity");

                player.getCooldowns().removeCooldown(ItemRegistry.LUMINOUS_DEVICE.get());
                var missingCenter = new BlockPos(8, 1, 4);
                helper.setBlock(missingCenter, Blocks.STONE);
                var missingResult = useDeviceOn(helper, player, missingCenter);
                helper.assertTrue(missingResult.consumesAction()
                                && player.getCooldowns().isOnCooldown(ItemRegistry.LUMINOUS_DEVICE.get()),
                        "A targeted clean attempt with no lights should still consume the action and apply cooldown");

                player.getCooldowns().removeCooldown(ItemRegistry.LUMINOUS_DEVICE.get());
                var airResult = deviceStack.getItem().use(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND
                );
                helper.assertTrue(airResult.getResult() == InteractionResult.PASS
                                && !player.getCooldowns().isOnCooldown(ItemRegistry.LUMINOUS_DEVICE.get()),
                        "Clean mode should do nothing when no block is targeted");
            }
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

    private static RefillInteractionResult rightClickDevice(
            ItemStack deviceStack,
            ItemStack inputStack,
            FakePlayer player
    ) {
        var deviceContainer = new SimpleContainer(deviceStack);
        var inputContainer = new SimpleContainer(inputStack.copy());
        var slot = new Slot(deviceContainer, 0, 0, 0);
        var handled = deviceStack.getItem().overrideOtherStackedOnMe(
                deviceStack,
                inputContainer.getItem(0),
                slot,
                ClickAction.SECONDARY,
                player,
                SlotAccess.forContainer(inputContainer, 0)
        );
        return new RefillInteractionResult(handled, inputContainer.getItem(0).copy());
    }

    private static BlockTargetData setPendingTarget(
            FakePlayer player,
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell,
            BlockPos placePos
    ) {
        var targetData = new BlockTargetData();
        targetData.setTarget(
                placePos.below(),
                Direction.UP,
                Vec3.atCenterOf(placePos.below()),
                placePos,
                Direction.DOWN
        );
        BlockTargetingHelper.setPendingServerTarget(player, spell.getSpellResource(), targetData);
        return targetData;
    }

    private static InteractionResult useDeviceOn(
            GameTestHelper helper,
            FakePlayer player,
            BlockPos relativePos
    ) {
        var absolutePos = helper.absolutePos(relativePos);
        return player.getMainHandItem().getItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(absolutePos),
                        Direction.UP,
                        absolutePos,
                        false
                )
        ));
    }

    private static ItemStack createInstantManaPotion(
            net.minecraft.world.item.alchemy.Potion potion,
            Item potionItem
    ) {
        return PotionUtils.setPotion(new ItemStack(potionItem), potion);
    }

    private record RefillInteractionResult(boolean handled, ItemStack remainingStack) {
    }

    private record PlacementCase(net.minecraft.world.item.Item item, Block block, BlockPos targetPos) {
    }
}
