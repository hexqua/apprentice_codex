package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeConditionRegistry;
import jp.aquafactory.apprenticecodex.recipe.condition.ArcanumInAJarRecipeEnabledCondition;
import jp.aquafactory.apprenticecodex.recipe.condition.ApprenticeDeskRecipeEnabledCondition;
import jp.aquafactory.apprenticecodex.recipe.condition.ExplorersCodexRecipeEnabledCondition;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class RecipeGenerator extends RecipeProvider {
    public RecipeGenerator(PackOutput output) {
        super(output);
        RecipeConditionRegistry.register();
    }

    @SuppressWarnings("DataFlowIssue")
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

        ConditionalRecipe.builder()
                .addCondition(ArcanumInAJarRecipeEnabledCondition.INSTANCE)
                .addRecipe(consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.ARCANUM_IN_A_JAR.get())
                        .pattern("GAG")
                        .pattern("G G")
                        .pattern("GGG")
                        .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                        .define('G', Items.GLASS_PANE)
                        .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                        .save(consumer, ItemRegistry.ARCANUM_IN_A_JAR.getId()))
                .generateAdvancement()
                .build(recipeWriter, ItemRegistry.ARCANUM_IN_A_JAR.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.ESSENCE_SMOKER.get())
                .pattern("A A")
                .pattern("FEF")
                .pattern("DCD")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('F', ItemTags.WOODEN_FENCES)
                .define('D', Items.POLISHED_DEEPSLATE)
                .define('C', Items.CAMPFIRE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()))
                .save(recipeWriter);

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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PROTECTION_SPELL_SUPPORTER.get())
                .pattern(" L ")
                .pattern("L L")
                .pattern("ARA")
                .define('L', Items.LEATHER)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get())
                .pattern("SAS")
                .pattern("MGM")
                .pattern(" M ")
                .define('G', Items.ENCHANTED_GOLDEN_APPLE)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('S', Items.STRING)
                .unlockedBy(getHasName(Items.ENCHANTED_GOLDEN_APPLE), has(Items.ENCHANTED_GOLDEN_APPLE))
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

        ConditionalRecipe.builder()
                .addCondition(ExplorersCodexRecipeEnabledCondition.INSTANCE)
                .addRecipe(consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EXPLORERS_CODEX.get())
                        .pattern("GDG")
                        .pattern("NBN")
                        .pattern("GNG")
                        .define('B', Items.WRITABLE_BOOK)
                        .define('D', Items.DIAMOND)
                        .define('G', Items.GOLD_INGOT)
                        .define('N', Items.GOLD_NUGGET)
                        .unlockedBy(getHasName(Items.WRITABLE_BOOK), has(Items.WRITABLE_BOOK))
                        .save(consumer, ItemRegistry.EXPLORERS_CODEX.getId()))
                .generateAdvancement()
                .build(recipeWriter, ItemRegistry.EXPLORERS_CODEX.getId());

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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.IRON_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern("I I")
                .pattern(" I ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.COPPER_SPELL_AMPLIFIER.get())
                .pattern("LAL")
                .pattern("C C")
                .pattern(" C ")
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GOLD_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern("G G")
                .pattern(" G ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', Items.AMETHYST_CLUSTER)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.AMETHYST_CLUSTER), has(Items.AMETHYST_CLUSTER))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PHOTON_SIPHON.get())
                .pattern("EME")
                .pattern("S S")
                .pattern(" S ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.IRON_SPELLCASTER_GUN.get())
                .pattern("IAE")
                .pattern(" IR")
                .pattern(" BI")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('E', Items.ENDER_PEARL)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GOLD_SPELLCASTER_GUN.get())
                .pattern("GAC")
                .pattern(" GR")
                .pattern(" BG")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('C', io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get())
                .define('G', Items.GOLD_INGOT)
                .define('R', Items.REDSTONE)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 12)
                .pattern("A")
                .pattern("C")
                .pattern("G")
                .define('A', Items.AMETHYST_SHARD)
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.BASIC_SPELLCASTER_ROUND.get(), 8)
                .pattern("A")
                .pattern("I")
                .pattern("G")
                .define('A', Items.AMETHYST_SHARD)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ARCANE_SPELLCASTER_ROUND.get(), 6)
                .pattern("A")
                .pattern("R")
                .pattern("G")
                .define('A', Items.AMETHYST_SHARD)
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(recipeWriter);

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_HELMET.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_SCARF.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_HELMET.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_HELMET.get()))
                .save(recipeWriter, ItemRegistry.APPRENTICE_MAGE_SCARF.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_CHESTPLATE.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_TORSO.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_CHESTPLATE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_CHESTPLATE.get()))
                .save(recipeWriter, ItemRegistry.APPRENTICE_MAGE_TORSO.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_LEGGINGS.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_LEGGINGS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_LEGGINGS.get()))
                .save(recipeWriter, ItemRegistry.APPRENTICE_MAGE_LEGGINGS.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_BOOTS.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_BOOTS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_BOOTS.get()))
                .save(recipeWriter, ItemRegistry.APPRENTICE_MAGE_BOOTS.getId());

    }
}
