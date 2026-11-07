package jp.aquafactory.apprenticecodex.gametest.crafting.extraction;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.gametest.crafting.support.WorkbenchMenuScope;
import jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellExtractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class SpellExtractionGameTestScenarios {
    private SpellExtractionGameTestScenarios() {
    }

    public static void spellcasterWorkbenchExtractsEligibleSingleSlotSpells(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = PlayerTestSupport.createEquipmentTestPlayer(
                        helper,
                        new BlockPos(0, 2, 0),
                        "spell_extraction_success"
                );
                var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

                var unlockedBow = createImbuedStack(Items.BOW, magicMissile, 2, false, 1);
                var unlockedMenu = menus.track(createExtractionMenu(player, unlockedBow, 2), player);
                var unlockedResult = unlockedMenu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
                assertScrollSpell(
                        helper,
                        unlockedResult,
                        magicMissile,
                        2,
                        "Unlocked extraction should preserve the spell"
                );
                helper.assertTrue(unlockedMenu.getSlot(0).getItem().isEmpty(),
                        "Unlocked extraction should consume one target item");
                helper.assertTrue(unlockedMenu.getSlot(1).getItem().getCount() == 1,
                        "Unlocked extraction should consume exactly one extraction shard");

                for (var item : new Item[]{Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD}) {
                    var lockedLootStack = createImbuedStack(item, magicMissile, 1, true, 1);
                    helper.assertTrue(lockedLootStack.is(TagRegistry.Items.SPELL_DISMANTLEABLE),
                            "Default loot weapon should be tagged as spell dismantleable: " + item);
                    var lockedMenu = menus.track(createExtractionMenu(player, lockedLootStack, 1), player);
                    var lockedResult = lockedMenu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
                    assertScrollSpell(
                            helper,
                            lockedResult,
                            magicMissile,
                            1,
                            "Tagged locked loot weapon should extract its spell"
                    );
                    helper.assertTrue(lockedMenu.getSlot(0).getItem().isEmpty()
                                    && lockedMenu.getSlot(1).getItem().isEmpty(),
                            "Locked loot extraction should consume both inputs");
                }

                var icons = menus.track(new SpellcasterWorkbenchMenu(0, player.getInventory()), player).getSelectableIcons();
                helper.assertFalse(icons.stream().anyMatch(stack -> stack.is(ItemRegistry.SPELL_EXTRACT_SHARD.get())),
                        "Spell extraction should not add a selectable Workbench recipe icon");
            }
        });
    }

    public static void spellcasterWorkbenchReportsExtractionBlockReasons(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = PlayerTestSupport.createEquipmentTestPlayer(
                        helper,
                        new BlockPos(0, 2, 0),
                        "spell_extraction_block_reasons"
                );
                var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

                var uniqueStack = createImbuedStack(ItemRegistry.ILLUMINATE_STELLAR_STAFF.get(), magicMissile, 1, false, 1);
                assertBlockReason(helper, player, uniqueStack, SpellExtractionHelper.BlockReason.UNIQUE_ITEM);

                var calibrationExtractable = createImbuedStack(ItemRegistry.MANA_FORCE_BLADE.get(), magicMissile, 1, false, 1);
                assertBlockReason(helper, player, calibrationExtractable,
                        SpellExtractionHelper.BlockReason.CALIBRATION_EXTRACTABLE);

                var untaggedLocked = createImbuedStack(Items.BOW, magicMissile, 1, true, 1);
                assertBlockReason(helper, player, untaggedLocked, SpellExtractionHelper.BlockReason.LOCKED);

                var nonLootableSpell = SpellRegistry.ANCHOR_BLINK.get();
                var nonLootableLocked = createImbuedStack(Items.IRON_SWORD, nonLootableSpell, 1, true, 1);
                assertBlockReason(helper, player, nonLootableLocked, SpellExtractionHelper.BlockReason.LOCKED);

                var multiSlot = createImbuedStack(Items.BOW, magicMissile, 1, false, 2);
                assertBlockReason(helper, player, multiSlot, SpellExtractionHelper.BlockReason.NOT_TARGET);

                var emptyContainer = new ItemStack(Items.BOW);
                ISpellContainer.set(emptyContainer, ISpellContainer.create(1, true, false));
                assertBlockReason(helper, player, emptyContainer, SpellExtractionHelper.BlockReason.NOT_TARGET);

                var ordinaryMenu = menus.track(createExtractionMenu(player, new ItemStack(Items.COBBLESTONE), 1), player);
                helper.assertTrue(ordinaryMenu.getSpellExtractionBlockReason() == null,
                        "Items without a spell container should not show an extraction error");

                var extraInputMenu = menus.track(createExtractionMenu(player, untaggedLocked.copy(), 1), player);
                extraInputMenu.getSlot(2).set(new ItemStack(Items.STICK));
                helper.assertTrue(extraInputMenu.getSpellExtractionBlockReason() == null,
                        "Extraction errors should require exactly one empty Workbench input slot");
            }
        });
    }

    public static void spellcasterWorkbenchDoesNotConsumeExtractionInputsWhenOutputCannotMove(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var menus = new WorkbenchMenuScope()) {
                var player = PlayerTestSupport.createEquipmentTestPlayer(
                        helper,
                        new BlockPos(0, 2, 0),
                        "spell_extraction_full_inventory"
                );
                for (var slot = 0; slot < player.getInventory().items.size(); ++slot) {
                    player.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
                }

                var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var target = createImbuedStack(Items.BOW, magicMissile, 1, false, 1);
                target.set(DataComponents.CUSTOM_NAME,
                        Component.literal("Preserved extraction target"));
                var menu = menus.track(createExtractionMenu(player, target, 1), player);
                var expectedTarget = target.copy();
                var expectedShard = menu.getSlot(1).getItem().copy();
                var result = menu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
                helper.assertTrue(result.isEmpty(), "Extraction should fail when the output cannot move to inventory");
                helper.assertTrue(ItemStack.matches(expectedTarget, menu.getSlot(0).getItem()),
                        "Failed output transfer should preserve the target item");
                helper.assertTrue(ItemStack.matches(expectedShard, menu.getSlot(1).getItem()),
                        "Failed output transfer should preserve the extraction shard");
            }
        });
    }

    private static ItemStack createImbuedStack(
            Item item,
            AbstractSpell spell,
            int spellLevel,
            boolean locked,
            int maxSpellCount
    ) {
        var stack = new ItemStack(item);
        var mutable = ISpellContainer.create(maxSpellCount, true, false).mutableCopy();
        if (!mutable.addSpellAtIndex(spell, spellLevel, 0, locked)) {
            throw new IllegalStateException("Failed to create imbued GameTest stack for " + item);
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        return stack;
    }

    private static SpellcasterWorkbenchMenu createExtractionMenu(
            Player player,
            ItemStack target,
            int shardCount
    ) {
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        menu.getSlot(0).set(target);
        menu.getSlot(1).set(new ItemStack(ItemRegistry.SPELL_EXTRACT_SHARD.get(), shardCount));
        return menu;
    }

    private static void assertBlockReason(
            GameTestHelper helper,
            Player player,
            ItemStack target,
            SpellExtractionHelper.BlockReason expected
    ) {
        try (var menus = new WorkbenchMenuScope()) {
            var menu = menus.track(createExtractionMenu(player, target, 1), player);
            var targetSnapshot = target.copy();
            var shardSnapshot = menu.getSlot(1).getItem().copy();
            helper.assertTrue(menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                    "Blocked extraction should not expose a result: " + expected);
            helper.assertTrue(menu.getSpellExtractionBlockReason() == expected,
                    "Unexpected extraction block reason, expected " + expected
                            + " but got " + menu.getSpellExtractionBlockReason());
            helper.assertTrue(menu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT).isEmpty(),
                    "Blocked extraction should reject actual output transfer: " + expected);
            helper.assertTrue(ItemStack.matches(targetSnapshot, menu.getSlot(0).getItem())
                            && ItemStack.matches(shardSnapshot, menu.getSlot(1).getItem()),
                    "Blocked extraction should preserve both complete inputs: " + expected);
        }
    }

    private static void assertScrollSpell(
            GameTestHelper helper,
            ItemStack scrollStack,
            AbstractSpell expectedSpell,
            int expectedLevel,
            String message
    ) {
        helper.assertTrue(scrollStack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get()),
                message + " (result is not a scroll)");
        var spellContainer = ISpellContainer.get(scrollStack);
        helper.assertTrue(spellContainer != null, message + " (scroll spell container is null)");
        var spellData = spellContainer.getSpellAtIndex(0);
        helper.assertTrue(spellData.getSpell() == expectedSpell, message + " (spell mismatch)");
        helper.assertTrue(spellData.getLevel() == expectedLevel,
                message + " (level mismatch: " + spellData.getLevel() + ")");
    }
}
