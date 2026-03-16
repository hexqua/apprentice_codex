package jp.aquafactory.apprenticecodex.recipe.smithing;

import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class SpellbookCarryoverSmithingRecipe implements SmithingRecipe {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public SpellbookCarryoverSmithingRecipe(
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            ItemStack result
    ) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = sanitizeResult(result);
    }

    @Override
    public boolean matches(SmithingRecipeInput input, @NotNull Level level) {
        return template.test(input.template())
                && base.test(input.base())
                && addition.test(input.addition());
    }

    @Override
    public @NotNull ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        var baseStack = input.base();
        if (baseStack.isEmpty()) {
            return result.copy();
        }

        var resultStack = baseStack.transmuteCopy(result.getItem(), result.getCount());
        backfillMissingDefaultComponents(resultStack, result);
        return resultStack;
    }

    /**
     * ベース本の spell container を優先しつつ、結果アイテム固有の初期 component だけ補う。
     */
    private static void backfillMissingDefaultComponents(ItemStack resultStack, ItemStack defaultResultStack) {
        var builder = DataComponentPatch.builder();
        boolean hasPatch = false;

        for (var component : defaultResultStack.getComponents()) {
            if (resultStack.getComponents().has(component.type())) {
                continue;
            }

            builder.set(component);
            hasPatch = true;
        }

        if (hasPatch) {
            resultStack.applyComponents(builder.build());
        }
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean isIncomplete() {
        return template.hasNoItems() || base.hasNoItems() || addition.hasNoItems();
    }

    public @NotNull Ingredient getTemplate() {
        return template;
    }

    public @NotNull Ingredient getBase() {
        return base;
    }

    public @NotNull Ingredient getAddition() {
        return addition;
    }

    public @NotNull ItemStack getResultTemplate() {
        return result.copy();
    }

    private static ItemStack sanitizeResult(ItemStack result) {
        if (result.isEmpty() || result.getCount() <= 0) {
            throw new IllegalArgumentException("Spellbook carryover smithing recipe requires a non-empty result.");
        }
        return result.copy();
    }
}
