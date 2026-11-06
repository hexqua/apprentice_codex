package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import java.util.ArrayList;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.ArchivistsGrimoireServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoireMenu;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import top.theillusivec4.curios.api.SlotContext;

import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.assertCurioModifierAmount;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.assertScrollSpell;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.assertSpellData;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.TooltipTestSupport.assertTooltipTranslationKey;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.assertUpgradeable;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellcasterWorkbenchMenuWithInputs;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellcasterWorkbenchMenuWithInputs;

public final class ArchivistsGrimoireScenarios {
    private ArchivistsGrimoireScenarios() {}

    static void archivistsGrimoireInventoryKeepsOnlyScrollsAndPersists(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());

            var rejected = inventory.insertItem(0, new ItemStack(Items.DIAMOND), false);
            helper.assertTrue(!rejected.isEmpty() && rejected.is(Items.DIAMOND),
                    "Archivist's Grimoire should reject non-scroll insertion");
            helper.assertTrue(inventory.getStackInSlot(0).isEmpty(),
                    "Rejected non-scroll should not remain in Archivist's Grimoire inventory");

            inventory.setStackInSlot(5, createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get()));
            var restoredInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            assertScrollSpell(
                    helper,
                    restoredInventory.getStackInSlot(5),
                    SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Archivist's Grimoire lost persisted scroll data"
            );

            restoredInventory.setStackInSlot(6, new ItemStack(Items.DIAMOND));
            var cleanedInventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            helper.assertTrue(cleanedInventory.getStackInSlot(6).isEmpty(),
                    "Archivist's Grimoire should purge non-scroll stacks from saved inventory data");
        });
    }

    static void archivistsGrimoireSelectedRowNavigationUsesPopulatedRowsOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoireStack, 5);
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 2 + 4,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 5 + 1,
                    createSpellScroll(SpellRegistry.HEAL_SPELL.get())
            );

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 4);
            helper.assertTrue(ArchivistsGrimoire.ensureSelectedRowHasScroll(grimoireStack, helper.getLevel().registryAccess()),
                    "Archivist's Grimoire should find a populated row from an empty selected row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 2,
                    "Archivist's Grimoire should normalize empty selected row to the first populated row");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 1, helper.getLevel().registryAccess()),
                    "Archivist's Grimoire should move forward to the next populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire forward navigation skipped the wrong rows");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 1, helper.getLevel().registryAccess()),
                    "Archivist's Grimoire should wrap forward to the first populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 2,
                    "Archivist's Grimoire forward wrap did not land on the populated row");

            helper.assertTrue(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, -1, helper.getLevel().registryAccess()),
                    "Archivist's Grimoire should wrap backward to the last populated row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire backward wrap did not land on the populated row");

            helper.assertFalse(ArchivistsGrimoire.changeSelectedRowToPopulatedRow(grimoireStack, 0, helper.getLevel().registryAccess()),
                    "Archivist's Grimoire should ignore zero row delta");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 5,
                    "Archivist's Grimoire zero delta should not change the selected row");
        });
    }

    static void archivistsGrimoireVisibleSpellsExposeOnlySelectedRow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoireStack, 5);
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT + 8,
                    createSpellScroll(SpellRegistry.HEAL_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 4 + 3,
                    createSpellScroll(SpellRegistry.FIRE_BREATH_SPELL.get())
            );

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 1);
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0, helper.getLevel().registryAccess()),
                    SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    1,
                    "Archivist's Grimoire visible slot 0 mismatch"
            );
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 8, helper.getLevel().registryAccess()),
                    SpellRegistry.HEAL_SPELL.get(),
                    1,
                    "Archivist's Grimoire visible slot 8 mismatch"
            );
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 3, helper.getLevel().registryAccess()) == SpellData.EMPTY,
                    "Archivist's Grimoire should not expose spells from another row");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, -1, helper.getLevel().registryAccess()) == SpellData.EMPTY,
                    "Archivist's Grimoire should return empty for negative visible slots");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, ArchivistsGrimoire.COLUMN_COUNT, helper.getLevel().registryAccess()) == SpellData.EMPTY,
                    "Archivist's Grimoire should return empty for out-of-range visible slots");

            ArchivistsGrimoire.setSelectedRow(grimoireStack, 4);
            assertSpellData(
                    helper,
                    ArchivistsGrimoire.getVisibleSpell(grimoireStack, 3, helper.getLevel().registryAccess()),
                    SpellRegistry.FIRE_BREATH_SPELL.get(),
                    1,
                    "Archivist's Grimoire should expose the newly selected row"
            );
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0, helper.getLevel().registryAccess()) == SpellData.EMPTY,
                    "Archivist's Grimoire should hide previous row spells after row change");
        });
    }

    static void archivistsGrimoireLockedRowsHideLegacyScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 2,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            ArchivistsGrimoire.setSelectedRow(grimoireStack, 2);

            helper.assertTrue(ArchivistsGrimoire.getUnlockedRowCount(grimoireStack) == 1,
                    "NBT-less Archivist's Grimoire should start with one unlocked row");
            helper.assertTrue(ArchivistsGrimoire.getSelectedRow(grimoireStack) == 0,
                    "Locked selected rows should normalize into the unlocked row range");
            helper.assertTrue(ArchivistsGrimoire.getVisibleSpell(grimoireStack, 0, helper.getLevel().registryAccess()) == SpellData.EMPTY,
                    "Locked-row legacy scrolls must not be exposed as usable spells");
            helper.assertFalse(ArchivistsGrimoire.ensureSelectedRowHasScroll(grimoireStack, helper.getLevel().registryAccess()),
                    "Locked-row legacy scrolls must not make the selected row usable");
            helper.assertTrue(ArchivistsGrimoire.hasScrollInLockedSlot(grimoireStack, helper.getLevel().registryAccess()),
                    "Locked-row legacy scroll should be detectable for warnings");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_locked_slot_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, grimoireStack);
            var menu = new ArchivistsGrimoireMenu(
                    0,
                    player.getInventory(),
                    InteractionHand.MAIN_HAND
            );
            var lockedSlot = menu.getSlot(ArchivistsGrimoire.COLUMN_COUNT * 2);
            helper.assertFalse(lockedSlot.mayPlace(createSpellScroll(SpellRegistry.HEAL_SPELL.get())),
                    "Locked Archivist's Grimoire slots should reject new scroll insertion");
            helper.assertTrue(lockedSlot.mayPickup(player),
                    "Locked Archivist's Grimoire slots should still allow legacy scroll extraction");
        });
    }

    static void archivistsGrimoireTooltipShowsInscribeHintOnlyWhenEmpty(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, true,
                    "Empty Archivist's Grimoire should show the special inscription hint");

            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get()));
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, false,
                    "Archivist's Grimoire with a stored spell should hide the special inscription hint");

            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2, ItemStack.EMPTY);
            assertArchivistsGrimoireInscribeHintTooltip(helper, grimoireStack, true,
                    "Archivist's Grimoire should show the special inscription hint again after becoming empty");
        });
    }

    static void archivistsGrimoireTooltipWarnsAboutLockedScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT + 2,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );

            assertTooltipTranslationKey(
                    helper,
                    grimoireStack,
                    "item.apprenticecodex.archivists_grimoire.tooltip.warning_legacy_slot",
                    true,
                    "Archivist's Grimoire should warn when a locked slot stores a scroll"
            );

            inventory.setStackInSlot(ArchivistsGrimoire.COLUMN_COUNT + 2, ItemStack.EMPTY);
            assertTooltipTranslationKey(
                    helper,
                    grimoireStack,
                    "item.apprenticecodex.archivists_grimoire.tooltip.warning_legacy_slot",
                    false,
                    "Archivist's Grimoire should remove the locked-slot warning after the scroll is removed"
            );
        });
    }

    static void archivistsGrimoireWorkbenchUpgradePreservesStoredScrolls(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_upgrade_test");
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );

            var menu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    grimoireStack,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
            );
            var result = menu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
            helper.assertFalse(result.isEmpty(), "Archivist's Grimoire row upgrade should produce a result");
            helper.assertTrue(ArchivistsGrimoire.getUpgradeCount(result) == 1,
                    "Archivist's Grimoire row upgrade should increment the upgrade count");
            helper.assertTrue(ArchivistsGrimoire.getUnlockedRowCount(result) == 2,
                    "Archivist's Grimoire row upgrade should unlock exactly one additional row");
            assertScrollSpell(
                    helper,
                    new ArchivistsGrimoire.ScrollInventory(result, helper.getLevel().registryAccess()).getStackInSlot(ArchivistsGrimoire.COLUMN_COUNT),
                    SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Archivist's Grimoire row upgrade should preserve stored scrolls"
            );
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty()
                            && menu.getSlot(1).getItem().isEmpty()
                            && menu.getSlot(2).getItem().isEmpty(),
                    "Archivist's Grimoire row upgrade should consume all three inputs");

            var maxedGrimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(maxedGrimoireStack, 5);
            var maxedMenu = createSpellcasterWorkbenchMenuWithInputs(
                    player,
                    maxedGrimoireStack,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get()),
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
            );
            helper.assertTrue(maxedMenu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Maxed Archivist's Grimoire should not expose another upgrade result");
            helper.assertTrue(maxedMenu.isBlockedByArchivistsGrimoireMaxSlotReached(),
                    "Maxed Archivist's Grimoire should expose a max-slot warning state");
        });
    }

    static void archivistsGrimoireConfigMaxRowsCannotFallBelowInitialRows(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useArchivistsGrimoireConfigOverrideForGameTest(
                    new ArchivistsGrimoireServerConfig.Values(4, 2)
            )) {
                var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
                helper.assertTrue(ApprenticeCodexServerConfig.archivistsGrimoireInitialRows() == 4,
                        "Archivist's Grimoire initialRows override mismatch");
                helper.assertTrue(ApprenticeCodexServerConfig.archivistsGrimoireEffectiveMaxRows() == 4,
                        "Archivist's Grimoire maxRows lower than initialRows should resolve to initialRows");
                helper.assertTrue(ArchivistsGrimoire.getUnlockedRowCount(grimoireStack) == 4,
                        "Archivist's Grimoire should use the resolved row bounds");
                helper.assertFalse(ArchivistsGrimoire.canUpgrade(grimoireStack),
                        "Archivist's Grimoire should not upgrade when resolved maxRows equals initialRows");
            }
        });
    }

    static void archivistsGrimoireCurioAndUpgradeContractsStayRegistered(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            var spellbookTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("curios", Curios.SPELLBOOK_SLOT)
            );
            helper.assertTrue(grimoireStack.is(spellbookTag),
                    "Archivist's Grimoire should be tagged for the Curios spellbook slot");
            assertUpgradeable(helper, grimoireStack,
                    "Archivist's Grimoire should remain upgradeable via explicit whitelist entry");

            var item = (ArchivistsGrimoire) ItemRegistry.ARCHIVISTS_GRIMOIRE.get();
            var slotContext = new SlotContext(
                    Curios.SPELLBOOK_SLOT,
                    helper.spawn(EntityType.PIG, new BlockPos(0, 2, 0)),
                    0,
                    false,
                    true
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    slotContext,
                    grimoireStack,
                    AttributeRegistry.MAX_MANA,
                    200.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Archivist's Grimoire spellbook-slot max mana bonus regression"
            );

            var wrongSlotContext = new SlotContext(
                    CuriosSlotConstants.CHARM,
                    helper.spawn(EntityType.PIG, new BlockPos(0, 2, 1)),
                    0,
                    false,
                    true
            );
            assertCurioModifierAmount(
                    helper,
                    item,
                    wrongSlotContext,
                    grimoireStack,
                    AttributeRegistry.MAX_MANA,
                    0.0D,
                    AttributeModifier.Operation.ADD_VALUE,
                    "Archivist's Grimoire should not add spellbook max mana outside the spellbook slot"
            );
        });
    }

    static void archivistsGrimoireSpellSelectionManagerReadsVisibleRow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var grimoireStack = new ItemStack(ItemRegistry.ARCHIVISTS_GRIMOIRE.get());
            ArchivistsGrimoire.setUpgradeCount(grimoireStack, 5);
            var inventory = new ArchivistsGrimoire.ScrollInventory(grimoireStack, helper.getLevel().registryAccess());
            inventory.setStackInSlot(0, createSpellScroll(SpellRegistry.HEAL_SPELL.get()));
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 3 + 1,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            inventory.setStackInSlot(
                    ArchivistsGrimoire.COLUMN_COUNT * 3 + 4,
                    createSpellScroll(SpellRegistry.FIRE_BREATH_SPELL.get())
            );
            ArchivistsGrimoire.setSelectedRow(grimoireStack, 3);

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "archivists_grimoire_selection_test");
            equipCurio(player, Curios.SPELLBOOK_SLOT, grimoireStack);

            var manager = new SpellSelectionManager(player);
            var spellbookOptions = manager.getSpellsForSlot(Curios.SPELLBOOK_SLOT);
            helper.assertTrue(spellbookOptions.size() == 2,
                    "Archivist's Grimoire should expose exactly the two spells in the selected row but got " + spellbookOptions.size());
            helper.assertTrue(spellbookOptions.stream().anyMatch(option ->
                            option.slotIndex == 1
                                    && option.spellData.getSpell() == SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                    "Archivist's Grimoire spell selection missing selected-row Magic Missile at visible slot 1");
            helper.assertTrue(spellbookOptions.stream().anyMatch(option ->
                            option.slotIndex == 4
                                    && option.spellData.getSpell() == SpellRegistry.FIRE_BREATH_SPELL.get()),
                    "Archivist's Grimoire spell selection missing selected-row Fire Breath at visible slot 4");
            helper.assertFalse(spellbookOptions.stream().anyMatch(option ->
                            option.spellData.getSpell() == SpellRegistry.HEAL_SPELL.get()),
                    "Archivist's Grimoire spell selection should not expose spells outside the selected row");
        });
    }

    public static BlockDropsEvent createBlockDropsExperienceEvent(ServerLevel level, BlockPos pos, BlockState state, Player breaker, ItemStack tool, int droppedExperience) {
        var event = new BlockDropsEvent(level, pos, state, null, new ArrayList<>(), breaker, tool);
        event.setDroppedExperience(droppedExperience);
        return event;
    }

    static void assertArchivistsGrimoireInscribeHintTooltip(
            GameTestHelper helper,
            ItemStack stack,
            boolean expected,
            String message
    ) {
        assertTooltipTranslationKey(helper, stack, "item.apprenticecodex.special_spellbook.inscribe_hint", expected, message);
    }
}
