package jp.aquafactory.apprenticecodex.recipe.smithing;

import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class AlchemistsFlaskSmithingRecipe implements SmithingRecipe {
    private static final HolderLookup.Provider SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final String STORAGE_TAG = "SpellcastersFlask";
    private static final String STORED_ITEM_TAG = "StoredItem";
    private static final String PARTICLES_SUPPRESSED_TAG = "ParticlesSuppressed";

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public AlchemistsFlaskSmithingRecipe(
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
                && addition.test(input.addition())
                && canConvertStoredItem(input.base());
    }

    @Override
    public @NotNull ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        var baseStack = input.base();
        var convertedStoredItem = convertStoredItem(baseStack);
        if (convertedStoredItem == null) {
            return ItemStack.EMPTY;
        }

        var resultStack = baseStack.isEmpty()
                ? result.copy()
                : baseStack.transmuteCopy(result.getItem(), result.getCount());
        replaceStoredItem(resultStack, convertedStoredItem);
        resetEffectParticlesSuppression(resultStack);
        removeGuzzleEnchantment(resultStack);
        backfillMissingDefaultComponents(resultStack, result);
        return resultStack;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ALCHEMISTS_FLASK_SMITHING_SERIALIZER.get();
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

    private static boolean canConvertStoredItem(ItemStack baseStack) {
        return convertStoredItem(baseStack) != null;
    }

    private static ItemStack sanitizeResult(ItemStack result) {
        if (result.isEmpty() || result.getCount() <= 0) {
            throw new IllegalArgumentException("Alchemist's Flask smithing recipe requires a non-empty result.");
        }
        return result.copy();
    }

    private static ItemStack convertStoredItem(ItemStack baseStack) {
        var storedItem = AbstractPotionFlaskItem.getStoredItem(baseStack);
        if (storedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (storedItem.is(Items.POTION)) {
            var converted = new ItemStack(Items.SPLASH_POTION);
            var potionContents = storedItem.get(DataComponents.POTION_CONTENTS);
            if (potionContents != null) {
                converted.set(DataComponents.POTION_CONTENTS, potionContents);
            }
            return converted;
        }

        if (storedItem.is(Items.SPLASH_POTION) || storedItem.is(Items.LINGERING_POTION)) {
            var converted = storedItem.copy();
            converted.setCount(1);
            return converted;
        }

        if (storedItem.getItem() instanceof io.redspace.ironsspellbooks.item.consumables.SimpleElixir) {
            var converted = storedItem.copy();
            converted.setCount(1);
            return converted;
        }

        return null;
    }

    private static void replaceStoredItem(ItemStack resultStack, ItemStack storedItem) {
        if (storedItem.isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, resultStack, tag -> {
            var storageTag = tag.contains(STORAGE_TAG, Tag.TAG_COMPOUND)
                    ? tag.getCompound(STORAGE_TAG).copy()
                    : new CompoundTag();
            storageTag.put(STORED_ITEM_TAG, storedItem.saveOptional(SERIALIZATION_LOOKUP));
            tag.put(STORAGE_TAG, storageTag);
        });
    }

    private static void resetEffectParticlesSuppression(ItemStack resultStack) {
        CustomData.update(DataComponents.CUSTOM_DATA, resultStack, tag -> {
            if (!tag.contains(STORAGE_TAG, Tag.TAG_COMPOUND)) {
                return;
            }

            var storageTag = tag.getCompound(STORAGE_TAG).copy();
            storageTag.remove(PARTICLES_SUPPRESSED_TAG);
            if (storageTag.getAllKeys().isEmpty()) {
                tag.remove(STORAGE_TAG);
            } else {
                tag.put(STORAGE_TAG, storageTag);
            }
        });
    }

    private static void removeGuzzleEnchantment(ItemStack stack) {
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            var enchantment = entry.getKey();
            if (enchantment == null || enchantment.is(Enchantments.GUZZLE)) {
                continue;
            }
            enchantments.set(enchantment, entry.getValue());
        }

        EnchantmentHelper.setEnchantments(stack, enchantments.toImmutable());
    }

    /**
     * 変換元フラスコの component を優先しつつ、結果アイテム固有の初期 component だけ補う。
     */
    private static void backfillMissingDefaultComponents(ItemStack resultStack, ItemStack defaultResultStack) {
        var builder = net.minecraft.core.component.DataComponentPatch.builder();
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
}
