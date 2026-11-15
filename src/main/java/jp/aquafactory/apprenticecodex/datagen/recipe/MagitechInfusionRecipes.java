package jp.aquafactory.apprenticecodex.datagen.recipe;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.recipe.InfusionRecipe;

import java.util.List;

public final class MagitechInfusionRecipes {
    private MagitechInfusionRecipes() {
    }

    public static void generate(RecipeOutput output) {
        // 生成時はMagitechを読み込むが、配布先では未導入でもレシピの解析を省略できるようにする。
        output.withConditions(new ModLoadedCondition("magitech")).accept(
                ItemRegistry.MAGI_ENGINEERS_STAFF.getId(),
                new InfusionRecipe("", SizedIngredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.GRAYBEARD_STAFF.get(), 1), List.of(
                        SizedIngredient.of(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(), 8),
                        SizedIngredient.of(ItemInit.HIGH_PURITY_FLUORITE.get(), 8),
                        SizedIngredient.of(ItemInit.FLUXIUM_INGOT.get(), 8)
                ), 500_000L, new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get())),
                null);

        output.withConditions(new ModLoadedCondition("magitech")).accept(
                ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.getId(),
                new InfusionRecipe("", SizedIngredient.of(ItemRegistry.MANA_THRUSTER.get(), 1), List.of(
                        SizedIngredient.of(ItemInit.RADIANT_STEEL_INGOT.get(), 2),
                        SizedIngredient.of(io.redspace.ironsspellbooks.registries.ItemRegistry.DIVINE_SOULSHARD.get(), 1),
                        SizedIngredient.of(ItemInit.FLAMGLIDE_STRIDER.get(), 1),
                        SizedIngredient.of(ItemInit.AETHER_LIFTER.get(), 1)
                ), 1_000_000L, new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get())),
                null);
    }
}
