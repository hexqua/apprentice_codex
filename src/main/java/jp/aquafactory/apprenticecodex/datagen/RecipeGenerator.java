package jp.aquafactory.apprenticecodex.datagen;

import jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe;
import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CompletableFuture;

public final class RecipeGenerator extends RecipeProvider {
    private static final TagKey<Item> IRONS_WIZARD_BASE_HELMET = ironItemTag("wizard_base_helmet");
    private static final TagKey<Item> IRONS_WIZARD_BASE_CHESTPLATE = ironItemTag("wizard_base_chestplate");
    private static final TagKey<Item> IRONS_WIZARD_BASE_LEGGINGS = ironItemTag("wizard_base_leggings");
    private static final TagKey<Item> IRONS_WIZARD_BASE_BOOTS = ironItemTag("wizard_base_boots");

    public RecipeGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    private static TagKey<Item> ironItemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", path));
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.APPRENTICE_DESK.get())
                .pattern("CAC")
                .pattern("SSS")
                .pattern("F F")
                .define('C', Items.COPPER_INGOT)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(recipeOutput, ItemRegistry.APPRENTICE_DESK.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.ARCANUM_IN_A_JAR.get())
                .pattern("GAG")
                .pattern("GRG")
                .pattern("GGG")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS_PANE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeOutput, ItemRegistry.ARCANUM_IN_A_JAR.getId());

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
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.SPELLCASTER_WORKBENCH.get())
                .pattern(" M ")
                .pattern("SSS")
                .pattern("FAF")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(ItemRegistry.IRON_SPELLCASTER_GUN.get()), has(ItemRegistry.IRON_SPELLCASTER_GUN.get()))
                .save(recipeOutput);

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
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.SPELL_DISPENSER.get())
                .pattern("WWW")
                .pattern("WBW")
                .pattern("ARA")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('B', Items.BOW)
                .define('R', Items.REDSTONE)
                .define('W', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.BOW), has(Items.BOW))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.SPELL_CALIBRATION_BENCH.get())
                .pattern("ACI")
                .pattern("SSS")
                .pattern("F F")
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())
                .define('C', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('A', Items.AMETHYST_SHARD)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(ItemRegistry.SCROLLCASTER_GAUNTLET.get()), has(ItemRegistry.SCROLLCASTER_GAUNTLET.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ItemRegistry.COMFORT_SANDWICH.get())
                .requires(Items.BREAD)
                .requires(ItemRegistry.COMFORT_BERRIES.get(), 2)
                .unlockedBy(getHasName(ItemRegistry.COMFORT_BERRIES.get()), has(ItemRegistry.COMFORT_BERRIES.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SCARLET_THIRST.get())
                .pattern("VI ")
                .pattern("IMI")
                .pattern(" I ")
                .define('V', io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.BLOOD_VIAL.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.CRAFTSMANS_DELIGHT.get())
                .pattern("RID")
                .pattern("IMI")
                .pattern(" I ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', Items.REDSTONE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.NATURE_RUNE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELL_CAST_PARRYING_RING.get())
                .pattern("SIR")
                .pattern("IMI")
                .pattern(" I ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .define('S', ItemRegistry.REFLECTCAST_SHIELD.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(ItemRegistry.REFLECTCAST_SHIELD.get()), has(ItemRegistry.REFLECTCAST_SHIELD.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PROTECTION_SPELL_SUPPORTER.get())
                .pattern(" L ")
                .pattern("LML")
                .pattern("ARA")
                .define('L', Items.LEATHER)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MANA_SHIELD_CHARM.get())
                .pattern(" R ")
                .pattern("ADA")
                .pattern(" M ")
                .define('D', Items.DIAMOND)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.AUTOCAST_AMULET.get())
                .pattern("I I")
                .pattern("ACA")
                .pattern(" M ")
                .define('C', Items.CLOCK)
                .define('I', Items.CHAIN)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())
                .pattern("I I")
                .pattern("ADA")
                .pattern(" M ")
                .define('D', Items.DIAMOND)
                .define('I', Items.CHAIN)
                .define('A', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()), has(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MANA_THRUSTER.get())
                .pattern("F F")
                .pattern("P P")
                .pattern("M M")
                .define('F', Items.FEATHER)
                .define('P', Items.PHANTOM_MEMBRANE)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.JUMPCAST_CHARM.get())
                .pattern("S S")
                .pattern("D D")
                .pattern("M M")
                .define('D', Items.DIAMOND)
                .define('S', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ASHEN_CIRCLET.get())
                .pattern(" D ")
                .pattern("ATA")
                .define('T', io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get())
                .define('D', Items.DIAMOND)
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ENCHANTED_CIRCLET.get())
                .pattern(" A ")
                .pattern("ITI")
                .pattern(" M ")
                .define('T', io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('A', Items.AMETHYST_CLUSTER)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ABSORPTION_AMPLIFY_AMULET.get())
                .pattern("SIS")
                .pattern("MGM")
                .pattern(" M ")
                .define('G', Items.ENCHANTED_GOLDEN_APPLE)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('S', Items.STRING)
                .unlockedBy(getHasName(Items.ENCHANTED_GOLDEN_APPLE), has(Items.ENCHANTED_GOLDEN_APPLE))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCASTER_AMMO_POUCH.get())
                .pattern("MAM")
                .pattern("LCL")
                .pattern("LLL")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('L', Items.LEATHER)
                .define('C', Tags.Items.CHESTS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCASTER_QUIVER.get())
                .pattern("I I")
                .pattern("ACA")
                .pattern(" A ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .define('C', Tags.Items.CHESTS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GRIMOIRE_MANIFEST.get())
                .pattern(" E ")
                .pattern("OBO")
                .pattern(" O ")
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get())
                .define('E', Items.ENDER_EYE)
                .define('O', Items.OBSIDIAN)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EXPLORERS_CODEX.get())
                .pattern("GDG")
                .pattern("NBN")
                .pattern("GNG")
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get())
                .define('D', Items.DIAMOND)
                .define('G', Items.GOLD_INGOT)
                .define('N', Items.GOLD_NUGGET)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get()))
                .save(recipeOutput, ItemRegistry.EXPLORERS_CODEX.getId());

        saveAlchemistsFlaskSmithingRecipe(recipeOutput);
        saveSpellbookCarryoverSmithingRecipe(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ARCHIVISTS_GRIMOIRE.get())
                .pattern("MDM")
                .pattern("CBC")
                .pattern("MWM")
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get())
                .define('D', Items.DIAMOND)
                .define('C', Tags.Items.CHESTS)
                .define('W', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PASTEL_STAFF.get())
                .pattern(" MU")
                .pattern(" W ")
                .pattern("P  ")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('U', io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get())
                .define('W', ItemTags.PLANKS)
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MULTICAST_ECHO_STAFF.get())
                .pattern(" ME")
                .pattern(" WN")
                .pattern("PN ")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('E', Items.ECHO_SHARD)
                .define('N', Items.NETHERITE_INGOT)
                .define('W', ItemTags.PLANKS)
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ZENITH_STAFF.get())
                .pattern(" MS")
                .pattern(" WC")
                .pattern("P  ")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get())
                .define('C', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
                .define('W', ItemTags.PLANKS)
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.UPGRADE_ORB.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CRYSTAL_BLADED_STAFF.get())
                .pattern(" DS")
                .pattern(" AD")
                .pattern("D  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.DIAMOND_SWORD)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(Items.DIAMOND_SWORD), has(Items.DIAMOND_SWORD))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get())
                .pattern(" NS")
                .pattern(" TN")
                .pattern("A  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('N', Items.NETHERITE_SCRAP)
                .define('T', Items.TRIDENT)
                .unlockedBy(getHasName(Items.NETHERITE_SWORD), has(Items.NETHERITE_SWORD))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MANA_FORCE_BLADE.get())
                .pattern(" A ")
                .pattern(" A ")
                .pattern("MSM")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(Items.NETHERITE_SWORD), has(Items.NETHERITE_SWORD))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELL_SIDE_EDGE.get())
                .pattern(" M")
                .pattern("EC")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('C', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('E', Items.EMERALD)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ELEMENTAL_BOW.get())
                .pattern(" AS")
                .pattern("DMS")
                .pattern(" AS")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.DIAMOND)
                .define('S', Items.STRING)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.FOCUS_STAFFBOW.get())
                .pattern(" MS")
                .pattern("DPS")
                .pattern(" MS")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.PYRIUM_INGOT.get())
                .define('D', io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get())
                .define('S', Items.STRING)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ILLUMINATE_STELLAR_STAFF.get())
                .pattern(" YS")
                .pattern(" NY")
                .pattern("D  ")
                .define('N', Items.NETHER_STAR)
                .define('S', Items.NETHERITE_SWORD)
                .define('Y', Items.YELLOW_STAINED_GLASS)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.UNITE_LUNA_STAFF.get())
                .pattern(" PS")
                .pattern(" RP")
                .pattern("D  ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('P', Items.PURPLE_STAINED_GLASS)
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CIRCUIT_HEAT_STAFF.get())
                .pattern(" PD")
                .pattern(" WG")
                .pattern("P  ")
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.PYRIUM_INGOT.get())
                .define('G', Items.GOLD_INGOT)
                .define('D', Items.DIAMOND)
                .define('W', ItemTags.PLANKS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PYRIUM_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PYRIUM_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SMASHCAST_SCEPTER.get())
                .pattern(" MI")
                .pattern(" WM")
                .pattern("D  ")
                .define('I', Items.HEAVY_CORE)
                .define('W', Items.BREEZE_ROD)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(Items.HEAVY_CORE), has(Items.HEAVY_CORE))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SCROLLCASTER_GAUNTLET.get())
                .pattern(" M ")
                .pattern("MDM")
                .pattern("LBL")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', Items.DIAMOND)
                .define('L', Items.LEATHER)
                .define('B', Items.CHISELED_BOOKSHELF)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.REFLECTCAST_SHIELD.get())
                .pattern("AGA")
                .pattern("DSD")
                .pattern("AGA")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.SHIELD)
                .define('D', Items.DIAMOND)
                .define('G', Items.GLASS_PANE)
                .unlockedBy(getHasName(Items.SHIELD), has(Items.SHIELD))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.IRON_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" I ")
                .pattern(" I ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.COPPER_SPELL_AMPLIFIER.get())
                .pattern("LAL")
                .pattern(" C ")
                .pattern(" C ")
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_BOTTLE.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SILVER_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" S ")
                .pattern(" A ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GOLD_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" G ")
                .pattern(" G ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get())
                .pattern("EAE")
                .pattern(" D ")
                .pattern(" D ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeOutput);

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get()),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.NETHERITE_SPELL_AMPLIFIER.get()
                )
                .unlocks(getHasName(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get()), has(ItemRegistry.DIAMOND_SPELL_AMPLIFIER.get()))
                .save(recipeOutput, ItemRegistry.NETHERITE_SPELL_AMPLIFIER.getId());

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
                .save(recipeOutput);

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
                .save(recipeOutput);

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
                .save(recipeOutput);

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
                .save(recipeOutput);

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
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MITHRIL_FREECAST_STAFF.get())
                .pattern(" AG")
                .pattern("LWD")
                .pattern("D  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GLASS)
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('D', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.REVOLVERCAST_STAFF.get())
                .pattern(" AG")
                .pattern("LWM")
                .pattern("P  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('G', Items.GLASS)
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.PYRIUM_INGOT.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeOutput);

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()),
                        Ingredient.of(Items.NETHERITE_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.NETHERITE_SWINGCAST_STAFF.get()
                )
                .unlocks(getHasName(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()), has(ItemRegistry.DIAMOND_SWINGCAST_STAFF.get()))
                .save(recipeOutput, ItemRegistry.NETHERITE_SWINGCAST_STAFF.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PHOTON_SIPHON.get())
                .pattern("EAE")
                .pattern(" S ")
                .pattern(" S ")
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeOutput);

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
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCASTERS_FLASK.get())
                .pattern(" A ")
                .pattern("DBD")
                .pattern(" G ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', Items.DIAMOND)
                .define('B', Items.GLASS_BOTTLE)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(Items.GLASS_BOTTLE), has(Items.GLASS_BOTTLE))
                .save(recipeOutput);

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
                .save(recipeOutput);

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
                .save(recipeOutput);

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
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get())
                .pattern("MWL")
                .pattern(" MR")
                .pattern(" BM")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('W', io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get())
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_UPGRADE_ORB.get())
                .define('R', Items.REDSTONE)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get()))
                .save(recipeOutput);

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
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.RAPID_SPELLCASTER_ROUND.get(), 12)
                .pattern("A")
                .pattern("C")
                .pattern("G")
                .define('A', Items.AMETHYST_SHARD)
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Items.TORCH, 6)
                .pattern("A")
                .pattern("S")
                .define('A', ItemRegistry.ARCANE_CINDER.get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ItemRegistry.ARCANE_CINDER.get()), has(ItemRegistry.ARCANE_CINDER.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(ItemRegistry.ARCANE_CINDER.getId().getNamespace(), "torch_from_arcane_cinder"));

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_HELMET.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_SCARF.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_HELMET.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_HELMET.get()))
                .save(recipeOutput, ItemRegistry.APPRENTICE_MAGE_SCARF.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_CHESTPLATE.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_TORSO.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_CHESTPLATE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_CHESTPLATE.get()))
                .save(recipeOutput, ItemRegistry.APPRENTICE_MAGE_TORSO.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_LEGGINGS.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_LEGGINGS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_LEGGINGS.get()))
                .save(recipeOutput, ItemRegistry.APPRENTICE_MAGE_LEGGINGS.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_BOOTS.get()),
                        Ingredient.of(Items.IRON_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.APPRENTICE_MAGE_BOOTS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_BOOTS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.WANDERING_MAGICIAN_BOOTS.get()))
                .save(recipeOutput, ItemRegistry.APPRENTICE_MAGE_BOOTS.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_HELMET.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_HAT.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_HELMET.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_HELMET.get()))
                .save(recipeOutput, ItemRegistry.ENCHANTRESS_HAT.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_CHESTPLATE.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_ROBE.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_CHESTPLATE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_CHESTPLATE.get()))
                .save(recipeOutput, ItemRegistry.ENCHANTRESS_ROBE.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_LEGGINGS.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_LEGGINGS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_LEGGINGS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_LEGGINGS.get()))
                .save(recipeOutput, ItemRegistry.ENCHANTRESS_LEGGINGS.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.HOGSKIN.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_BOOTS.get()),
                        Ingredient.of(Items.GOLD_INGOT),
                        RecipeCategory.COMBAT,
                        ItemRegistry.ENCHANTRESS_BOOTS.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_BOOTS.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PUMPKIN_BOOTS.get()))
                .save(recipeOutput, ItemRegistry.ENCHANTRESS_BOOTS.getId());

        saveChromaticMagiaDressSmithingRecipes(recipeOutput);
        saveElementMaidenRobeSmithingRecipes(recipeOutput);

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_HELMET),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeOutput, ItemRegistry.STEALTH_RUNE_ARMOR_HEAD.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_CHESTPLATE),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_BODY.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeOutput, ItemRegistry.STEALTH_RUNE_ARMOR_BODY.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_LEGGINGS),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_LEG.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeOutput, ItemRegistry.STEALTH_RUNE_ARMOR_LEG.getId());

        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(Items.GOLDEN_BOOTS),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()),
                        RecipeCategory.COMBAT,
                        ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.get()
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.INVISIBILITY_ELIXIR.get()))
                .save(recipeOutput, ItemRegistry.STEALTH_RUNE_ARMOR_FOOT.getId());
    }

    private void saveChromaticMagiaDressSmithingRecipes(@NotNull RecipeOutput recipeOutput) {
        saveChromaticMagiaDressSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_HELMET,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()
        );
        saveChromaticMagiaDressSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_CHESTPLATE,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()
        );
        saveChromaticMagiaDressSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_LEGGINGS,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()
        );
        saveChromaticMagiaDressSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_BOOTS,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
        );
    }

    private void saveElementMaidenRobeSmithingRecipes(@NotNull RecipeOutput recipeOutput) {
        saveElementMaidenRobeSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_HELMET,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()
        );
        saveElementMaidenRobeSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_CHESTPLATE,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()
        );
        saveElementMaidenRobeSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_LEGGINGS,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()
        );
        saveElementMaidenRobeSmithingRecipe(
                recipeOutput,
                IRONS_WIZARD_BASE_BOOTS,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
        );
    }

    private void saveChromaticMagiaDressSmithingRecipe(
            @NotNull RecipeOutput recipeOutput,
            TagKey<Item> baseTag,
            Item result
    ) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()),
                        Ingredient.of(baseTag),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()),
                        RecipeCategory.COMBAT,
                        result
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get()))
                .save(recipeOutput, BuiltInRegistries.ITEM.getKey(result));
    }

    private void saveElementMaidenRobeSmithingRecipe(
            @NotNull RecipeOutput recipeOutput,
            TagKey<Item> baseTag,
            Item result
    ) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()),
                        Ingredient.of(baseTag),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()),
                        RecipeCategory.COMBAT,
                        result
                )
                .unlocks(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()))
                .save(recipeOutput, BuiltInRegistries.ITEM.getKey(result));
    }

    private void saveAlchemistsFlaskSmithingRecipe(@NotNull RecipeOutput recipeOutput) {
        var recipeId = ItemRegistry.ALCHEMISTS_FLASK.getId();
        var defaultResult = ItemRegistry.ALCHEMISTS_FLASK.get().getDefaultInstance();

        var advancement = recipeOutput.advancement()
                .addCriterion(
                        getHasName(ItemRegistry.SPELLCASTERS_FLASK.get()),
                        has(ItemRegistry.SPELLCASTERS_FLASK.get())
                )
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.withPrefix("recipes/" + RecipeCategory.COMBAT.getFolderName() + "/"));

        recipeOutput.accept(
                recipeId,
                new AlchemistsFlaskSmithingRecipe(
                        Ingredient.of(Items.EMERALD),
                        Ingredient.of(ItemRegistry.SPELLCASTERS_FLASK.get()),
                        Ingredient.of(Items.GUNPOWDER),
                        defaultResult
                ),
                advancement
        );
    }

    private void saveSpellbookCarryoverSmithingRecipe(@NotNull RecipeOutput recipeOutput) {
        var recipeId = ItemRegistry.SPELLSTAINED_RUNIC_TABLET.getId();
        var defaultResult = new ItemStack(ItemRegistry.SPELLSTAINED_RUNIC_TABLET.get(), 1);

        var advancement = recipeOutput.advancement()
                .addCriterion(
                        getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.GOLD_SPELL_BOOK.get()),
                        has(io.redspace.ironsspellbooks.registries.ItemRegistry.GOLD_SPELL_BOOK.get())
                )
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.withPrefix("recipes/" + RecipeCategory.COMBAT.getFolderName() + "/"));

        recipeOutput.accept(
                recipeId,
                new SpellbookCarryoverSmithingRecipe(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.BLANK_RUNE.get()),
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.GOLD_SPELL_BOOK.get()),
                        Ingredient.of(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()),
                        defaultResult
                ),
                advancement
        );
    }
}
