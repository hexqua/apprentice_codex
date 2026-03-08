package jp.aquafactory.apprenticecodex.datagen;

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
                        "rotten_flesh_to_leather",
                        Ingredient.of(ItemRegistry.DIVINE_PEARL.get()),
                        Ingredient.of(Items.ROTTEN_FLESH),
                        new ItemStack(Items.LEATHER)
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
