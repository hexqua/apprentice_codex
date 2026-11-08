package jp.aquafactory.apprenticecodex.gametest.equipment.spellbook;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.FireResistantUniqueSpellBook;
import jp.aquafactory.apprenticecodex.recipe.crafting.ExplorersCodexGuidebookTransferRecipe;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import top.theillusivec4.curios.api.SlotContext;

import static jp.aquafactory.apprenticecodex.gametest.equipment.autocastamulet.AutocastAmuletTestSupport.fillSpellContainerToActiveCount;
import static jp.aquafactory.apprenticecodex.gametest.support.InventoryTestSupport.createCraftingInput;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.assertSpellData;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.createUpgradeData;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createInitializedPresetStack;

final class GuidebookScenarios {
    private GuidebookScenarios() {}

    static void isekaiTravelGuidebookStartsWithTwoFixedSpellsAndNoAttributes(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var item = (FireResistantUniqueSpellBook) stack.getItem();
            item.initializeSpellContainer(stack);

            helper.assertTrue(ISpellContainer.isSpellContainer(stack),
                    "Isekai Travel Guidebook did not initialize a spell container");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Isekai Travel Guidebook spell container is null");
            helper.assertTrue(spellContainer.getMaxSpellCount() == 2,
                    "Isekai Travel Guidebook spell slot count mismatch: " + spellContainer.getMaxSpellCount());

            var firstSpell = spellContainer.getSpellAtIndex(0);
            var secondSpell = spellContainer.getSpellAtIndex(1);
            helper.assertTrue(firstSpell != SpellData.EMPTY,
                    "Isekai Travel Guidebook first spell is empty");
            helper.assertTrue(secondSpell != SpellData.EMPTY,
                    "Isekai Travel Guidebook second spell is empty");
            helper.assertTrue(firstSpell.getSpell() == SpellRegistry.HEALING_BLOOM.get(),
                    "Isekai Travel Guidebook first spell mismatch: " + firstSpell.getSpell().getSpellResource());
            helper.assertTrue(secondSpell.getSpell() == SpellRegistry.COMPANION_TRUNK.get(),
                    "Isekai Travel Guidebook second spell mismatch: " + secondSpell.getSpell().getSpellResource());

            var pig = helper.spawn(EntityType.PIG, new BlockPos(0, 2, 0));
            var slotContext = new SlotContext(
                    Curios.SPELLBOOK_SLOT,
                    pig,
                    0,
                    false,
                    true
            );
            var modifiers = item.getAttributeModifiers(
                    slotContext,
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/isekai_travel_guidebook"),
                    stack
            );
            helper.assertTrue(modifiers.isEmpty(),
                    "Isekai Travel Guidebook should not add spellbook attributes: " + modifiers);
        });
    }

    static void explorersCodexGuidebookTransferRecipeMovesFixedSpellsAndKeepsExplorersData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            explorersCodexStack.set(DataComponents.CUSTOM_NAME, Component.literal("Codex transfer check"));
            explorersCodexStack.set(DataComponents.REPAIR_COST, 7);
            var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.UNBREAKING);
            var expectedUpgradeData = createUpgradeData(
                    helper.getLevel().registryAccess(),
                    explorersCodexStack,
                    UpgradeOrbTypeRegistry.MANA,
                    EquipmentSlot.OFFHAND.getName()
            );
            var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(unbreaking, 1);
            EnchantmentHelper.setEnchantments(explorersCodexStack, enchantments.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingInput = createCraftingInput(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingInput, helper.getLevel()),
                    "Explorer's Codex + Isekai Travel Guidebook should match the transfer recipe");

            var result = recipe.assemble(craftingInput, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(ItemRegistry.EXPLORERS_CODEX.get()),
                    "Transfer recipe should return Explorer's Codex but got " + BuiltInRegistries.ITEM.getKey(result.getItem()));
            helper.assertTrue("Codex transfer check".equals(result.getHoverName().getString()),
                    "Explorer's Codex custom name was not preserved: " + result.getHoverName().getString());
            helper.assertTrue(result.getOrDefault(DataComponents.REPAIR_COST, 0) == 7,
                    "Explorer's Codex repair cost was not preserved: " + result.getOrDefault(DataComponents.REPAIR_COST, 0));
            helper.assertTrue(UpgradeData.getUpgradeData(result).equals(expectedUpgradeData),
                    "Explorer's Codex upgrade data was not preserved: " + UpgradeData.getUpgradeData(result));
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(unbreaking, result) == 1,
                    "Explorer's Codex enchantments were not preserved");

            var resultSpellContainer = ISpellContainer.get(result);
            helper.assertTrue(resultSpellContainer != null, "Transferred Explorer's Codex lost its spell container");
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getMaxSpellCount() == 6,
                    "Transferred Explorer's Codex slot count mismatch: " + (resultSpellContainer == null ? -1 : resultSpellContainer.getMaxSpellCount()));
            assertSpellData(helper, resultSpellContainer, 0, SpellRegistry.ASSIST_WINGS.get(), 1, true, "Transferred Explorer's Codex first spell mismatch");
            assertSpellData(helper, resultSpellContainer, 1, SpellRegistry.TERRA_RESONANCE.get(), 1, true, "Transferred Explorer's Codex second spell mismatch");
            assertSpellData(helper, resultSpellContainer, 2, SpellRegistry.SENSE_EVIL.get(), 1, true, "Transferred Explorer's Codex third spell mismatch");
            assertSpellData(helper, resultSpellContainer, 3, SpellRegistry.REMOTE_EYE.get(), 1, true, "Transferred Explorer's Codex fourth spell mismatch");
            assertSpellData(helper, resultSpellContainer, 4, SpellRegistry.HEALING_BLOOM.get(), 1, true, "Transferred Explorer's Codex transferred Healing Bloom mismatch");
            assertSpellData(helper, resultSpellContainer, 5, SpellRegistry.COMPANION_TRUNK.get(), 1, true, "Transferred Explorer's Codex transferred Companion Trunk mismatch");
        });
    }

    static void explorersCodexGuidebookTransferRecipeIgnoresDuplicateGuidebookSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(5);
            helper.assertTrue(mutable.addSpell(SpellRegistry.HEALING_BLOOM.get(), 1, true),
                    "Failed to prepare duplicate Healing Bloom on Explorer's Codex");
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingInput = createCraftingInput(explorersCodexStack, guidebookStack);

            helper.assertTrue(recipe.matches(craftingInput, helper.getLevel()),
                    "Recipe should still match when one guidebook spell is already present");

            var result = recipe.assemble(craftingInput, helper.getLevel().registryAccess());
            var resultSpellContainer = ISpellContainer.get(result);
            helper.assertTrue(resultSpellContainer != null, "Transferred Explorer's Codex lost its spell container");
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getMaxSpellCount() == 6,
                    "Duplicate-ignore result slot count mismatch: " + (resultSpellContainer == null ? -1 : resultSpellContainer.getMaxSpellCount()));
            helper.assertTrue(resultSpellContainer != null && resultSpellContainer.getIndexForSpell(SpellRegistry.HEALING_BLOOM.get()) == 4,
                    "Healing Bloom should remain single and keep its prepared slot");
            assertSpellData(helper, resultSpellContainer, 5, SpellRegistry.COMPANION_TRUNK.get(), 1, true,
                    "Companion Trunk should still be transferred when Healing Bloom is already present");
        });
    }

    static void explorersCodexGuidebookTransferRecipeRejectsSpellSlotOverflow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var recipe = getExplorersCodexGuidebookTransferRecipe(helper);
            var explorersCodexStack = createInitializedPresetStack(ItemRegistry.EXPLORERS_CODEX.get());
            var mutable = ISpellContainer.get(explorersCodexStack).mutableCopy();
            mutable.setMaxSpellCount(14);
            fillSpellContainerToActiveCount(helper, mutable, 14);
            ISpellContainer.set(explorersCodexStack, mutable.toImmutable());

            var guidebookStack = createInitializedPresetStack(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get());
            var craftingInput = createCraftingInput(explorersCodexStack, guidebookStack);

            helper.assertFalse(recipe.matches(craftingInput, helper.getLevel()),
                    "Recipe should reject Explorer's Codex when transferred spells would exceed 15 slots");
            helper.assertTrue(recipe.assemble(craftingInput, helper.getLevel().registryAccess()).isEmpty(),
                    "Overflow recipe assembly should return empty result");
        });
    }

    static ExplorersCodexGuidebookTransferRecipe getExplorersCodexGuidebookTransferRecipe(GameTestHelper helper) {
        var recipeHolder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "explorers_codex_guidebook_transfer"))
                .orElse(null);
        var recipe = recipeHolder == null ? null : recipeHolder.value();
        helper.assertTrue(recipe instanceof ExplorersCodexGuidebookTransferRecipe,
                "Missing Explorer's Codex guidebook transfer recipe: " + recipe);
        return (ExplorersCodexGuidebookTransferRecipe) recipe;
    }
}
