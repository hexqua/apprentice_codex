package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import static jp.aquafactory.apprenticecodex.gametest.BowGameTestSupport.*;

final class ElementalBowGameTestScenarios {
    private ElementalBowGameTestScenarios() {
    }

    static void elementalBowHeldWisdomAndPlunderWorkInBothHands(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var state = Blocks.DIAMOND_ORE.defaultBlockState();

            var mainhandPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "elemental_bow_mainhand_held_enchant_test"));
            var mainhandBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            mainhandBow.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            mainhandBow.enchant(EnchantmentRegistry.PLUNDER.get(), 2);
            mainhandPlayer.setItemInHand(InteractionHand.MAIN_HAND, mainhandBow);

            var mainhandExperience = new BlockEvent.BreakEvent(level, new BlockPos(3, 2, 0), state, mainhandPlayer);
            mainhandExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(mainhandExperience);
            helper.assertTrue(mainhandExperience.getExpToDrop() == 4,
                    "Elemental Bow mainhand Wisdom should increase block experience from 3 to 4 but got " + mainhandExperience.getExpToDrop());

            var mainhandLootingEvent = new net.minecraftforge.event.entity.living.LootingLevelEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1)),
                    mainhandPlayer.damageSources().playerAttack(mainhandPlayer),
                    0
            );
            jp.aquafactory.apprenticecodex.enchantment.PlunderLootingLevelEvent.onLootingLevel(mainhandLootingEvent);
            helper.assertTrue(mainhandLootingEvent.getLootingLevel() == 2,
                    "Elemental Bow mainhand Plunder should set looting level to 2 but got " + mainhandLootingEvent.getLootingLevel());

            var offhandPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "elemental_bow_offhand_held_enchant_test"));
            var offhandBow = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            offhandBow.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            offhandBow.enchant(EnchantmentRegistry.PLUNDER.get(), 3);
            offhandPlayer.setItemInHand(InteractionHand.OFF_HAND, offhandBow);

            var offhandExperience = new BlockEvent.BreakEvent(level, new BlockPos(4, 2, 0), state, offhandPlayer);
            offhandExperience.setExpToDrop(3);
            WisdomExperienceDropEvent.onBlockBreak(offhandExperience);
            helper.assertTrue(offhandExperience.getExpToDrop() == 4,
                    "Elemental Bow offhand Wisdom should increase block experience from 3 to 4 but got " + offhandExperience.getExpToDrop());

            var offhandLootingEvent = new net.minecraftforge.event.entity.living.LootingLevelEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1)),
                    offhandPlayer.damageSources().playerAttack(offhandPlayer),
                    0
            );
            jp.aquafactory.apprenticecodex.enchantment.PlunderLootingLevelEvent.onLootingLevel(offhandLootingEvent);
            helper.assertTrue(offhandLootingEvent.getLootingLevel() == 3,
                    "Elemental Bow offhand Plunder should set looting level to 3 but got " + offhandLootingEvent.getLootingLevel());
        });
    }

    static void elementalBowKeepsVanillaBowEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            var expectedEnchantments = expectedElementalBowEnchantments();
            var expectedBookEnchantments = expectedElementalBowBookEnchantments();
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedEnchantments,
                    expectedBookEnchantments,
                    expectedEnchantments,
                    "Elemental Bow"
            );
        });
    }

    static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_view_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            var healingArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), net.minecraft.world.item.alchemy.Potions.HEALING);
            var regenerationArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), net.minecraft.world.item.alchemy.Potions.REGENERATION);
            var healingId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(healingArrow));
            var regenerationId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(regenerationArrow));
            helper.assertTrue(healingId != null && regenerationId != null,
                    "Elemental Bow selection view test could not resolve tipped arrow potion ids");
            var availablePotionIds = new LinkedHashSet<ResourceLocation>();
            if (healingId != null) {
                availablePotionIds.add(healingId);
            }
            if (regenerationId != null) {
                availablePotionIds.add(regenerationId);
            }
            var expectedPotionOrder = ForgeRegistries.POTIONS.getValues().stream()
                    .map(ForgeRegistries.POTIONS::getKey)
                    .filter(id -> id != null && availablePotionIds.contains(id))
                    .toList();

            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));
            player.getInventory().setItem(2, healingArrow);
            player.getInventory().setItem(3, regenerationArrow);

            var views = ElementalBow.getAvailableSelectionViews(player, stack);
            var actualSelections = views.stream()
                    .map(BowGameTestSupport::describeElementalBowSelectionView)
                    .toList();
            var expectedSelections = new ArrayList<String>();
            expectedSelections.add("normal");
            expectedSelections.add("arrow");
            expectedSelections.add("special:minecraft:spectral_arrow");
            for (var potionId : expectedPotionOrder) {
                expectedSelections.add("special:" + potionId);
            }
            expectedSelections.add("magic:" + SchoolRegistry.FIRE_RESOURCE);
            expectedSelections.add("magic:" + SchoolRegistry.ENDER_RESOURCE);
            expectedSelections.add("magic:" + SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(actualSelections.equals(expectedSelections),
                    "Elemental Bow selection view order mismatch: expected=" + expectedSelections + ", actual=" + actualSelections);
            helper.assertTrue(views.get(0).iconStack().is(Items.BOW),
                    "Elemental Bow vanilla mode selection should render as a bow icon");
            helper.assertTrue(views.get(0).badgeText() == null,
                    "Elemental Bow vanilla mode selection should not show an ammo badge");
            helper.assertTrue(views.get(1).iconStack().is(Items.ARROW),
                    "Elemental Bow arrow-only selection should render as an arrow icon");
            helper.assertTrue("\u221e".equals(views.get(1).badgeText()),
                    "Elemental Bow arrow-only selection should show infinity while Infinity is enchanted: " + views.get(1).badgeText());

            var fireView = views.stream()
                    .filter(view -> "magic".equals(view.selection().shotMode()) && SchoolRegistry.FIRE_RESOURCE.equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(fireView != null, "Elemental Bow selection view should include Fire magic");
            if (fireView != null) {
                helper.assertTrue(fireView.iconKind() == ElementalBow.SelectionIconKind.SPELL,
                        "Elemental Bow magic selection should render as a spell icon");
                helper.assertTrue(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get().getSpellIconResource().equals(fireView.spellIcon()),
                        "Elemental Bow Fire magic selection should use the Fire Arrow spell icon");
            }
        });
    }

    static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(ElementalBow.getInventoryOverlayView(stack) == null,
                    "Elemental Bow normal mode should not expose an inventory overlay");

            setElementalBowShotSelection(stack, "arrow", null);
            var arrowOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(arrowOverlay != null,
                    "Elemental Bow arrow-only selection should expose an inventory overlay");
            if (arrowOverlay != null) {
                helper.assertTrue(arrowOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow arrow-only selection should render as an item overlay");
                helper.assertTrue(arrowOverlay.iconStack().is(Items.ARROW),
                        "Elemental Bow arrow-only selection should render the arrow icon");
            }

            var spectralArrowId = ResourceLocation.tryParse("minecraft:spectral_arrow");
            setElementalBowShotSelection(stack, "special", spectralArrowId);
            var spectralOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(spectralOverlay != null,
                    "Elemental Bow spectral selection should expose an inventory overlay");
            if (spectralOverlay != null) {
                helper.assertTrue(spectralOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow spectral selection should render as an item overlay");
                helper.assertTrue(spectralOverlay.iconStack().is(Items.SPECTRAL_ARROW),
                        "Elemental Bow spectral selection should render the spectral arrow icon");
            }

            var healingArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), net.minecraft.world.item.alchemy.Potions.HEALING);
            var healingId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(healingArrow));
            helper.assertTrue(healingId != null,
                    "Elemental Bow overlay test could not resolve the healing arrow potion id");
            if (healingId != null) {
                setElementalBowShotSelection(stack, "special", healingId);
                var tippedOverlay = ElementalBow.getInventoryOverlayView(stack);
                helper.assertTrue(tippedOverlay != null,
                        "Elemental Bow tipped arrow selection should expose an inventory overlay");
                if (tippedOverlay != null) {
                    helper.assertTrue(tippedOverlay.iconStack().is(Items.TIPPED_ARROW),
                            "Elemental Bow tipped arrow selection should render a tipped arrow icon");
                    helper.assertTrue(PotionUtils.getPotion(tippedOverlay.iconStack()) == net.minecraft.world.item.alchemy.Potions.HEALING,
                            "Elemental Bow tipped arrow overlay should keep the selected potion");
                }
            }

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            var fireOverlay = ElementalBow.getInventoryOverlayView(stack);
            helper.assertTrue(fireOverlay != null,
                    "Elemental Bow magic selection should expose an inventory overlay");
            if (fireOverlay != null) {
                helper.assertTrue(fireOverlay.iconKind() == ElementalBow.SelectionIconKind.ITEM,
                        "Elemental Bow magic selection should render as an item overlay");
                helper.assertTrue(fireOverlay.iconStack().is(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get()),
                        "Elemental Bow Fire mode should render the Fire rune icon");
            }
        });
    }

    static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_selection_overheat_overlay_test");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        helper.runAtTickTime(1, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null, "Elemental Bow overheat overlay test should expose the Fire magic selection");
            if (fireView != null) {
                helper.assertFalse(fireView.overheatActive(),
                        "Elemental Bow Fire selection should not be overheated before any cast");
                helper.assertTrue(fireView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Fire selection should start with an empty overheat overlay");
            }

            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    40
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.NATURE_RESOURCE,
                    20
            );

            var overheatedFireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(overheatedFireView != null && overheatedFireView.overheatActive(),
                    "Elemental Bow Fire selection should report active overheat immediately after cast");
            if (overheatedFireView != null) {
                helper.assertTrue(overheatedFireView.overheatFillRatio() == 1.0F,
                        "Elemental Bow Fire selection should start with a full overheat overlay: " + overheatedFireView.overheatFillRatio());
            }
        });

        helper.runAtTickTime(11, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null && fireView.overheatActive(),
                    "Elemental Bow Fire selection should still be overheated mid-cooldown");
            if (fireView != null) {
                helper.assertTrue(Mth.equal(fireView.overheatFillRatio(), 0.75F),
                        "Elemental Bow Fire selection should decay based on its own cooldown: " + fireView.overheatFillRatio());
            }

            var natureView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(natureView != null && natureView.overheatActive(),
                    "Elemental Bow Nature selection should track its own overheat independently");
            if (natureView != null) {
                helper.assertTrue(Mth.equal(natureView.overheatFillRatio(), 0.5F),
                        "Elemental Bow Nature selection should show its shorter cooldown independently: " + natureView.overheatFillRatio());
            }

            var enderView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.ENDER_RESOURCE);
            helper.assertTrue(enderView != null, "Elemental Bow overheat overlay test should expose the Ender magic selection");
            if (enderView != null) {
                helper.assertFalse(enderView.overheatActive(),
                        "Elemental Bow Ender selection should stay inactive when untouched");
                helper.assertTrue(enderView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Ender selection should not show an overheat overlay");
            }
        });

        helper.runAtTickTime(42, () -> {
            var fireView = findElementalBowSelectionView(player, stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireView != null, "Elemental Bow Fire selection should remain in the selection list after cooldown");
            if (fireView != null) {
                helper.assertFalse(fireView.overheatActive(),
                        "Elemental Bow Fire selection should clear overheat after cooldown expires");
                helper.assertTrue(fireView.overheatFillRatio() == 0.0F,
                        "Elemental Bow Fire selection overlay should be empty after cooldown expires");
            }
        });

        helper.runAtTickTime(43, helper::succeed);
    }

    static void elementalBowClampsPersistedFutureOverheat(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_future_overheat_test");

        helper.runAtTickTime(1, () -> {
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    40
            );
            var schoolTag = player.getPersistentData()
                    .getCompound("ApprenticeCodexElementalBowOverheat")
                    .getCompound(SchoolRegistry.FIRE_RESOURCE.toString());
            schoolTag.putLong("ExpireGameTime", player.level().getGameTime() + 72000L);

            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(
                    player,
                    SchoolRegistry.FIRE_RESOURCE
            );

            helper.assertTrue(state.expireGameTime() <= player.level().getGameTime() + 40L,
                    "Elemental Bow stored overheat should be clamped to the last applied duration");
            helper.assertTrue(schoolTag.getLong("ExpireGameTime") == state.expireGameTime(),
                    "Elemental Bow persistent overheat NBT should be rewritten after clamping");
            helper.succeed();
        });
    }

    static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_empty_selection_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));

            var selectedViews = ElementalBow.getAvailableSelectionViews(player, stack);
            var spectralView = selectedViews.stream()
                    .filter(view -> "special".equals(view.selection().shotMode())
                            && ResourceLocation.tryParse("minecraft:spectral_arrow").equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(spectralView != null, "Elemental Bow should keep the empty current special selection in the UI");
            if (spectralView != null) {
                helper.assertTrue("0".equals(spectralView.badgeText()),
                        "Elemental Bow empty current special selection should show 0 ammo");
                helper.assertTrue(spectralView.badgeColor() == 0xFF5555,
                        "Elemental Bow empty current special selection should render its ammo count in red");
            }

            setElementalBowShotSelection(stack, "normal", null);
            var normalViews = ElementalBow.getAvailableSelectionViews(player, stack);
            helper.assertTrue(normalViews.stream().noneMatch(view ->
                            "special".equals(view.selection().shotMode())
                                    && ResourceLocation.tryParse("minecraft:spectral_arrow").equals(view.selection().selectionId())),
                    "Elemental Bow should drop the empty special selection after another mode is chosen");
            var arrowView = normalViews.stream()
                    .filter(view -> "arrow".equals(view.selection().shotMode()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(arrowView != null, "Elemental Bow should always expose the arrow-only selection");
            if (arrowView != null) {
                helper.assertTrue("0".equals(arrowView.badgeText()),
                        "Elemental Bow arrow-only selection should show 0 ammo while empty");
                helper.assertTrue(arrowView.badgeColor() == 0xFF5555,
                        "Elemental Bow arrow-only selection should render empty ammo in red even while another mode is selected");
            }
        });
    }

    static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_mana_gate_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow mana gate test could not resolve player mana data");
            magicData.setMana(0.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow should fail to start drawing when mana is insufficient: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow should not enter use state without enough mana");
        });
    }

    static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.getOrCreateTag().putString("ElementalBowMode", "fire");

            item.initializeSpellContainer(stack);

            assertElementalBowSelection(helper, stack, null, null,
                    "Elemental Bow should clear unresolved legacy mode values back to normal mode");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container after falling back to normal mode");
        });
    }

    static void elementalBowSynchronizesSpellContainerToCurrentMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            item.initializeSpellContainer(stack);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Elemental Bow should expose a spell container outside NONE mode");
            helper.assertTrue(spellContainer != null && !spellContainer.isSpellWheel(),
                    "Elemental Bow should keep its derived spell out of the spell wheel");
            assertSpellData(
                    helper,
                    spellContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    1,
                    true,
                    "Elemental Bow should sync Fire mode into a locked spell container"
            );
        });
    }

    static void elementalBowSpellContainerAppliesPowerFlameAndClearsInNoneMode(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            stack.enchant(Enchantments.POWER_ARROWS, 2);
            stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), 1);
            stack.enchant(Enchantments.FLAMING_ARROWS, 1);

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            item.initializeSpellContainer(stack);
            helper.assertTrue(stack.getEnchantmentLevel(Enchantments.POWER_ARROWS) == 2,
                    "Elemental Bow spell container test should preserve POWER II on the stack");
            helper.assertTrue(stack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) == 1,
                    "Elemental Bow spell container test should preserve FLAME I on the stack");
            helper.assertTrue(stack.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get()) == 1,
                    "Elemental Bow spell container test should preserve TRANSCENDENCE I on the stack");
            var fireMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireMode != null, "Elemental Bow Fire mode should resolve from the loaded mode definitions");
            var expectedFireLevel = fireMode != null ? fireMode.resolveSpellLevel(stack) : 1;
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow should expose a displayed spell profile in Fire mode");
            helper.assertTrue(fireProfile.spell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    "Elemental Bow Fire mode should resolve Fire Arrow");
            helper.assertTrue(fireProfile.spellLevel() == expectedFireLevel,
                    "Elemental Bow Fire mode display level should stay in sync with the loaded mode resolver but got " + fireProfile.spellLevel());
            var fireContainer = ISpellContainer.get(stack);
            helper.assertTrue(fireContainer != null, "Elemental Bow Fire mode should keep a synced spell container");
            helper.assertTrue(fireContainer != null && !fireContainer.isSpellWheel(),
                    "Elemental Bow Fire mode container should stay hidden from the spell wheel");
            assertSpellData(
                    helper,
                    fireContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    expectedFireLevel,
                    true,
                    "Elemental Bow Fire mode container should stay in sync with the loaded mode resolver"
            );

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.ENDER_RESOURCE);
            item.initializeSpellContainer(stack);
            var enderMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.ENDER_RESOURCE);
            helper.assertTrue(enderMode != null, "Elemental Bow Ender mode should resolve from the loaded mode definitions");
            var expectedEnderLevel = enderMode != null ? enderMode.resolveSpellLevel(stack) : 1;
            var enderProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(enderProfile != null, "Elemental Bow should expose a displayed spell profile in Ender mode");
            helper.assertTrue(enderProfile.spell() == io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get(),
                    "Elemental Bow Ender mode should resolve Magic Arrow");
            helper.assertTrue(enderProfile.spellLevel() == expectedEnderLevel,
                    "Elemental Bow Ender mode display level should stay in sync with the loaded mode resolver but got " + enderProfile.spellLevel());
            var enderContainer = ISpellContainer.get(stack);
            helper.assertTrue(enderContainer != null, "Elemental Bow Ender mode should keep a synced spell container");
            helper.assertTrue(enderContainer != null && !enderContainer.isSpellWheel(),
                    "Elemental Bow Ender mode container should stay hidden from the spell wheel");
            assertSpellData(
                    helper,
                    enderContainer,
                    0,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get(),
                    expectedEnderLevel,
                    true,
                    "Elemental Bow Ender mode container should stay in sync with the loaded mode resolver"
            );

            stack.getOrCreateTag().remove("ElementalBowMode");
            item.initializeSpellContainer(stack);
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container in NONE mode");
        });
    }

    static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_spell_wheel_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            ((ElementalBow) stack.getItem()).initializeSpellContainer(stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var selectionManager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
            var mainhandSelections = selectionManager.getSpellsForSlot(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND);
            helper.assertTrue(mainhandSelections.isEmpty(),
                    "Elemental Bow should not add its derived spell to the mainhand spell wheel: " + mainhandSelections);
            helper.assertTrue(selectionManager.getSelection() == null,
                    "Elemental Bow should not create a selected spell from its derived container");
        });
    }

    static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            var scrollStack = createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get());

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(stack, scrollStack),
                    "Elemental Bow should reject Arcane Anvil spell imbuing regardless of scroll spell"
            );
        });
    }

    static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = ElementalBow.createInsufficientManaMessage(
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                    null
            );
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.irons_spellbooks.cast_error_mana",
                    "Elemental Bow mana error should use Iron's cast_error_mana key"
            );
        });
    }

    static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_partial_release_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow partial release test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult().consumesAction(),
                    "Elemental Bow should start drawing when mana and ammo are available: " + useResult.getResult());

            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 19);
            helper.assertTrue(stack.getDamageValue() == 0, "Elemental Bow should not lose durability before full draw");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow should not consume arrows before full draw");
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Elemental Bow should not consume mana before full draw: " + magicData.getMana());
        });
    }

    static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_infinity_draw_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow should start vanilla draw with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow should enter use state for Infinity vanilla draw");
        });
    }

    static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_vanilla_special_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow vanilla mode should start drawing with only special arrows available: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow vanilla mode should consume the special arrow that vanilla resolution selected");
        });
    }

    static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_only_mode_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow arrow-only mode should fail when only special arrows are available: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow arrow-only mode should not enter use state without normal arrows");
            helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                    "Elemental Bow arrow-only mode should not consume special arrows");
            assertElementalBowSelection(helper, stack, "arrow", null,
                    "Elemental Bow arrow-only mode should keep its selection while empty");
        });
    }

    static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_arrow_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowShotSelection(stack, "arrow", null);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow arrow-only mode should start drawing with Infinity even without arrows: " + result.getResult());
            helper.assertTrue(player.isUsingItem(), "Elemental Bow arrow-only mode should enter use state for Infinity draw");
        });
    }

    static void elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_special_arrow_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(firstUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing while the selected arrow exists: " + firstUse.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 20);
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(),
                    "Elemental Bow special mode should consume the selected arrow even with Infinity");

            var secondUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(secondUse.getResult().consumesAction(),
                    "Elemental Bow special mode should start drawing again with Infinity after the selected arrow runs out: " + secondUse.getResult());
            helper.assertTrue(player.isUsingItem(),
                    "Elemental Bow special mode should enter use state again while keeping its empty selection");
            assertElementalBowSelection(helper, stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"),
                    "Elemental Bow special mode should keep the selected arrow after ammo loss");
        });
    }

    static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_infinity_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(Enchantments.INFINITY_ARROWS, 1);
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == net.minecraft.world.InteractionResult.FAIL,
                    "Elemental Bow magic mode should fail to start without ammo even with Infinity: " + result.getResult());
            helper.assertFalse(player.isUsingItem(), "Elemental Bow magic mode should not enter use state without ammo");
        });
    }

    static void elementalBowAcceptsSynthesisEnchantmentsAndTooltip(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            helper.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack, EnchantmentRegistry.SYNTHESIS.get()),
                    "Elemental Bow should accept Synthesis at the enchanting table");
            helper.assertTrue(stack.getItem().isBookEnchantable(stack, createEnchantedBook(EnchantmentRegistry.SYNTHESIS.get())),
                    "Elemental Bow should accept Synthesis from enchanted books");
            helper.assertFalse(EnchantmentRegistry.SYNTHESIS.get().isCompatibleWith(Enchantments.INFINITY_ARROWS),
                    "Synthesis should be incompatible with Infinity");
            helper.assertFalse(EnchantmentRegistry.SYNTHESIS.get().isCompatibleWith(Enchantments.MENDING),
                    "Synthesis should be incompatible with Mending");

            assertTooltipKeyAt(helper, stack, 0, "item.apprenticecodex.elemental_bow.mode",
                    "Elemental Bow should always show the current mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.desc", ChatFormatting.GRAY,
                    "Elemental Bow should always show the description tooltip line");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment",
                    "Elemental Bow should not show spell ammo tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show Infinity spell tooltip while not in magic mode");
            assertTooltipKeyAbsent(helper, stack, "item.apprenticecodex.elemental_bow.spell.with_synthesis",
                    "Elemental Bow should not show Synthesis spell tooltip while not in magic mode");

            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            assertTooltipKeyAt(helper, stack, 1, "item.apprenticecodex.elemental_bow.desc",
                    "Elemental Bow should show the description below the mode tooltip line");
            assertTooltipKeyUsesColor(helper, stack, "item.apprenticecodex.elemental_bow.spell.no_enchantment", ChatFormatting.YELLOW,
                    "Elemental Bow should show the no-enchantment spell tooltip in magic mode");

            var infinityStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(infinityStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            infinityStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            assertTooltipKeyUsesColor(helper, infinityStack, "item.apprenticecodex.elemental_bow.spell.with_infinity", ChatFormatting.YELLOW,
                    "Elemental Bow should show the Infinity spell tooltip in magic mode");

            var synthesisStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(synthesisStack, "magic", SchoolRegistry.FIRE_RESOURCE);
            synthesisStack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            assertTooltipKeyUsesColor(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", ChatFormatting.AQUA,
                    "Elemental Bow should show the Synthesis spell tooltip in magic mode");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.with_synthesis",
                    "Elemental Bow should no longer show the legacy Synthesis tooltip key");

            synthesisStack.enchant(Enchantments.INFINITY_ARROWS, 1);
            assertTooltipKeyUsesColor(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_synthesis", ChatFormatting.AQUA,
                    "Elemental Bow should prefer the Synthesis spell tooltip when Synthesis and Infinity are both present");
            assertTooltipKeyAbsent(helper, synthesisStack, "item.apprenticecodex.elemental_bow.spell.with_infinity",
                    "Elemental Bow should not show the Infinity spell tooltip when Synthesis is also present");
        });
    }

    static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_empty_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis test could not resolve player mana data");
            magicData.setMana(250.0F);
            var initialMana = magicData.getMana();

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start without arrows when Synthesis is enchanted: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(stack.getDamageValue() == 1,
                    "Elemental Bow Synthesis magic shot should still damage the bow after a successful cast");
            helper.assertTrue(magicData.getMana() < initialMana,
                    "Elemental Bow Synthesis magic shot should still consume spell mana");
        });
    }

    static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_magic_synthesis_ammo_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(EnchantmentRegistry.SYNTHESIS.get(), 1);
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 3));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow Synthesis ammo test could not resolve player mana data");
            magicData.setMana(250.0F);

            var result = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult().consumesAction(),
                    "Elemental Bow magic mode should start with Synthesis while arrows are present: " + result.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            helper.assertTrue(player.getInventory().getItem(1).getCount() == 3,
                    "Elemental Bow Synthesis magic shot should not consume arrows even when arrows are available");
        });
    }

    static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            setElementalBowShotSelection(stack, "special", ResourceLocation.tryParse("minecraft:spectral_arrow"));

            item.initializeSpellContainer(stack);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should not expose a spell container outside magic mode");
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack) == null,
                    "Elemental Bow should not expose a displayed spell profile outside magic mode");
        });
    }

    static void elementalBowCooldownHelperIgnoresWeaponMultiplierButKeepsPlayerCooldownReduction(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_helper_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var cooldownAttribute = player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get());
            helper.assertTrue(cooldownAttribute != null, "Elemental Bow cooldown helper test could not resolve cooldown attribute");
            cooldownAttribute.addPermanentModifier(new AttributeModifier(
                    UUID.fromString("24565bf4-5900-4a8f-8e05-a9f4a0db3dd7"),
                    "apprenticecodex.elemental_bow.cooldown_helper_test",
                    0.35D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));

            var helperCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            var vanillaCooldown = io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(helperCooldown == vanillaCooldown,
                    "Elemental Bow cooldown helper should keep Iron's sword multiplier path: "
                            + helperCooldown + " / expected " + vanillaCooldown);

            var spellbookCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(spellbookCooldown > helperCooldown,
                    "Elemental Bow cooldown helper should still reflect the SWORD cooldown multiplier: "
                            + helperCooldown + " / spellbook " + spellbookCooldown);
        });
    }

    static void elementalBowSuppressesElementalArrowCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_cooldown_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow cooldown test could not resolve player mana data");
            magicData.setPlayerCastingItem(stack.copy());

            var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var expectedStoredCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 0,
                    "Elemental Bow should suppress elemental arrow cooldowns but got " + cooldownEvent.getEffectiveCooldown());
            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            0
                    ) == expectedStoredCooldown,
                    "Elemental Bow should store the helper cooldown for overheat timing"
            );

            var controlEvent = new SpellCooldownAddedEvent.Pre(
                    160,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get(),
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == 160,
                    "Elemental Bow cooldown suppression should not affect unrelated spells");
        });
    }

    static void elementalBowConsumesAdditionalManaWhileOverheated(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_mana_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow overheat mana test could not resolve player mana data");

            var item = (ElementalBow) stack.getItem();
            item.initializeSpellContainer(stack);
            var fireProfile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(fireProfile != null, "Elemental Bow overheat mana test should resolve the active Fire profile");
            var fireArrow = fireProfile != null
                    ? fireProfile.spell()
                    : io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            var baseMana = fireProfile != null ? fireProfile.spell().getManaCost(fireProfile.spellLevel()) : fireArrow.getManaCost(1);

            magicData.setMana(300.0F);
            var initialMana = magicData.getMana();

            magicData.setPlayerCastingItem(stack.copy());
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                    fireArrow,
                    player,
                    CastSource.SWORD
            );
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(cooldownEvent);
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            fireArrow.getSpellCooldown()
                    )
            );

            var extraMana = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    baseMana
            );
            helper.assertTrue(extraMana > 0.0F, "Elemental Bow should charge extra mana once Fire overheat is active");

            var overheatedUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(overheatedUseResult.getResult().consumesAction(),
                    "Elemental Bow should still allow a second overheated draw: " + overheatedUseResult.getResult());
            stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - ElementalBow.READY_DRAW_TICKS);
            player.stopUsingItem();

            var manaAfterOverheatedShot = magicData.getMana();
            helper.assertTrue(Math.abs(manaAfterOverheatedShot - (initialMana - baseMana - extraMana)) < 1.0e-3F,
                    "Elemental Bow overheated shot consumed the wrong mana: " + manaAfterOverheatedShot);
            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(state.active() && state.chainDepth() >= 2,
                    "Elemental Bow overheated shot should keep Fire overheat active and deepen the chain: " + state);
        });
    }

    static void elementalBowOverheatTracksSchoolsSeparately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_school_test");
            var fireStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            fireStack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.FIRE_RESOURCE.toString());

            var natureStack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            natureStack.getOrCreateTag().putString("ElementalBowMode", SchoolRegistry.NATURE_RESOURCE.toString());

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Elemental Bow school overheat test could not resolve player mana data");

            magicData.setPlayerCastingItem(fireStack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            160,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get(),
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            0
                    )
            );

            magicData.setPlayerCastingItem(natureStack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            120,
                            io.redspace.ironsspellbooks.api.registry.SpellRegistry.POISON_ARROW_SPELL.get(),
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.NATURE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.NATURE_RESOURCE,
                            0
                    )
            );

            var fireState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(fireState.active() && fireState.chainDepth() == 1,
                    "Elemental Bow fire overheat should stay isolated at depth 1: " + fireState);

            var natureState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.NATURE_RESOURCE);
            helper.assertTrue(natureState.active() && natureState.chainDepth() == 1,
                    "Elemental Bow nature overheat should stay isolated at depth 1: " + natureState);

            helper.assertTrue(
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                            player,
                            SchoolRegistry.ENDER_RESOURCE,
                            10.0F
                    ) == 0.0F,
                    "Elemental Bow should not leak overheat into untouched schools"
            );
        });
    }

    static void elementalBowOverheatRefreshesDurationAfterRepeatedCast(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_refresh_test");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
        var magicData = MagicData.getPlayerMagicData(player);
        var firstExpire = new java.util.concurrent.atomic.AtomicLong();
        var fireArrow = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
        var expectedCooldown = jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                fireArrow,
                player,
                CastSource.SWORD
        );

        helper.assertTrue(magicData != null, "Elemental Bow overheat refresh test could not resolve player mana data");

        helper.runAtTickTime(1, () -> {
            magicData.setPlayerCastingItem(stack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                            fireArrow,
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            expectedCooldown
                    )
            );
            firstExpire.set(jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE).expireGameTime());
        });

        helper.runAtTickTime(40, () -> {
            magicData.setPlayerCastingItem(stack.copy());
            jp.aquafactory.apprenticecodex.item.ElementalBowCastEvent.onSpellCooldownAdded(
                    new SpellCooldownAddedEvent.Pre(
                            io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(fireArrow, player, CastSource.SWORD),
                            fireArrow,
                            player,
                            CastSource.SWORD
                    )
            );
            jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                    player,
                    SchoolRegistry.FIRE_RESOURCE,
                    jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.consumePendingCooldown(
                            player,
                            SchoolRegistry.FIRE_RESOURCE,
                            expectedCooldown
                    )
            );

            var state = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
            helper.assertTrue(state.active(), "Elemental Bow repeated cast should keep fire overheat active");
            helper.assertTrue(state.chainDepth() == 2, "Elemental Bow repeated cast should raise overheat chain depth to 2: " + state.chainDepth());
            helper.assertTrue(state.expireGameTime() > firstExpire.get(),
                    "Elemental Bow repeated cast should refresh overheat expiry but got " + state.expireGameTime() + " <= " + firstExpire.get());
            helper.assertTrue(state.expireGameTime() - helper.getLevel().getGameTime() == expectedCooldown,
                    "Elemental Bow repeated cast should reset overheat duration from the latest cast");
        });

        helper.runAtTickTime(41, helper::succeed);
    }

    static void elementalBowMagicDrawTicksUseProfileAndServerMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.5D,
                    0.20D,
                    0.08D,
                    1.0D,
                    0,
                    0,
                    1.0D
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_draw_config_test");
                var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
                setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                player.getInventory().setItem(1, new ItemStack(Items.ARROW, 2));

                var magicData = MagicData.getPlayerMagicData(player);
                helper.assertTrue(magicData != null, "Elemental Bow draw config test could not resolve player mana data");
                magicData.setMana(300.0F);
                var initialMana = magicData.getMana();

                helper.assertTrue(ElementalBow.resolveMagicRequiredDrawTicks(stack) == 30,
                        "Elemental Bow required draw ticks should use profile ticks and server multiplier");
                var shortUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(shortUseResult.getResult().consumesAction(),
                        "Elemental Bow draw config test should start drawing: " + shortUseResult.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 29);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 0,
                        "Elemental Bow should not fire before configured draw ticks");
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 2,
                        "Elemental Bow should not consume arrows before configured draw ticks");
                helper.assertTrue(magicData.getMana() == initialMana,
                        "Elemental Bow should not consume mana before configured draw ticks");

                var readyUseResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                helper.assertTrue(readyUseResult.getResult().consumesAction(),
                        "Elemental Bow draw config test should restart drawing: " + readyUseResult.getResult());
                stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration() - 30);
                player.stopUsingItem();
                helper.assertTrue(stack.getDamageValue() == 1,
                        "Elemental Bow should fire at configured draw ticks");
                helper.assertTrue(player.getInventory().getItem(1).getCount() == 1,
                        "Elemental Bow should consume one arrow after configured draw ticks");
                helper.assertTrue(magicData.getMana() < initialMana,
                        "Elemental Bow should consume spell mana after configured draw ticks");
            }
        });
    }

    static void elementalBowAdditionalManaUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.5D,
                    0.25D,
                    1.0D,
                    0,
                    0,
                    1.0D
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_mana_config_test");
                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        100
                );

                var extraMana = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getAdditionalManaCost(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        100.0F
                );
                helper.assertTrue(Math.abs(extraMana - 75.0F) < 1.0e-3F,
                        "Elemental Bow additional mana should use its server config but got " + extraMana);
            }
        });
    }

    static void elementalBowOverheatDurationUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.20D,
                    0.08D,
                    2.0D,
                    30,
                    50,
                    1.0D
            )) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "elemental_bow_overheat_duration_config_test");

                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        10
                );
                var minState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
                helper.assertTrue(minState.active()
                                && minState.expireGameTime() - helper.getLevel().getGameTime() == 30,
                        "Elemental Bow overheat duration should use configured minimum: " + minState);

                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.clear(player, SchoolRegistry.FIRE_RESOURCE);
                jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.applyOverheatAfterCast(
                        player,
                        SchoolRegistry.FIRE_RESOURCE,
                        100
                );
                var capState = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager.getState(player, SchoolRegistry.FIRE_RESOURCE);
                helper.assertTrue(capState.active()
                                && capState.expireGameTime() - helper.getLevel().getGameTime() == 50,
                        "Elemental Bow overheat duration should use configured cap: " + capState);
            }
        });
    }

    static void elementalBowPowerSpellLevelBonusUsesServerConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowConfigOverrideForGameTest(
                    1.0D,
                    0.20D,
                    0.08D,
                    1.0D,
                    0,
                    0,
                    0.5D
            )) {
                var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
                var stack = new ItemStack(item);
                stack.enchant(Enchantments.POWER_ARROWS, 3);
                setElementalBowShotSelection(stack, "magic", SchoolRegistry.FIRE_RESOURCE);

                item.initializeSpellContainer(stack);

                var fireMode = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.getResolvedDefinition(SchoolRegistry.FIRE_RESOURCE);
                helper.assertTrue(fireMode != null, "Elemental Bow power config test should resolve Fire mode");
                var powerBonus = jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowModeManager.resolvePowerArrowSpellLevelBonus(stack);
                helper.assertTrue(powerBonus == 1,
                        "Elemental Bow Power III should add floor(3 * 0.5) spell levels but got " + powerBonus);
                var expectedLevel = fireMode == null ? 1 : Mth.clamp(1 + powerBonus, fireMode.spell().getMinLevel(), fireMode.spell().getMaxLevel());
                var profile = ElementalBow.getDisplayedSpellProfile(stack);
                helper.assertTrue(profile != null, "Elemental Bow power config test should expose a displayed spell profile");
                helper.assertTrue(profile != null && profile.spellLevel() == expectedLevel,
                        "Elemental Bow Power spell level should use the configured bonus before spell level clamp but got "
                                + (profile == null ? "null" : profile.spellLevel()));
            }
        });
    }
}
