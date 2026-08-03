package jp.aquafactory.apprenticecodex.datagen.recipe;

import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class EssenceSmokerRecipeDataGenerator implements DataProvider {
    private final PathProvider pathProvider;

    public EssenceSmokerRecipeDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/essence_smoker");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var recipes = List.of(
                recipe(
                        "purify_rotten_flesh_to_leather",
                        Ingredient.of(ItemRegistry.DIVINE_PEARL.get()),
                        Ingredient.of(Items.ROTTEN_FLESH),
                        new ItemStack(Items.LEATHER)
                ),
                recipe(
                        "purify_potato",
                        Ingredient.of(ItemRegistry.DIVINE_PEARL.get()),
                        Ingredient.of(Items.POISONOUS_POTATO),
                        new ItemStack(Items.POTATO)
                ),
                recipe(
                        "taint_potato",
                        Ingredient.of(ItemRegistry.BLOOD_VIAL.get()),
                        Ingredient.of(Items.POTATO),
                        new ItemStack(Items.POISONOUS_POTATO)
                ),
                recipe(
                        "taint_eye",
                        Ingredient.of(ItemRegistry.BLOOD_VIAL.get()),
                        Ingredient.of(Items.SPIDER_EYE),
                        new ItemStack(Items.FERMENTED_SPIDER_EYE)
                ),
                recipe(
                        "blaze_bonemeal_to_gunpowder",
                        Ingredient.of(Items.BLAZE_POWDER),
                        Ingredient.of(Items.BONE_MEAL),
                        new ItemStack(Items.GUNPOWDER)
                ),
                recipe(
                        "infuse_coal_to_arcane_cinder",
                        Ingredient.of(ItemRegistry.CINDER_ESSENCE.get()),
                        Ingredient.of(Items.COAL),
                        new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.ARCANE_CINDER.get())
                ),
                recipe(
                        "infuse_charcoal_to_arcane_cinder",
                        Ingredient.of(ItemRegistry.CINDER_ESSENCE.get()),
                        Ingredient.of(Items.CHARCOAL),
                        new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.ARCANE_CINDER.get())
                ),
                recipe(
                        "infuse_scroll_to_ingot",
                        Ingredient.of(ItemRegistry.SCROLL.get()),
                        Ingredient.of(ItemRegistry.ARCANE_INGOT.get()),
                        new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                ),
                recipe(
                        "infuse_cinder_to_netherite",
                        Ingredient.of(ItemRegistry.CINDER_ESSENCE.get()),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get())
                ),
                recipe(
                        "infuse_scroll_to_diamond",
                        Ingredient.of(ItemRegistry.SCROLL.get()),
                        Ingredient.of(Items.DIAMOND),
                        new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.SPELLSTAINED_DIAMOND.get())
                )

        );

        return CompletableFuture.allOf(recipes.stream()
                .map(recipe -> saveRecipe(cachedOutput, recipe))
                .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> saveRecipe(CachedOutput cachedOutput, RecipeDefinition recipe) {
        return DataProvider.saveStable(
                cachedOutput,
                createRecipeJson(recipe),
                pathProvider.json(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, recipe.path()))
        );
    }

    private static JsonObject createRecipeJson(RecipeDefinition recipe) {
        var result = recipe.result();
        if (result.isEmpty() || result.getCount() <= 0) {
            throw new IllegalArgumentException("EssenceSmoker recipe requires a non-empty result.");
        }

        var json = new JsonObject();
        json.addProperty("type", ApprenticeCodex.MODID + ":essence_smoker");
        json.add("catalyst", recipe.catalyst().toJson());
        json.add("material", recipe.material().toJson());
        json.add("result", serializeItemStack(result));
        return json;
    }

    private static JsonObject serializeItemStack(ItemStack stack) {
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            throw new IllegalStateException("Unregistered item in EssenceSmoker datagen result: " + stack.getItem());
        }

        var json = new JsonObject();
        json.addProperty("item", itemId.toString());
        if (stack.getCount() != 1) {
            json.addProperty("count", stack.getCount());
        }
        return json;
    }

    private static RecipeDefinition recipe(String path, Ingredient catalyst, Ingredient material, ItemStack result) {
        return new RecipeDefinition(path, catalyst, material, result);
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex EssenceSmoker Recipes";
    }

    private record RecipeDefinition(
            String path,
            Ingredient catalyst,
            Ingredient material,
            ItemStack result
    ) {
    }
}
