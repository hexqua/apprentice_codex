package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowScrollStorage;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.StreamSupport;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowSelectionGameTests {
    private ElementalBowSelectionGameTests() {}

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var infinity = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_selection_view_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            stack.enchant(infinity, 1);
            prepareElementalBowScrolls(stack);
            var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.HEALING.value());
            var regenerationArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.REGENERATION.value());
            var healingId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(healingArrow));
            var regenerationId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(regenerationArrow));
            helper.assertTrue(healingId != null && regenerationId != null,
                    "Elemental Bow selection view test could not resolve tipped arrow potion ids");
            var availablePotionIds = new LinkedHashSet<ResourceLocation>();
            if (healingId != null) {
                availablePotionIds.add(healingId);
            }
            if (regenerationId != null) {
                availablePotionIds.add(regenerationId);
            }
            var expectedPotionOrder = StreamSupport.stream(BuiltInRegistries.POTION.spliterator(), false)
                    .map(BuiltInRegistries.POTION::getKey)
                    .filter(id -> id != null && availablePotionIds.contains(id))
                    .toList();

            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().setItem(1, new ItemStack(Items.SPECTRAL_ARROW));
            player.getInventory().setItem(2, healingArrow);
            player.getInventory().setItem(3, regenerationArrow);

            var views = ElementalBow.getAvailableSelectionViews(player, stack);
            var actualSelections = views.stream()
                    .map(ElementalBowSelectionTestSupport::describeElementalBowSelectionView)
                    .toList();
            var expectedSelections = new ArrayList<String>();
            expectedSelections.add("normal");
            expectedSelections.add("arrow");
            expectedSelections.add("special:minecraft:spectral_arrow");
            for (var potionId : expectedPotionOrder) {
                expectedSelections.add("special:" + potionId);
            }
            expectedSelections.add("magic:" + ElementalBow.selectionIdForSlot(0));
            expectedSelections.add("magic:" + ElementalBow.selectionIdForSlot(1));
            expectedSelections.add("magic:" + ElementalBow.selectionIdForSlot(2));
            helper.assertTrue(actualSelections.equals(expectedSelections),
                    "Elemental Bow selection view order mismatch: expected=" + expectedSelections + ", actual=" + actualSelections);
            helper.assertTrue(views.get(0).iconStack().is(Items.BOW),
                    "Elemental Bow vanilla mode selection should render as a bow icon");
            helper.assertTrue(views.get(0).badgeText() == null,
                    "Elemental Bow vanilla mode selection should not show an ammo badge");
            helper.assertTrue(views.get(1).iconStack().is(Items.ARROW),
                    "Elemental Bow arrow-only selection should render as an arrow icon");
            helper.assertTrue("∞".equals(views.get(1).badgeText()),
                    "Elemental Bow arrow-only selection should show infinity while Infinity is enchanted: " + views.get(1).badgeText());

            var fireView = views.stream()
                    .filter(view -> "magic".equals(view.selection().shotMode()) && ElementalBow.selectionIdForSlot(0).equals(view.selection().selectionId()))
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(fireView != null, "Elemental Bow selection view should include Fire magic");
            if (fireView != null) {
                helper.assertTrue(fireView.iconKind() == ElementalBow.SelectionIconKind.SPELL,
                        "Elemental Bow magic selection should render as a spell icon");
                helper.assertTrue(SpellRegistry.FIRE_ARROW_SPELL.get().getSpellIconResource().equals(fireView.spellIcon()),
                        "Elemental Bow Fire magic selection should use the Fire Arrow spell icon");
            }
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = useElementalBowSpellConfig(helper)) {
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

                var spectralArrowId = ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow");
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

                var healingArrow = PotionContentsHelper.createPotionStack(Items.TIPPED_ARROW, Potions.HEALING.value());
                var healingId = BuiltInRegistries.POTION.getKey(PotionContentsHelper.getPotion(healingArrow));
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
                        helper.assertTrue(PotionContentsHelper.getPotion(tippedOverlay.iconStack()) == Potions.HEALING.value(),
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
                            "Elemental Bow Fire mode should render the Fire rune icon: " + fireOverlay.iconStack() + " / " + ElementalBow.getConfiguredSchoolId(stack));
                }
            }
        });
    }

    @GameTest(template = "gametest/bow_casting", batch = "apprenticecodex.elemental_bow_overheat", timeoutTicks = 80)
    public static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_shared_overlay");
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        prepareElementalBowScrolls(stack);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.runAtTickTime(1, () -> ElementalBowOverheatManager.applyOverheatAfterCast(player, 40));
        helper.runAtTickTime(11, () -> {
            var views = ElementalBow.getAvailableSelectionViews(player, stack).stream()
                    .filter(v -> v.selection().shotMode().equals("magic")).toList();
            helper.assertTrue(views.size() == 3, "All stored spells must be selectable");
            for (var view : views) helper.assertTrue(view.overheatActive() && Mth.equal(view.overheatFillRatio(), 0.75F),
                    "All spell slots must show the same shared overheat");
        });
        helper.runAtTickTime(42, () -> {
            helper.assertFalse(ElementalBowOverheatManager.getState(player).active(), "Overheat must expire");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_empty_selection_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));

            var selectedViews = ElementalBow.getAvailableSelectionViews(player, stack);
            var spectralView = selectedViews.stream()
                    .filter(view -> "special".equals(view.selection().shotMode())
                            && ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow").equals(view.selection().selectionId()))
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
                                    && ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow").equals(view.selection().selectionId())),
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

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowMode", "fire"));

            ElementalBowScrollStorage.migrate(stack);

            assertElementalBowSelection(helper, stack, null, null,
                    "Elemental Bow should clear unresolved legacy mode values back to normal mode");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should remove its spell container after falling back to normal mode");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowUsesStoredScrollWithoutSpellContainer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, "fire");
            helper.assertFalse(ISpellContainer.isSpellContainer(stack), "Bow must not expose an imbued spell container");
            var profile = ElementalBow.getDisplayedSpellProfile(stack);
            helper.assertTrue(profile != null && profile.spell() == SpellRegistry.FIRE_ARROW_SPELL.get()
                    && profile.spellLevel() == 1, "Stored scroll must supply the selected spell");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(12, 2, 12), "elemental_bow_spell_wheel_test");
            var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
            setElementalBowMode(stack, SchoolRegistry.FIRE_RESOURCE.toString());
            ElementalBowScrollStorage.migrate(stack);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var selectionManager = new SpellSelectionManager(player);
            var mainhandSelections = selectionManager.getSpellsForSlot(SpellSelectionManager.MAINHAND);
            helper.assertTrue(mainhandSelections.isEmpty(),
                    "Elemental Bow should not add its derived spell to the mainhand spell wheel: " + mainhandSelections);
            helper.assertTrue(selectionManager.getSelection() == null,
                    "Elemental Bow should not create a selected spell from its derived container");
        });
    }

    @GameTest(template = "gametest/bow_casting")
    public static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (ElementalBow) ItemRegistry.ELEMENTAL_BOW.get();
            var stack = new ItemStack(item);
            setElementalBowShotSelection(stack, "special", ResourceLocation.fromNamespaceAndPath("minecraft", "spectral_arrow"));

            ElementalBowScrollStorage.migrate(stack);

            helper.assertFalse(ISpellContainer.isSpellContainer(stack),
                    "Elemental Bow should not expose a spell container outside magic mode");
            helper.assertTrue(ElementalBow.getDisplayedSpellProfile(stack) == null,
                    "Elemental Bow should not expose a displayed spell profile outside magic mode");
        });
    }
}
