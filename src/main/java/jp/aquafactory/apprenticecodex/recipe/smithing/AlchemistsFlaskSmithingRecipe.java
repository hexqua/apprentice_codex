package jp.aquafactory.apprenticecodex.recipe.smithing;

import jp.aquafactory.apprenticecodex.item.flask.AbstractPotionFlaskItem;
import jp.aquafactory.apprenticecodex.item.flask.AlchemistsFlask;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public final class AlchemistsFlaskSmithingRecipe implements SmithingRecipe {
    private static final String STORAGE_TAG = "SpellcastersFlask";
    private static final String STORED_ITEM_TAG = "StoredItem";

    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public AlchemistsFlaskSmithingRecipe(
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
        if (!template.test(container.getItem(0))
                || !base.test(container.getItem(1))
                || !addition.test(container.getItem(2))) {
            return false;
        }

        return canConvertStoredItem(container.getItem(1));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        var resultStack = result.copy();
        initializeAlchemistsFlask(resultStack);

        var baseStack = container.getItem(1);
        var convertedStoredItem = convertStoredItem(baseStack);
        if (convertedStoredItem == null) {
            return ItemStack.EMPTY;
        }

        if (baseStack.hasTag()) {
            var copiedBaseTag = baseStack.getTag();
            if (copiedBaseTag != null) {
                resultStack.setTag(copiedBaseTag.copy());
            }
        }

        replaceStoredItem(resultStack, convertedStoredItem);
        removeGuzzleEnchantment(resultStack);
        backfillMissingDefaultTags(resultStack, createDefaultResultStack());
        return resultStack;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return createDefaultResultStack();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ALCHEMISTS_FLASK_SMITHING_SERIALIZER.get();
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
    public boolean isTemplateIngredient(@NotNull ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(@NotNull ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(@NotNull ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(template, base, addition).anyMatch(net.minecraftforge.common.ForgeHooks::hasNoElements);
    }

    private static boolean canConvertStoredItem(ItemStack baseStack) {
        return convertStoredItem(baseStack) != null;
    }

    private static ItemStack convertStoredItem(ItemStack baseStack) {
        var storedItem = AbstractPotionFlaskItem.getStoredItem(baseStack);
        if (storedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (storedItem.is(Items.POTION)) {
            var converted = new ItemStack(Items.SPLASH_POTION);
            if (storedItem.hasTag()) {
                if (storedItem.getTag() != null) {
                    converted.setTag(storedItem.getTag().copy());
                }
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

        var storageTag = resultStack.getTagElement(STORAGE_TAG);
        if (storageTag == null) {
            return;
        }

        storageTag.put(STORED_ITEM_TAG, storedItem.save(new CompoundTag()));
    }

    private static void removeGuzzleEnchantment(ItemStack stack) {
        if (!EnchantmentRegistry.GUZZLE.isPresent()) {
            return;
        }

        var enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.remove(EnchantmentRegistry.GUZZLE.get()) != null) {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }

    private ItemStack createDefaultResultStack() {
        var resultStack = result.copy();
        initializeAlchemistsFlask(resultStack);
        return resultStack;
    }

    private static void initializeAlchemistsFlask(ItemStack resultStack) {
        if (resultStack.getItem() instanceof AlchemistsFlask alchemistsFlask) {
            alchemistsFlask.initializeSpellContainer(resultStack);
        }
    }

    /**
     * 変換元タグを優先しつつ、結果フラスコ側の spell container 初期値だけ欠損時に補う。
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
            resultStack.setTag(null);
        }
    }
}
