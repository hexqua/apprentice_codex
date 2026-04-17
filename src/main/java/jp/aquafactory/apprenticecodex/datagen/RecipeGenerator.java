package jp.aquafactory.apprenticecodex.datagen;

import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeConditionRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.recipe.condition.ArcanumInAJarRecipeEnabledCondition;
import jp.aquafactory.apprenticecodex.recipe.condition.ApprenticeDeskRecipeEnabledCondition;
import jp.aquafactory.apprenticecodex.recipe.condition.ExplorersCodexRecipeEnabledCondition;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
                        .pattern("GRG")
                        .pattern("GGG")
                        .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                        .define('R', Items.REDSTONE)
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

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.SPELLCASTER_WORKBENCH.get())
                .pattern(" M ")
                .pattern("SSS")
                .pattern("FAF")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(ItemRegistry.IRON_SPELLCASTER_GUN.get()), has(ItemRegistry.IRON_SPELLCASTER_GUN.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.ATELIER_STATION.get())
                .pattern("BMR")
                .pattern("SSS")
                .pattern("F F")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('B', ItemRegistry.SPELLCASTERS_FLASK.get())
                .define('R', Items.BLAZE_ROD)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(ItemRegistry.SPELLCASTERS_FLASK.get()), has(ItemRegistry.SPELLCASTERS_FLASK.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SCARLET_THIRST.get())
                .pattern("VI ")
                .pattern("IMI")
                .pattern(" I ")
                .define('V', io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.CRAFTSMANS_DELIGHT.get())
                .pattern("RID")
                .pattern("IMI")
                .pattern(" I ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', Items.REDSTONE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PROTECTION_SPELL_SUPPORTER.get())
                .pattern(" L ")
                .pattern("LML")
                .pattern("ARA")
                .define('L', Items.LEATHER)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ASHEN_CIRCLET.get())
                .pattern(" D ")
                .pattern("ATA")
                .define('T', io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get())
                .define('D', Items.DIAMOND)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ENCHANTED_CIRCLET.get())
                .pattern(" A ")
                .pattern("ITI")
                .pattern(" M ")
                .define('T', io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('A', Items.AMETHYST_CLUSTER)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get())
                .pattern("SIS")
                .pattern("MGM")
                .pattern(" M ")
                .define('G', Items.ENCHANTED_GOLDEN_APPLE)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('S', Items.STRING)
                .unlockedBy(getHasName(Items.ENCHANTED_GOLDEN_APPLE), has(Items.ENCHANTED_GOLDEN_APPLE))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCASTER_AMMO_POUCH.get())
                .pattern("MAM")
                .pattern("LCL")
                .pattern("LLL")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('L', Items.LEATHER)
                .define('C', Tags.Items.CHESTS)
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
                        .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get())
                        .define('D', Items.DIAMOND)
                        .define('G', Items.GOLD_INGOT)
                        .define('N', Items.GOLD_NUGGET)
                        .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get()))
                        .save(consumer, ItemRegistry.EXPLORERS_CODEX.getId()))
                .generateAdvancement()
                .build(recipeWriter, ItemRegistry.EXPLORERS_CODEX.getId());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.get())
                .requires(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get())
                .requires(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get(), 2)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get()),
                        has(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get()))
                .save(recipeWriter, ItemRegistry.ISEKAI_TRAVEL_GUIDEBOOK.getId());

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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CRYSTAL_BLADED_STAFF.get())
                .pattern(" DS")
                .pattern(" AD")
                .pattern("D  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.DIAMOND_SWORD)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(Items.DIAMOND_SWORD), has(Items.DIAMOND_SWORD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ELEMENTAL_BOW.get())
                .pattern(" AS")
                .pattern("DMS")
                .pattern(" AS")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.DIAMOND)
                .define('S', Items.STRING)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ILLUMINATE_STELLAR_STAFF.get())
                .pattern(" YS")
                .pattern(" NY")
                .pattern("D  ")
                .define('N', Items.NETHER_STAR)
                .define('S', Items.NETHERITE_SWORD)
                .define('Y', Items.YELLOW_STAINED_GLASS)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.UNITE_LUNA_STAFF.get())
                .pattern(" PS")
                .pattern(" RP")
                .pattern("D  ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('P', Items.PURPLE_STAINED_GLASS)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.REFLECTCAST_SHIELD.get())
                .pattern("AGA")
                .pattern("DSD")
                .pattern("AGA")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.SHIELD)
                .define('D', Items.DIAMOND)
                .define('G', Items.GLASS_PANE)
                .unlockedBy(getHasName(Items.SHIELD), has(Items.SHIELD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.IRON_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" I ")
                .pattern(" I ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.COPPER_SPELL_AMPLIFIER.get())
                .pattern("LAL")
                .pattern(" C ")
                .pattern(" C ")
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SILVER_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" S ")
                .pattern(" A ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GOLD_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" G ")
                .pattern(" G ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" D ")
                .pattern(" D ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get()),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get()
                )
                .unlocks(getHasName(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get()), has(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get()))
                .save(recipeWriter, ItemRegistry.NETHERITE_SPELL_AMPLIFIER.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.IRON_SWINGCAST_STAFF.get())
                .pattern(" AG")
                .pattern("LWI")
                .pattern("I  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GLASS)
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.COPPER_SWINGCAST_STAFF.get())
                .pattern(" AB")
                .pattern("LWC")
                .pattern("C  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get())
                .define('L', Items.LEATHER)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SILVER_SWINGCAST_STAFF.get())
                .pattern(" AG")
                .pattern("LWS")
                .pattern("A  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GLASS)
                .define('L', Items.LEATHER)
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GOLD_SWINGCAST_STAFF.get())
                .pattern(" AG")
                .pattern("LWI")
                .pattern("I  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GLASS)
                .define('L', Items.LEATHER)
                .define('I', Items.GOLD_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.DIAMOND_SWINGCAST_STAFF.get())
                .pattern(" AG")
                .pattern("LWD")
                .pattern("D  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GLASS)
                .define('L', Items.LEATHER)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.NETHERITE_SWINGCAST_STAFF.get()
                )
                .unlocks(getHasName(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()), has(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()))
                .save(recipeWriter, ItemRegistry.NETHERITE_SWINGCAST_STAFF.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PHOTON_SIPHON.get())
                .pattern("EAE")
                .pattern(" S ")
                .pattern(" S ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EXPLORERS_CANE.get())
                .pattern(" CG")
                .pattern(" IA")
                .pattern("S  ")
                .define('C', Items.COMPASS)
                .define('G', Items.GOLD_INGOT)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.GRAYBEARD_STAFF.get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCASTERS_FLASK.get())
                .pattern(" A ")
                .pattern("DBD")
                .pattern(" G ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.DIAMOND)
                .define('B', Items.GLASS_BOTTLE)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.GLASS_BOTTLE), has(Items.GLASS_BOTTLE))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.COPPER_SPELLCASTER_GUN.get())
                .pattern("CAL")
                .pattern(" CR")
                .pattern(" BC")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get())
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.DIAMOND_SPELLCASTER_GUN.get())
                .pattern("DML")
                .pattern(" DR")
                .pattern(" BD")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_UPGRADE_ORB.get())
                .define('D', Items.DIAMOND)
                .define('R', Items.REDSTONE)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Items.TORCH, 6)
                .pattern("A")
                .pattern("S")
                .define('A', ItemRegistry.ARCANE_CINDER.get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ItemRegistry.ARCANE_CINDER.get()), has(ItemRegistry.ARCANE_CINDER.get()))
                .save(recipeWriter, ResourceLocation.fromNamespaceAndPath(ItemRegistry.ARCANE_CINDER.getId().getNamespace(), "torch_from_arcane_cinder"));

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

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_HELMET.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_HAT.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_HELMET.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_HELMET.get()))
                .save(recipeWriter, ItemRegistry.ENCHANTRESS_HAT.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_CHESTPLATE.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_ROBE.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_CHESTPLATE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_CHESTPLATE.get()))
                .save(recipeWriter, ItemRegistry.ENCHANTRESS_ROBE.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_LEGGINGS.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_LEGGINGS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_LEGGINGS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_LEGGINGS.get()))
                .save(recipeWriter, ItemRegistry.ENCHANTRESS_LEGGINGS.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_BOOTS.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_BOOTS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_BOOTS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_BOOTS.get()))
                .save(recipeWriter, ItemRegistry.ENCHANTRESS_BOOTS.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_HELMET),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeWriter, ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_CHESTPLATE),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeWriter, ItemRegistry.STEALTH_RUNE_ARMOR_BODY.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_LEGGINGS),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeWriter, ItemRegistry.STEALTH_RUNE_ARMOR_LEG.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_BOOTS),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeWriter, ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.getId());

        saveMalumSpiritRepairRecipes(recipeWriter);
        saveSpellbookCarryoverSmithingRecipe(recipeWriter);

    }

    private void saveMalumSpiritRepairRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        // Malum は通常の修理判定から自動連携しないため、対象と spirit コストを明示する.
        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "apprentice_mage_robe",
                List.of(
                        ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                        ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                        ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                        ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
                ),
                io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get(),
                1,
                0.5f,
                List.of(new MalumSpiritCost("arcane", 8))
        );

        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "enchantress_robe",
                List.of(
                        ItemRegistry.ENCHANTRESS_HAT.get(),
                        ItemRegistry.ENCHANTRESS_ROBE.get(),
                        ItemRegistry.ENCHANTRESS_LEGGINGS.get(),
                        ItemRegistry.ENCHANTRESS_BOOTS.get()
                ),
                io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get(),
                1,
                0.5f,
                List.of(
                        new MalumSpiritCost("infernal", 8),
                        new MalumSpiritCost("sacred", 4)
                )
        );

        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "reflectcast_shield",
                List.of(ItemRegistry.REFLECTCAST_SHIELD.get()),
                io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get(),
                1,
                0.5f,
                List.of(new MalumSpiritCost("arcane", 8))
        );

        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "elemental_bow",
                List.of(ItemRegistry.ELEMENTAL_BOW.get()),
                io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get(),
                1,
                0.5f,
                List.of(
                        new MalumSpiritCost("arcane", 8),
                        new MalumSpiritCost("earth", 8)
                )
        );
    }

    private void saveMalumSpiritRepairRecipe(
            @NotNull Consumer<FinishedRecipe> recipeWriter,
            @NotNull String name,
            @NotNull List<Item> inputs,
            @NotNull Item repairMaterial,
            int repairMaterialCount,
            float durabilityPercentage,
            @NotNull List<MalumSpiritCost> spirits
    ) {
        var recipeId = ResourceLocation.fromNamespaceAndPath(
                ItemRegistry.APPRENTICE_MAGE_SCARF.getId().getNamespace(),
                "malum/spirit_crucible/repair/" + name
        );
        recipeWriter.accept(new MalumSpiritRepairFinishedRecipe(
                recipeId,
                inputs,
                repairMaterial,
                repairMaterialCount,
                durabilityPercentage,
                spirits
        ));
    }

    private void saveSpellbookCarryoverSmithingRecipe(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        var recipeId = ItemRegistry.SPELLSTAINED_RUNIC_TABLET.getId();
        var advancement = Advancement.Builder.recipeAdvancement()
                .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
                .addCriterion(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.GOLD_SPELL_BOOK.get()),
                        has(io.redspace.ironsspellbooks.registries.ItemRegistry.GOLD_SPELL_BOOK.get()))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(RequirementsStrategy.OR);

        recipeWriter.accept(new SpellbookCarryoverSmithingFinishedRecipe(
                recipeId,
                Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.BLANK_RUNE.get()),
                Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.GOLD_SPELL_BOOK.get()),
                Ingredient.of(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()),
                ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get(),
                advancement,
                recipeId.withPrefix("recipes/" + RecipeCategory.COMBAT.getFolderName() + "/")
        ));
    }

    private record SpellbookCarryoverSmithingFinishedRecipe(
            ResourceLocation id,
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            Item result,
            Advancement.Builder advancement,
            ResourceLocation advancementId
    ) implements FinishedRecipe {
        @Override
        public @NotNull ResourceLocation getId() {
            return id;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            json.add("template", template.toJson());
            json.add("base", base.toJson());
            json.add("addition", addition.toJson());

            var resultJson = new JsonObject();
            resultJson.addProperty("item", ForgeRegistries.ITEMS.getKey(result).toString());
            json.add("result", resultJson);
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return RecipeRegistry.SPELLBOOK_CARRYOVER_SMITHING_SERIALIZER.get();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return advancement.serializeToJson();
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return advancementId;
        }
    }

    private record MalumSpiritRepairFinishedRecipe(
            ResourceLocation id,
            List<Item> inputs,
            Item repairMaterial,
            int repairMaterialCount,
            float durabilityPercentage,
            List<MalumSpiritCost> spirits
    ) implements FinishedRecipe {
        @Override
        public @NotNull ResourceLocation getId() {
            return id;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            var conditions = new com.google.gson.JsonArray();
            var modLoadedCondition = new JsonObject();
            modLoadedCondition.addProperty("type", "forge:mod_loaded");
            modLoadedCondition.addProperty("modid", "malum");
            conditions.add(modLoadedCondition);
            json.add("conditions", conditions);

            json.addProperty("type", "malum:spirit_repair");
            json.addProperty("durabilityPercentage", durabilityPercentage);
            json.addProperty("itemIdRegex", "");
            json.addProperty("modIdRegex", "");

            var inputArray = new com.google.gson.JsonArray();
            for (var input : inputs) {
                inputArray.add(ForgeRegistries.ITEMS.getKey(input).toString());
            }
            json.add("inputs", inputArray);

            var repairMaterialJson = new JsonObject();
            repairMaterialJson.addProperty("item", ForgeRegistries.ITEMS.getKey(repairMaterial).toString());
            repairMaterialJson.addProperty("count", repairMaterialCount);
            json.add("repairMaterial", repairMaterialJson);

            var spiritArray = new com.google.gson.JsonArray();
            for (var spirit : spirits) {
                spiritArray.add(spirit.toJson());
            }
            json.add("spirits", spiritArray);
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPELESS_RECIPE;
        }

        @Override
        public @Nullable JsonObject serializeAdvancement() {
            return null;
        }

        @Override
        public @Nullable ResourceLocation getAdvancementId() {
            return null;
        }
    }

    private record MalumSpiritCost(String type, int count) {
        private JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("count", count);
            return json;
        }
    }
}
