package jp.aquafactory.apprenticecodex.datagen;

import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class RecipeGenerator extends RecipeProvider {
    private static final TagKey<Item> IRONS_WIZARD_BASE_HELMET = ironItemTag("wizard_base_helmet");
    private static final TagKey<Item> IRONS_WIZARD_BASE_CHESTPLATE = ironItemTag("wizard_base_chestplate");
    private static final TagKey<Item> IRONS_WIZARD_BASE_LEGGINGS = ironItemTag("wizard_base_leggings");
    private static final TagKey<Item> IRONS_WIZARD_BASE_BOOTS = ironItemTag("wizard_base_boots");

    public RecipeGenerator(PackOutput output) {
        super(output);
    }

    private static TagKey<Item> ironItemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", path));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        var waterPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.CRUDE_INK.get())
                // 1.20.1 ではポーション種別がNBTにあるため、水入り瓶だけに部分一致させる。
                .requires(PartialNBTIngredient.of(Items.POTION, waterPotion.getOrCreateTag()))
                .requires(Items.LAPIS_LAZULI)
                .requires(Items.REDSTONE)
                .requires(Items.GLOW_BERRIES, 2)
                .requires(Items.INK_SAC)
                .unlockedBy(getHasName(Items.GLOW_BERRIES), has(Items.GLOW_BERRIES))
                .save(recipeWriter, ItemRegistry.CRUDE_INK.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.APPRENTICE_DESK.get())
                .pattern("CAC")
                .pattern("SSS")
                .pattern("F F")
                .define('C', Items.COPPER_INGOT)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(recipeWriter, ItemRegistry.APPRENTICE_DESK.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.ARCANUM_IN_A_JAR.get())
                .pattern("GAG")
                .pattern("GRG")
                .pattern("GGG")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS_PANE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
                .save(recipeWriter, ItemRegistry.ARCANUM_IN_A_JAR.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.ESSENCE_SMOKER.get())
                .pattern("A A")
                .pattern("FEF")
                .pattern("DCD")
                .define('A', Items.AMETHYST_SHARD)
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get())
                .define('F', ItemTags.WOODEN_FENCES)
                .define('D', Items.POLISHED_DEEPSLATE)
                .define('C', Items.CAMPFIRE)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.SPELLCASTER_WORKBENCH.get())
                .pattern(" M ")
                .pattern("SSS")
                .pattern("FAF")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BREWING, ItemRegistry.ALCHEMY_BREWER.get())
                .pattern("BMA")
                .pattern("SSS")
                .pattern("FWF")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('B', Items.DECORATED_POT)
                .define('A', Items.AMETHYST_CLUSTER)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('F', ItemTags.WOODEN_FENCES)
                .define('W', Items.WATER_BUCKET)
                .unlockedBy(getHasName(Items.DECORATED_POT), has(Items.DECORATED_POT))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BREWING, ItemRegistry.ATELIER_STATION.get())
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

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ItemRegistry.SPELL_DISPENSER.get())
                .pattern("WWW")
                .pattern("WBW")
                .pattern("ARA")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('B', Items.BOW)
                .define('R', Items.REDSTONE)
                .define('W', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.BOW), has(Items.BOW))
                .save(recipeWriter);

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
                .save(recipeWriter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ItemRegistry.COMFORT_SANDWICH.get())
                .requires(Items.BREAD)
                .requires(ItemRegistry.COMFORT_BERRIES.get(), 2)
                .unlockedBy(getHasName(ItemRegistry.COMFORT_BERRIES.get()), has(ItemRegistry.COMFORT_BERRIES.get()))
                .save(recipeWriter);

        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()),
                        RecipeCategory.MISC,
                        io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get(),
                        1.0F,
                        100
                )
                .unlockedBy(
                        getHasName(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()),
                        has(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get())
                )
                .save(recipeWriter, ItemRegistry.CRYSTALLINE_ARCANE_SHARD.getId());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ItemRegistry.WISDOM_SHARD.get())
                .requires(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get())
                .requires(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .requires(ItemRegistry.ARCANE_CINDER.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.OVERDRIVE_BROOM_ENGINE.get())
                .pattern("ARA")
                .pattern("RCR")
                .pattern("ARA")
                .define('C', io.redspace.ironsspellbooks.registries.ItemRegistry.ENERGIZED_CORE.get())
                .define('A', ItemRegistry.ARCANE_CINDER.get())
                .define('R', Items.REDSTONE_BLOCK)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ENERGIZED_CORE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ENERGIZED_CORE.get()))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ItemRegistry.SPELL_EXTRACT_SHARD.get())
                .requires(Items.FLINT)
                .requires(io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get())
                .requires(ItemRegistry.ARCANE_CINDER.get())
                .requires(Items.GUNPOWDER)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SHRIVING_STONE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.STORAGE_STABILIZER.get())
                .pattern(" MA")
                .pattern(" C ")
                .pattern("AB ")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('C', Items.ENDER_CHEST)
                .define('B', Items.BLAZE_ROD)
                .unlockedBy(getHasName(Items.ENDER_CHEST), has(Items.ENDER_CHEST))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.LUMINOUS_DEVICE.get())
                .pattern("  A")
                .pattern(" W ")
                .pattern("IC ")
                .define('A', ItemRegistry.ARCANE_CINDER.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('C', Tags.Items.CHESTS)
                .define('W', ItemTags.PLANKS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.CINDER_ESSENCE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.CINDER_ESSENCE.get()))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELL_CAST_PARRYING_RING.get())
                .pattern("SIR")
                .pattern("IMI")
                .pattern(" I ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .define('S', Items.SHIELD)
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(Items.SHIELD), has(Items.SHIELD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ATTACKCAST_RING.get())
                .pattern("SIR")
                .pattern("IMI")
                .pattern(" I ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())
                .define('S', ItemRegistry.MITHRIL_FREECAST_STAFF.get())
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(ItemRegistry.MITHRIL_FREECAST_STAFF.get()), has(ItemRegistry.MITHRIL_FREECAST_STAFF.get()))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MANA_SHIELD_CHARM.get())
                .pattern(" R ")
                .pattern("ADA")
                .pattern(" M ")
                .define('D', Items.DIAMOND)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.AUTOCAST_AMULET.get())
                .pattern("I I")
                .pattern("ACA")
                .pattern(" M ")
                .define('C', Items.CLOCK)
                .define('I', Items.CHAIN)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get())
                .pattern("I I")
                .pattern("ADA")
                .pattern(" M ")
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('I', Items.CHAIN)
                .define('A', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(ItemRegistry.SPELLSTAINED_DIAMOND.get()), has(ItemRegistry.SPELLSTAINED_DIAMOND.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MANA_THRUSTER.get())
                .pattern("F F")
                .pattern("P P")
                .pattern("M M")
                .define('F', Items.FEATHER)
                .define('P', Items.PHANTOM_MEMBRANE)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.JUMPCAST_CHARM.get())
                .pattern("S S")
                .pattern("D D")
                .pattern("M M")
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('S', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(ItemRegistry.SPELLSTAINED_DIAMOND.get()), has(ItemRegistry.SPELLSTAINED_DIAMOND.get()))
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
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCASTER_QUIVER.get())
                .pattern("I I")
                .pattern("ACA")
                .pattern(" A ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .define('C', Tags.Items.CHESTS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get()))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.EXPLORERS_CODEX.get())
                .pattern("GDG")
                .pattern("NBN")
                .pattern("GNG")
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get())
                .define('D', Items.DIAMOND)
                .define('G', Items.GOLD_INGOT)
                .define('N', Items.GOLD_NUGGET)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.COPPER_SPELL_BOOK.get()))
                .save(recipeWriter, ItemRegistry.EXPLORERS_CODEX.getId());

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ARCHIVISTS_GRIMOIRE.get())
                .pattern("MDM")
                .pattern("CBC")
                .pattern("MWM")
                .define('B', io.redspace.ironsspellbooks.registries.ItemRegistry.RUINED_BOOK.get())
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('C', Tags.Items.CHESTS)
                .define('W', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_WEAVE.get()))
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
                .save(recipeWriter);

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
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CRYSTAL_BLADED_STAFF.get())
                .pattern(" DS")
                .pattern(" WD")
                .pattern("A  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.DIAMOND_SWORD)
                .define('W', ItemTags.PLANKS)
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .unlockedBy(getHasName(Items.DIAMOND_SWORD), has(Items.DIAMOND_SWORD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get())
                .pattern(" NS")
                .pattern(" TN")
                .pattern("A  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('N', Items.NETHERITE_SCRAP)
                .define('T', Items.TRIDENT)
                .unlockedBy(getHasName(Items.NETHERITE_SWORD), has(Items.NETHERITE_SWORD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MANA_FORCE_BLADE.get())
                .pattern(" A ")
                .pattern(" A ")
                .pattern("MSM")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(Items.NETHERITE_SWORD), has(Items.NETHERITE_SWORD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELL_SIDE_EDGE.get())
                .pattern(" M")
                .pattern("EC")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('C', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('E', Items.EMERALD)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SPELLCHARGED_GREATSWORD.get())
                .pattern("  N")
                .pattern("AN ")
                .pattern("MA ")
                .define('N', Items.NETHERITE_INGOT)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ELEMENTAL_BOW.get())
                .pattern(" AS")
                .pattern("DMS")
                .pattern(" AS")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('S', Items.STRING)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.FOCUS_STAFFBOW.get())
                .pattern(" MS")
                .pattern("DPS")
                .pattern(" MS")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('P', io.redspace.ironsspellbooks.registries.ItemRegistry.PYRIUM_INGOT.get())
                .define('D', io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get())
                .define('S', Items.STRING)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.ILLUMINATE_STELLAR_STAFF.get())
                .pattern(" YS")
                .pattern(" NY")
                .pattern("D  ")
                .define('N', Items.NETHER_STAR)
                .define('S', Items.NETHERITE_SWORD)
                .define('Y', Items.YELLOW_STAINED_GLASS)
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .unlockedBy(getHasName(Items.NETHER_STAR), has(Items.NETHER_STAR))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.UNITE_LUNA_STAFF.get())
                .pattern(" PS")
                .pattern(" RP")
                .pattern("D  ")
                .define('R', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .define('S', Items.NETHERITE_SWORD)
                .define('P', Items.PURPLE_STAINED_GLASS)
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CIRCUIT_HEAT_STAFF.get())
                .pattern(" PD")
                .pattern(" WG")
                .pattern("P  ")
                .define('P', ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get())
                .define('G', Items.GOLD_INGOT)
                .define('D', Items.DIAMOND)
                .define('W', ItemTags.PLANKS)
                .unlockedBy(getHasName(ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get()), has(ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SMASHCAST_SCEPTER.get())
                .pattern(" MI")
                .pattern(" WM")
                .pattern("D  ")
                .define('I', Items.IRON_BLOCK) // 申し送り:1.21.1ではヘビーコアにしたい.
                .define('W', io.redspace.ironsspellbooks.registries.ItemRegistry.WEAPON_PARTS.get()) // 申し送り:1.21.1ではブリーズロッドにしたい.
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .unlockedBy(getHasName(Items.IRON_BLOCK), has(Items.IRON_BLOCK))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.SCROLLCASTER_GAUNTLET.get())
                .pattern(" M ")
                .pattern("MDM")
                .pattern("LBL")
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('L', Items.LEATHER)
                .define('B', Items.CHISELED_BOOKSHELF)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.CHARGECAST_CATALYSTBOOK.get())
                .pattern("GLA")
                .pattern("DPP")
                .pattern("GLA")
                .define('A', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('L', Items.LEATHER)
                .define('P', Items.PAPER)
                .define('D', Items.DIAMOND)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy(getHasName(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()), has(ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.REFLECTCAST_SHIELD.get())
                .pattern("AGA")
                .pattern("DSD")
                .pattern("AGA")
                .define('A', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('S', Items.SHIELD)
                .define('D', Items.DIAMOND)
                .define('G', Items.GLASS_PANE)
                .unlockedBy(getHasName(Items.SHIELD), has(Items.SHIELD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.BULWARK_GREATSHIELD.get())
                .pattern("NMN")
                .pattern("NSN")
                .pattern("NAN")
                .define('A', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('S', Items.SHIELD)
                .define('N', Items.NETHERITE_INGOT)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(Items.SHIELD), has(Items.SHIELD))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.PARRYCAST_BUCKLER.get())
                .pattern(" N ")
                .pattern("MSM")
                .pattern(" A ")
                .define('A', ItemRegistry.SPELLSTAINED_ARCANE_INGOT.get())
                .define('S', Items.SHIELD)
                .define('N', Items.NETHERITE_INGOT)
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
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
                .pattern(" GD")
                .pattern("LWI")
                .pattern("I  ")
                .define('G', Items.GOLD_INGOT)
                .define('W', ItemTags.PLANKS)
                .define('D', Items.DIAMOND)
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.DIAMOND), has(Items.DIAMOND))
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
                .pattern(" AD")
                .pattern("LWS")
                .pattern("A  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('L', Items.LEATHER)
                .define('S', io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.GOLD_SWINGCAST_STAFF.get())
                .pattern(" AD")
                .pattern("LWI")
                .pattern("I  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('L', Items.LEATHER)
                .define('I', Items.GOLD_INGOT)
                .unlockedBy(getHasName(ItemRegistry.SPELLSTAINED_DIAMOND.get()), has(ItemRegistry.SPELLSTAINED_DIAMOND.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.DIAMOND_SWINGCAST_STAFF.get())
                .pattern(" AS")
                .pattern("LWD")
                .pattern("D  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('L', Items.LEATHER)
                .define('S', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(ItemRegistry.SPELLSTAINED_DIAMOND.get()), has(ItemRegistry.SPELLSTAINED_DIAMOND.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.MITHRIL_FREECAST_STAFF.get())
                .pattern(" AI")
                .pattern("LWD")
                .pattern("D  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('I', Items.DIAMOND)
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('D', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ItemRegistry.REVOLVERCAST_STAFF.get())
                .pattern(" AD")
                .pattern("LWM")
                .pattern("P  ")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('W', ItemTags.PLANKS)
                .define('D', ItemRegistry.SPELLSTAINED_DIAMOND.get())
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_INGOT.get())
                .define('P', ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get())
                .unlockedBy(getHasName(ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get()), has(ItemRegistry.EMBERSTAINED_NETHERITE_INGOT.get()))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ItemRegistry.FLOATMOUNT_BROOM.get())
                .pattern(" PW")
                .pattern("EWB")
                .pattern("HI ")
                .define('W', ItemTags.PLANKS)
                .define('B', Items.BLAZE_ROD)
                .define('P', Items.PHANTOM_MEMBRANE)
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('E', io.redspace.ironsspellbooks.registries.ItemRegistry.ENDER_RUNE.get())
                .define('H', Items.HAY_BLOCK)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.ENDER_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.ENDER_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ItemRegistry.HOVERRIDE_BROOM.get())
                .pattern(" PW")
                .pattern("LWP")
                .pattern("HI ")
                .define('W', ItemTags.PLANKS)
                .define('P', Items.PHANTOM_MEMBRANE)
                .define('I', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('L', io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_RUNE.get())
                .define('H', Items.HAY_BLOCK)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_RUNE.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.LIGHTNING_RUNE.get()))
                .save(recipeWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ItemRegistry.INSTANT_SEARCH_BRAZIER.get())
                .pattern("REL")
                .pattern("GCG")
                .pattern(" A ")
                .define('R', Items.REDSTONE_BLOCK)
                .define('E', Items.EMERALD)
                .define('L', Items.LAPIS_BLOCK)
                .define('G', Items.GOLD_INGOT)
                .define('C', Items.COMPASS)
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .unlockedBy(getHasName(Items.COMPASS), has(Items.COMPASS))
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
                .pattern("DAM")
                .pattern(" DR")
                .pattern(" BD")
                .define('A', io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get())
                .define('M', io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get())
                .define('D', Items.DIAMOND)
                .define('R', Items.REDSTONE)
                .define('B', ItemTags.BUTTONS)
                .unlockedBy(getHasName(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()), has(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get()))
                .save(recipeWriter);

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

        saveChromaticMagiaDressSmithingRecipes(recipeWriter);
        saveElementMaidenRobeSmithingRecipes(recipeWriter);
        saveMagiAgentSuitSmithingRecipes(recipeWriter);
        recipeWriter.accept(new MalumSpellAmplifierFinishedRecipe(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "soulstained_steel_spell_amplifier")
        ));

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
        saveMalumSpiritInfusionRecipes(recipeWriter);
        saveAlchemistsFlaskSmithingRecipe(recipeWriter);
        saveSpellbookCarryoverSmithingRecipe(recipeWriter);

    }

    private void saveChromaticMagiaDressSmithingRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        saveChromaticMagiaDressSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_HELMET,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()
        );
        saveChromaticMagiaDressSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_CHESTPLATE,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()
        );
        saveChromaticMagiaDressSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_LEGGINGS,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()
        );
        saveChromaticMagiaDressSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_BOOTS,
                ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()
        );
    }

    private void saveElementMaidenRobeSmithingRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        saveElementMaidenRobeSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_HELMET,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()
        );
        saveElementMaidenRobeSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_CHESTPLATE,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()
        );
        saveElementMaidenRobeSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_LEGGINGS,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()
        );
        saveElementMaidenRobeSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_BOOTS,
                ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
        );
    }

    private void saveMagiAgentSuitSmithingRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        saveMagiAgentSuitSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_HELMET,
                ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()
        );
        saveMagiAgentSuitSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_CHESTPLATE,
                ItemRegistry.MAGI_AGENT_SUIT_COAT.get()
        );
        saveMagiAgentSuitSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_LEGGINGS,
                ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()
        );
        saveMagiAgentSuitSmithingRecipe(
                recipeWriter,
                IRONS_WIZARD_BASE_BOOTS,
                ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()
        );
    }

    private void saveChromaticMagiaDressSmithingRecipe(
            @NotNull Consumer<FinishedRecipe> recipeWriter,
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
                .save(recipeWriter, ForgeRegistries.ITEMS.getKey(result));
    }

    private void saveElementMaidenRobeSmithingRecipe(
            @NotNull Consumer<FinishedRecipe> recipeWriter,
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
                .save(recipeWriter, ForgeRegistries.ITEMS.getKey(result));
    }

    private void saveMagiAgentSuitSmithingRecipe(
            @NotNull Consumer<FinishedRecipe> recipeWriter,
            TagKey<Item> baseTag,
            Item result
    ) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()),
                        Ingredient.of(baseTag),
                        Ingredient.of(ItemRegistry.SPELL_DOMINATOR_ROUND.get()),
                        RecipeCategory.COMBAT,
                        result
                )
                .unlocks(getHasName(ItemRegistry.SPELL_DOMINATOR_ROUND.get()), has(ItemRegistry.SPELL_DOMINATOR_ROUND.get()))
                .save(recipeWriter, ForgeRegistries.ITEMS.getKey(result));
    }

    private void saveMalumSpiritRepairRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        // Malum は通常の修理判定から自動連携しないため、対象と spirit コストを明示する.
        // 素材ごとにアイテムリストをまとめることでJEIのUX改善を図る.
        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "arcane_essence_armaments_repair",
                List.of(
                        ItemRegistry.APPRENTICE_MAGE_SCARF.get(),
                        ItemRegistry.APPRENTICE_MAGE_TORSO.get(),
                        ItemRegistry.APPRENTICE_MAGE_LEGGINGS.get(),
                        ItemRegistry.APPRENTICE_MAGE_BOOTS.get(),
                        ItemRegistry.REFLECTCAST_SHIELD.get()
                ),
                io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_ESSENCE.get(),
                1,
                0.5f,
                List.of(new MalumSpiritCost("arcane", 8))
        );

        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "arcane_ingot_armaments_repair",
                List.of(
                        ItemRegistry.ELEMENTAL_BOW.get(),
                        ItemRegistry.MANA_FORCE_BLADE.get(),
                        ItemRegistry.SPELL_SIDE_EDGE.get(),
                        ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                        ItemRegistry.BULWARK_GREATSHIELD.get(),
                        ItemRegistry.PARRYCAST_BUCKLER.get()
                ),
                io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT.get(),
                1,
                0.5f,
                List.of(
                        new MalumSpiritCost("arcane", 8),
                        new MalumSpiritCost("earth", 8)
                )
        );

        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "hogskin_armaments_repair",
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

        // ミスリルだけは算出数が少ないので修理割合高め.
        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "mithril_scrap_armaments_repair",
                List.of(
                        ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get(),
                        ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get(),
                        ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get(),
                        ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get(),
                        ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get(),
                        ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(),
                        ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get(),
                        ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get()
                ),
                io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get(),
                1,
                1.0f,
                List.of(
                        new MalumSpiritCost("arcane", 16),
                        new MalumSpiritCost("earth", 16),
                        new MalumSpiritCost("sacred", 16),
                        new MalumSpiritCost("eldritch", 4)
                )
        );

        saveMalumSpiritRepairRecipe(
                recipeWriter,
                "magic_cloth_armaments_repair",
                List.of(
                        ItemRegistry.MAGI_AGENT_SUIT_HOOD.get(),
                        ItemRegistry.MAGI_AGENT_SUIT_COAT.get(),
                        ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get(),
                        ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get(),
                        ItemRegistry.SOULCOLLECTOR_HAT.get(),
                        ItemRegistry.SOULCOLLECTOR_ROBE.get(),
                        ItemRegistry.SOULCOLLECTOR_LEGGINGS.get(),
                        ItemRegistry.SOULCOLLECTOR_BOOTS.get()
                ),
                io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get(),
                1,
                0.5f,
                List.of(
                        new MalumSpiritCost("arcane", 8),
                        new MalumSpiritCost("aerial", 8)
                )
        );
    }

    private void saveMalumSpiritInfusionRecipes(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        // Malum の装備IDは optional dependency のため、ResourceLocation で参照して datagen を単独実行可能にする.
        var spirits = List.of(
                new MalumSpiritCost("arcane", 16),
                new MalumSpiritCost("wicked", 16)
        );

        saveMalumSpiritInfusionRecipe(
                recipeWriter,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_infusion/soulcollector_hat"),
                ResourceLocation.fromNamespaceAndPath("malum", "soul_hunter_cloak"),
                itemId(ItemRegistry.SOULCOLLECTOR_HAT.get()),
                List.of(
                        new MalumRecipeItem(itemId(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()), 8),
                        new MalumRecipeItem(itemId(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), 2),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "processed_soulstone"), 4)
                ),
                spirits
        );
        saveMalumSpiritInfusionRecipe(
                recipeWriter,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_infusion/soulcollector_robe"),
                ResourceLocation.fromNamespaceAndPath("malum", "soul_hunter_robe"),
                itemId(ItemRegistry.SOULCOLLECTOR_ROBE.get()),
                List.of(
                        new MalumRecipeItem(itemId(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()), 8),
                        new MalumRecipeItem(itemId(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), 2),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "processed_soulstone"), 4)
                ),
                spirits
        );
        saveMalumSpiritInfusionRecipe(
                recipeWriter,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_infusion/soulcollector_leggings"),
                ResourceLocation.fromNamespaceAndPath("malum", "soul_hunter_leggings"),
                itemId(ItemRegistry.SOULCOLLECTOR_LEGGINGS.get()),
                List.of(
                        new MalumRecipeItem(itemId(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()), 8),
                        new MalumRecipeItem(itemId(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), 2),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "processed_soulstone"), 4)
                ),
                spirits
        );
        saveMalumSpiritInfusionRecipe(
                recipeWriter,
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "malum/spirit_infusion/soulcollector_boots"),
                ResourceLocation.fromNamespaceAndPath("malum", "soul_hunter_boots"),
                itemId(ItemRegistry.SOULCOLLECTOR_BOOTS.get()),
                List.of(
                        new MalumRecipeItem(itemId(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()), 8),
                        new MalumRecipeItem(itemId(io.redspace.ironsspellbooks.registries.ItemRegistry.MAGIC_CLOTH.get()), 2),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "processed_soulstone"), 4)
                ),
                spirits
        );
        saveMalumSpiritInfusionRecipe(
                recipeWriter,
                ResourceLocation.fromNamespaceAndPath(
                        ApprenticeCodex.MODID, "malum/spirit_infusion/soulstained_steel_swingcast_staff"),
                itemId(ItemRegistry.IRON_SWINGCAST_STAFF.get()),
                itemId(ItemRegistry.SOULSTAINED_STEEL_SWINGCAST_STAFF.get()),
                List.of(
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "chunk_of_brilliance"), 8),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "soul_stained_steel_ingot"), 4),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "hex_ash"), 8)
                ),
                List.of(
                        new MalumSpiritCost("wicked", 32),
                        new MalumSpiritCost("arcane", 16),
                        new MalumSpiritCost("eldritch", 8)
                )
        );
        saveMalumSpiritInfusionRecipe(
                recipeWriter,
                ResourceLocation.fromNamespaceAndPath(
                        ApprenticeCodex.MODID, "malum/spirit_infusion/malignant_spellcaster_gun"),
                itemId(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()),
                itemId(ItemRegistry.MALIGNANT_SPELLCASTER_GUN.get()),
                List.of(
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "malignant_pewter_ingot"), 4),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "mnemonic_fragment"), 8),
                        new MalumRecipeItem(ResourceLocation.fromNamespaceAndPath("malum", "void_salts"), 8)
                ),
                List.of(
                        new MalumSpiritCost("wicked", 32),
                        new MalumSpiritCost("arcane", 64),
                        new MalumSpiritCost("eldritch", 32)
                )
        );
    }

    private void saveMalumSpiritInfusionRecipe(
            @NotNull Consumer<FinishedRecipe> recipeWriter,
            @NotNull ResourceLocation recipeId,
            @NotNull ResourceLocation input,
            @NotNull ResourceLocation output,
            @NotNull List<MalumRecipeItem> extraItems,
            @NotNull List<MalumSpiritCost> spirits
    ) {
        recipeWriter.accept(new MalumSpiritInfusionFinishedRecipe(
                recipeId,
                input,
                output,
                extraItems,
                spirits
        ));
    }

    private static ResourceLocation itemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
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
                ApprenticeCodex.MODID,
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

    private void saveAlchemistsFlaskSmithingRecipe(@NotNull Consumer<FinishedRecipe> recipeWriter) {
        var recipeId = ItemRegistry.ALCHEMISTS_FLASK.getId();
        var advancement = Advancement.Builder.recipeAdvancement()
                .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
                .addCriterion(getHasName(ItemRegistry.SPELLCASTERS_FLASK.get()), has(ItemRegistry.SPELLCASTERS_FLASK.get()))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(RequirementsStrategy.OR);

        recipeWriter.accept(new AlchemistsFlaskSmithingFinishedRecipe(
                recipeId,
                Ingredient.of(Items.EMERALD),
                Ingredient.of(ItemRegistry.SPELLCASTERS_FLASK.get()),
                Ingredient.of(Items.GUNPOWDER),
                ItemRegistry.ALCHEMISTS_FLASK.get(),
                advancement,
                recipeId.withPrefix("recipes/" + RecipeCategory.COMBAT.getFolderName() + "/")
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

    private record AlchemistsFlaskSmithingFinishedRecipe(
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
            return RecipeRegistry.ALCHEMISTS_FLASK_SMITHING_SERIALIZER.get();
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

    private record MalumSpellAmplifierFinishedRecipe(ResourceLocation id) implements FinishedRecipe {
        @Override
        public @NotNull ResourceLocation getId() {
            return id;
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            var conditions = new com.google.gson.JsonArray();
            var modLoaded = new JsonObject();
            modLoaded.addProperty("type", "forge:mod_loaded");
            modLoaded.addProperty("modid", "malum");
            conditions.add(modLoaded);
            json.add("conditions", conditions);

            var pattern = new com.google.gson.JsonArray();
            pattern.add("HAH");
            pattern.add(" S ");
            pattern.add(" S ");
            json.add("pattern", pattern);

            var key = new JsonObject();
            key.add("H", itemJson("malum:hex_ash"));
            key.add("A", itemJson("irons_spellbooks:arcane_ingot"));
            key.add("S", itemJson("malum:soul_stained_steel_ingot"));
            json.add("key", key);

            var result = new JsonObject();
            result.addProperty("item", id.toString());
            json.add("result", result);
        }

        private static JsonObject itemJson(String itemId) {
            var item = new JsonObject();
            item.addProperty("item", itemId);
            return item;
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        @Override
        public JsonObject serializeAdvancement() {
            var root = new JsonObject();
            root.addProperty("parent", "minecraft:recipes/root");

            var criteria = new JsonObject();
            var hasIngot = new JsonObject();
            hasIngot.addProperty("trigger", "minecraft:inventory_changed");
            var hasIngotConditions = new JsonObject();
            var predicates = new com.google.gson.JsonArray();
            var predicate = new JsonObject();
            var items = new com.google.gson.JsonArray();
            items.add("malum:soul_stained_steel_ingot");
            predicate.add("items", items);
            predicates.add(predicate);
            hasIngotConditions.add("items", predicates);
            hasIngot.add("conditions", hasIngotConditions);
            criteria.add("has_soul_stained_steel_ingot", hasIngot);

            var hasRecipe = new JsonObject();
            hasRecipe.addProperty("trigger", "minecraft:recipe_unlocked");
            var hasRecipeConditions = new JsonObject();
            hasRecipeConditions.addProperty("recipe", id.toString());
            hasRecipe.add("conditions", hasRecipeConditions);
            criteria.add("has_the_recipe", hasRecipe);
            root.add("criteria", criteria);

            var requirements = new com.google.gson.JsonArray();
            var alternatives = new com.google.gson.JsonArray();
            alternatives.add("has_soul_stained_steel_ingot");
            alternatives.add("has_the_recipe");
            requirements.add(alternatives);
            root.add("requirements", requirements);

            var rewards = new JsonObject();
            var recipes = new com.google.gson.JsonArray();
            recipes.add(id.toString());
            rewards.add("recipes", recipes);
            root.add("rewards", rewards);

            var condition = new JsonObject();
            condition.addProperty("type", "forge:mod_loaded");
            condition.addProperty("modid", "malum");
            var conditions = new com.google.gson.JsonArray();
            conditions.add(condition);
            var entry = new JsonObject();
            entry.add("conditions", conditions);
            entry.add("advancement", root);
            var advancements = new com.google.gson.JsonArray();
            advancements.add(entry);
            var conditionalRoot = new JsonObject();
            conditionalRoot.add("advancements", advancements);
            return conditionalRoot;
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return ResourceLocation.fromNamespaceAndPath(
                    id.getNamespace(), "recipes/combat/" + id.getPath());
        }
    }

    private record MalumSpiritInfusionFinishedRecipe(
            ResourceLocation id,
            ResourceLocation input,
            ResourceLocation output,
            List<MalumRecipeItem> extraItems,
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

            json.addProperty("type", "malum:spirit_infusion");

            var extraItemsJson = new com.google.gson.JsonArray();
            for (var extraItem : extraItems) {
                extraItemsJson.add(extraItem.toJson());
            }
            json.add("extra_items", extraItemsJson);
            json.add("input", itemStackJson(input, 1));
            json.add("output", itemStackJson(output, 1));

            var spiritArray = new com.google.gson.JsonArray();
            for (var spirit : spirits) {
                spiritArray.add(spirit.toJson());
            }
            json.add("spirits", spiritArray);
        }

        private static JsonObject itemStackJson(ResourceLocation item, int count) {
            var json = new JsonObject();
            json.addProperty("item", item.toString());
            if (count > 1) {
                json.addProperty("count", count);
            }
            return json;
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

    private record MalumRecipeItem(ResourceLocation item, int count) {
        private JsonObject toJson() {
            return MalumSpiritInfusionFinishedRecipe.itemStackJson(item, count);
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
