package jp.aquafactory.apprenticecodex.recipe.crafting;

import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ExplorersCodexGuidebookTransferRecipe extends CustomRecipe {
    private static final int MAX_RESULT_SPELL_SLOTS = 15;

    public ExplorersCodexGuidebookTransferRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
        super(recipeId, category);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        return findMatch(container) != null;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        var match = findMatch(container);
        if (match == null) {
            return ItemStack.EMPTY;
        }

        var result = match.explorersCodexStack().copy();
        var spellContainer = ISpellContainer.get(result);
        if (spellContainer == null) {
            return ItemStack.EMPTY;
        }

        var mutable = spellContainer.mutableCopy();
        if (mutable.getMaxSpellCount() < match.requiredMaxSpellCount()) {
            // 初期 4 枠の写本でも序盤レシピとして成立させるため、移す固定魔法のぶんだけ最小限で枠を広げる。
            mutable.setMaxSpellCount(match.requiredMaxSpellCount());
        }

        for (var spellData : match.spellsToTransfer()) {
            var targetIndex = mutable.getNextAvailableIndex();
            if (targetIndex < 0 || !mutable.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), targetIndex, spellData.isLocked())) {
                return ItemStack.EMPTY;
            }
        }

        ISpellContainer.set(result, mutable.toImmutable());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.EXPLORERS_CODEX_GUIDEBOOK_TRANSFER_SERIALIZER.get();
    }

    private static @Nullable Match findMatch(CraftingContainer container) {
        ItemStack explorersCodexStack = ItemStack.EMPTY;
        ItemStack guidebookStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            var stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(ItemRegistry.EXPLORERS_CODEX.get())) {
                if (!explorersCodexStack.isEmpty()) {
                    return null;
                }

                explorersCodexStack = stack;
                continue;
            }

            if (stack.is(ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get())) {
                if (!guidebookStack.isEmpty()) {
                    return null;
                }

                guidebookStack = stack;
                continue;
            }

            return null;
        }

        if (explorersCodexStack.isEmpty() || guidebookStack.isEmpty()) {
            return null;
        }

        return buildMatch(explorersCodexStack, guidebookStack);
    }

    private static @Nullable Match buildMatch(ItemStack explorersCodexInput, ItemStack guidebookInput) {
        var explorersCodexStack = createInitializedCopy(explorersCodexInput);
        var guidebookStack = createInitializedCopy(guidebookInput);
        var explorersSpellContainer = ISpellContainer.get(explorersCodexStack);
        var guidebookSpellContainer = ISpellContainer.get(guidebookStack);
        if (explorersSpellContainer == null || guidebookSpellContainer == null) {
            return null;
        }
        if (explorersSpellContainer.getMaxSpellCount() > MAX_RESULT_SPELL_SLOTS) {
            return null;
        }

        var spellsToTransfer = collectTransferSpells(explorersSpellContainer, guidebookSpellContainer);
        if (spellsToTransfer.isEmpty()) {
            return null;
        }

        var requiredMaxSpellCount = explorersSpellContainer.getActiveSpellCount() + spellsToTransfer.size();
        if (requiredMaxSpellCount > MAX_RESULT_SPELL_SLOTS) {
            return null;
        }

        return new Match(explorersCodexStack, List.copyOf(spellsToTransfer), requiredMaxSpellCount);
    }

    private static ItemStack createInitializedCopy(ItemStack stack) {
        var copy = stack.copy();
        if (!ISpellContainer.isSpellContainer(copy) && copy.getItem() instanceof IPresetSpellContainer presetSpellContainer) {
            presetSpellContainer.initializeSpellContainer(copy);
        }

        return copy;
    }

    private static List<SpellData> collectTransferSpells(
            ISpellContainer explorersSpellContainer,
            ISpellContainer guidebookSpellContainer
    ) {
        var spellsToTransfer = new ArrayList<SpellData>();

        for (int i = 0; i < guidebookSpellContainer.getMaxSpellCount(); ++i) {
            var spellData = guidebookSpellContainer.getSpellAtIndex(i);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }
            if (explorersSpellContainer.getIndexForSpell(spellData.getSpell()) >= 0) {
                continue;
            }

            spellsToTransfer.add(spellData);
        }

        return spellsToTransfer;
    }

    private record Match(ItemStack explorersCodexStack, List<SpellData> spellsToTransfer, int requiredMaxSpellCount) {
    }
}
