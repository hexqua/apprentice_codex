package jp.aquafactory.apprenticecodex.gametest.equipment.throwablecard;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jp.aquafactory.apprenticecodex.block.spellcasterworkbench.SpellcasterWorkbenchMenu;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfile;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserSpellProfileManager;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellThrowableCardServerConfig;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.assertStackHasSpell;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.remotePlayerGeometryProfile;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.requireSpellId;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createSpellcasterWorkbenchMenuWithInputs;

final class ThrowableCardScenarios {
    private ThrowableCardScenarios() {}

    static void spellThrowableCardsAcceptOnlySupportedImpactProfilesAndAllowedRecasts(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var invokeCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_INVOKE_CARD.get();
            var autonomyCard = (AbstractSpellThrowableCardItem) ItemRegistry.SPELL_AUTONOMY_CARD.get();
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            var raiseDead = io.redspace.ironsspellbooks.api.registry.SpellRegistry.RAISE_DEAD_SPELL.get();

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(mageLight), remotePlayerGeometryProfile(false),
                    requireSpellId(raiseDead), remotePlayerGeometryProfile(false)
            ))) {
                helper.assertTrue(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should accept RemoteOwner profile spells");
                helper.assertTrue(autonomyCard.canImbueSpell(mageLight, 1),
                        "Spell Autonomy Card should accept RemoteOwner profile spells");
                helper.assertFalse(invokeCard.canImbueSpell(raiseDead, 1),
                        "Spell Invoke Card should reject recast spells when the profile does not allow initial recast");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(raiseDead), remotePlayerGeometryProfile(true)
            ))) {
                helper.assertTrue(invokeCard.canImbueSpell(raiseDead, 1),
                        "Spell Invoke Card should accept summon recasts controlled by SummonedEntitiesCastData");
                helper.assertTrue(autonomyCard.canImbueSpell(raiseDead, 1),
                        "Spell Autonomy Card should accept summon recasts controlled by SummonedEntitiesCastData");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of())) {
                helper.assertFalse(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should reject spells without a supported impact profile");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(mageLight), remotePlayerGeometryProfile(false)
            )); var ignoredConfig = ApprenticeCodexServerConfig.useRemoteOwnerCastConfigOverrideForGameTest(
                    true,
                    List.of(mageLight.getSpellResource().toString())
            )) {
                helper.assertTrue(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should ignore runtime RemoteOwner denylist during Imbue");
                helper.assertTrue(autonomyCard.canImbueSpell(mageLight, 1),
                        "Spell Autonomy Card should ignore runtime RemoteOwner denylist during Imbue");
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of());
                 var ignoredDispenserProfiles = SpellDispenserSpellProfileManager.useProfilesForGameTest(Map.of(
                         requireSpellId(mageLight), SpellDispenserSpellProfile.DEFAULT
                 ))) {
                helper.assertFalse(invokeCard.canImbueSpell(mageLight, 1),
                        "Spell Invoke Card should not use Spell Dispenser profiles as fallback");
                helper.assertFalse(autonomyCard.canImbueSpell(mageLight, 1),
                        "Spell Autonomy Card should not use Spell Dispenser profiles as fallback");
            }
        });
    }

    static void spellThrowableCardWorkbenchRecipesImbueFromScrollWithoutConsumingScroll(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mageLight = SpellRegistry.MAGE_LIGHT.get();
            helper.assertTrue(new ItemStack(Items.PAPER).is(TagRegistry.Items.SPELL_THROWABLE_CARD_PAPERS),
                    "Paper should be registered as a Spell Throwable Card paper ingredient");
            helper.assertTrue(new ItemStack(Items.BLACK_DYE).is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                    "Black dye should be registered as a Spell Invoke Card material");
            helper.assertTrue(new ItemStack(Items.INK_SAC).is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                    "Ink sac should be registered as a Spell Invoke Card material");
            helper.assertTrue(new ItemStack(Items.GLOW_INK_SAC).is(TagRegistry.Items.SPELL_INVOKE_CARD_CRAFTING_MATERIALS),
                    "Glow ink sac should be registered as a Spell Invoke Card material");
            helper.assertTrue(new ItemStack(Items.ENDER_EYE).is(TagRegistry.Items.SPELL_AUTONOMY_CARD_CRAFTING_MATERIALS),
                    "Eye of ender should be registered as a Spell Autonomy Card material");

            try (var ignored = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                    requireSpellId(mageLight), remotePlayerGeometryProfile(false)
            ))) {
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.BLACK_DYE),
                        16,
                        mageLight,
                        "Spell Invoke Card paper recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.INK_SAC),
                        16,
                        mageLight,
                        "Spell Invoke Card ink sac recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.GLOW_INK_SAC),
                        16,
                        mageLight,
                        "Spell Invoke Card glow ink sac recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get(), 16),
                        new ItemStack(Items.BLACK_DYE),
                        16,
                        mageLight,
                        "Spell Invoke Card rewrite recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        new ItemStack(Items.PAPER, 8),
                        new ItemStack(Items.ENDER_EYE),
                        8,
                        mageLight,
                        "Spell Autonomy Card paper recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        new ItemStack(ItemRegistry.SPELL_AUTONOMY_CARD.get(), 8),
                        new ItemStack(Items.ENDER_EYE),
                        8,
                        mageLight,
                        "Spell Autonomy Card rewrite recipe"
                );
                assertSpellThrowableCardWorkbenchButtonAcceptsSplitStacks(helper, mageLight);
                assertSpellThrowableCardWorkbenchButtonAppendsToActiveDynamicRecipe(helper, mageLight);
            }

            try (var configOverride = ApprenticeCodexServerConfig.useSpellThrowableCardConfigOverrideForGameTest(
                    new SpellThrowableCardServerConfig.Values(5, 3));
                 var ignored = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of(
                         requireSpellId(mageLight), remotePlayerGeometryProfile(false)
                 ))) {
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_INVOKE_CARD.get(),
                        new ItemStack(Items.PAPER, 5),
                        new ItemStack(Items.BLACK_DYE),
                        5,
                        mageLight,
                        "Spell Invoke Card configured count recipe"
                );
                assertSpellThrowableCardWorkbenchRecipe(
                        helper,
                        ItemRegistry.SPELL_AUTONOMY_CARD.get(),
                        new ItemStack(Items.PAPER, 3),
                        new ItemStack(Items.ENDER_EYE),
                        3,
                        mageLight,
                        "Spell Autonomy Card configured count recipe"
                );
            }

            try (var ignoredProfiles = RemoteOwnerCastProfileManager.useProfilesForGameTest(Map.of())) {
                assertSpellThrowableCardWorkbenchCantImbue(
                        helper,
                        new ItemStack(Items.PAPER, 16),
                        new ItemStack(Items.BLACK_DYE),
                        createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get()),
                        "Spell Invoke Card should show a blocked result for unsupported scrolls"
                );
            }
        });
    }

    static void assertSpellThrowableCardWorkbenchRecipe(
            GameTestHelper helper,
            Item resultItem,
            ItemStack baseStack,
            ItemStack catalystStack,
            int expectedCount,
            AbstractSpell spell,
            String context
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), context.toLowerCase(Locale.ROOT).replace(' ', '_'));
        var scrollStack = createSpellScroll(spell);
        var menu = createSpellcasterWorkbenchMenuWithInputs(player, baseStack.copy(), catalystStack.copy(), scrollStack.copy());

        var preview = menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(preview.is(resultItem) && preview.getCount() == expectedCount,
                context + " should preview the expected card stack: " + preview);
        assertStackHasSpell(helper, preview, spell, 1, context + " preview should be imbued");

        var crafted = menu.quickMoveStack(player, SpellcasterWorkbenchMenu.RESULT_SLOT);
        helper.assertTrue(crafted.is(resultItem) && crafted.getCount() == expectedCount,
                context + " should craft the expected card stack: " + crafted);
        assertStackHasSpell(helper, crafted, spell, 1, context + " result should be imbued");
        helper.assertTrue(countInputItem(menu, baseStack.getItem()) == 0,
                context + " should consume the base stack");
        helper.assertTrue(countInputItem(menu, catalystStack.getItem()) == 0,
                context + " should consume the catalyst");
        helper.assertTrue(hasMatchingInputStack(menu, scrollStack),
                context + " should leave the scroll in the Workbench inputs");
    }

    static void assertSpellThrowableCardWorkbenchButtonAcceptsSplitStacks(GameTestHelper helper, AbstractSpell spell) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_invoke_card_split_stack_button");
        var menu = new SpellcasterWorkbenchMenu(0, player.getInventory());
        player.getInventory().setItem(0, new ItemStack(Items.PAPER, 8));
        player.getInventory().setItem(1, new ItemStack(Items.PAPER, 8));
        player.getInventory().setItem(2, new ItemStack(Items.BLACK_DYE));
        player.getInventory().setItem(3, createSpellScroll(spell));

        helper.assertTrue(menu.clickMenuButton(player, findSelectableIconIndex(helper, menu, ItemRegistry.SPELL_INVOKE_CARD.get())),
                "Spell Invoke Card button should accept split paper stacks");
        var preview = menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem();
        helper.assertTrue(preview.is(ItemRegistry.SPELL_INVOKE_CARD.get()) && preview.getCount() == 16,
                "Spell Invoke Card button should preview from split paper stacks: " + preview);
        helper.assertTrue(countInputItem(menu, Items.PAPER) == 16,
                "Spell Invoke Card button should move both split paper stacks into inputs");
    }

    static void assertSpellThrowableCardWorkbenchButtonAppendsToActiveDynamicRecipe(GameTestHelper helper, AbstractSpell spell) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_invoke_card_append_button");
        var scrollStack = createSpellScroll(spell);
        var menu = createSpellcasterWorkbenchMenuWithInputs(
                player,
                new ItemStack(Items.PAPER, 16),
                new ItemStack(Items.BLACK_DYE),
                scrollStack.copy()
        );
        player.getInventory().setItem(0, new ItemStack(Items.PAPER, 16));
        player.getInventory().setItem(1, new ItemStack(Items.BLACK_DYE));

        helper.assertTrue(menu.clickMenuButton(player, findSelectableIconIndex(helper, menu, ItemRegistry.SPELL_INVOKE_CARD.get())),
                "Spell Invoke Card button should append to the active dynamic recipe");
        helper.assertTrue(countInputItem(menu, Items.PAPER) == 32,
                "Spell Invoke Card button should append one paper batch without returning existing inputs");
        helper.assertTrue(countInputItem(menu, Items.BLACK_DYE) == 2,
                "Spell Invoke Card button should append one catalyst batch without returning existing inputs");
        helper.assertTrue(hasMatchingInputStack(menu, scrollStack),
                "Spell Invoke Card button should keep the existing scroll input while appending");
    }

    static int findSelectableIconIndex(GameTestHelper helper, SpellcasterWorkbenchMenu menu, Item item) {
        var icons = menu.getSelectableIcons();
        for (var index = 0; index < icons.size(); ++index) {
            if (icons.get(index).is(item)) {
                return index;
            }
        }
        helper.fail("Missing Spellcaster Workbench selectable icon for " + item);
        return -1;
    }

    static void assertSpellThrowableCardWorkbenchCantImbue(
            GameTestHelper helper,
            ItemStack baseStack,
            ItemStack catalystStack,
            ItemStack scrollStack,
            String context
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), context.toLowerCase(Locale.ROOT).replace(' ', '_'));
        var menu = createSpellcasterWorkbenchMenuWithInputs(player, baseStack, catalystStack, scrollStack);
        helper.assertTrue(menu.getSlot(SpellcasterWorkbenchMenu.RESULT_SLOT).getItem().isEmpty(),
                context + ": result slot should stay empty");
        helper.assertTrue(menu.isBlockedBySpellThrowableCardCantImbue(),
                context + ": menu should expose the card imbue block reason");
    }

    static int countInputItem(SpellcasterWorkbenchMenu menu, Item item) {
        var count = 0;
        for (var slotIndex = 0; slotIndex < SpellcasterWorkbenchMenu.INPUT_SLOT_COUNT; ++slotIndex) {
            var stack = menu.getSlot(slotIndex).getItem();
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    static boolean hasMatchingInputStack(SpellcasterWorkbenchMenu menu, ItemStack expectedStack) {
        for (var slotIndex = 0; slotIndex < SpellcasterWorkbenchMenu.INPUT_SLOT_COUNT; ++slotIndex) {
            if (ItemStack.isSameItemSameComponents(menu.getSlot(slotIndex).getItem(), expectedStack)) {
                return true;
            }
        }
        return false;
    }
}
