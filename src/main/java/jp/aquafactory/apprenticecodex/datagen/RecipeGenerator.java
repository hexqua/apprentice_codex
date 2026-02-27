package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeConditionRegistry;
import jp.aquafactory.apprenticecodex.recipe.condition.ApprenticeDeskRecipeEnabledCondition;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class RecipeGenerator extends RecipeProvider {
    public RecipeGenerator(PackOutput output) {
        super(output);
        RecipeConditionRegistry.register();
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        ConditionalRecipe.builder()
                .addCondition(ApprenticeDeskRecipeEnabledCondition.INSTANCE)
                .addRecipe(consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.APPRENTICE_DESK.get())
                        .pattern("CAC")
                        .pattern("SSS")
                        .pattern("F F")
                        .define('C', Items.COPPER_INGOT)
                        .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                        .define('S', ItemTags.WOODEN_SLABS)
                        .define('F', ItemTags.WOODEN_FENCES)
                        .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                        .save(consumer, ItemRegistry.APPRENTICE_DESK.getId()))
                .generateAdvancement()
                .build(recipeWriter, ItemRegistry.APPRENTICE_DESK.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SCARLET_THIRST.get())
                .pattern("VI ")
                .pattern("I I")
                .pattern(" I ")
                .define('V', io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.CRAFTSMANS_DELIGHT.get())
                .pattern("RI ")
                .pattern("IDI")
                .pattern(" I ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.REDSTONE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GRIMOIRE_MANIFEST.get())
                .pattern(" E ")
                .pattern("OBO")
                .pattern(" O ")
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get())
                .define('E', Items.ENDER_EYE)
                .define('O', Items.OBSIDIAN)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PASTEL_STAFF.get())
                .pattern(" MU")
                .pattern(" W ")
                .pattern("P  ")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('U', io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get())
                .define('W', ItemTags.PLANKS)
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()))
                .save(recipeWriter);
    }
}
