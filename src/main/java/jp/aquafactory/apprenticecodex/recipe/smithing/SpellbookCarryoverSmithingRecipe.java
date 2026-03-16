package jp.aquafactory.apprenticecodex.recipe.smithing;

import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public final class SpellbookCarryoverSmithingRecipe implements SmithingRecipe {
    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public SpellbookCarryoverSmithingRecipe(
            ResourceLocation id,
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            ItemStack result
    ) {
        this.id = id;
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.result = result.copy();
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return template.test(container.getItem(0))
                && base.test(container.getItem(1))
                && addition.test(container.getItem(2));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        var resultStack = result.copy();
        var baseStack = container.getItem(1);
        if (!baseStack.hasTag()) {
            return resultStack;
        }

        var copiedBaseTag = baseStack.getTag();
        if (copiedBaseTag == null) {
            return resultStack;
        }

        resultStack.setTag(copiedBaseTag.copy());
        backfillMissingDefaultTags(resultStack, result);
        return resultStack;
    }

    /**
     * ベース本の情報を優先しつつ、結果アイテム固有の必須初期タグだけ不足時に補う.
     */
    private static void backfillMissingDefaultTags(ItemStack resultStack, ItemStack defaultResultStack) {
        if (!defaultResultStack.hasTag()) {
            return;
        }

        var defaultTag = defaultResultStack.getTag();
        if (defaultTag == null) {
            return;
        }

        var resultTag = resultStack.getOrCreateTag();
        for (var key : defaultTag.getAllKeys()) {
            if (resultTag.contains(key)) {
                continue;
            }

            Tag defaultValue = defaultTag.get(key);
            if (defaultValue != null) {
                resultTag.put(key, defaultValue.copy());
            }
        }

        if (resultTag.isEmpty()) {
            resultStack.setTag((CompoundTag) null);
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 1;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER.get();
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var ingredients = NonNullList.withSize(3, Ingredient.EMPTY);
        ingredients.set(0, template);
        ingredients.set(1, base);
        ingredients.set(2, addition);
        return ingredients;
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
        return Stream.of(template, base, addition).anyMatch(net.minecraftforge.common.ForgeHooks::hasNoElements);
    }
}
