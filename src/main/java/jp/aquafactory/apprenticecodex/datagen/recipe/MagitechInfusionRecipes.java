package jp.aquafactory.apprenticecodex.datagen.recipe;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.recipe.InfusionRecipe;

import java.util.List;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.GRAYBEARD_STAFF;

public final class MagitechInfusionRecipes {
    private MagitechInfusionRecipes() {
    }

    public static void generate(RecipeOutput output) {
        // 生成時はMagitechを読み込むが、配布先では未導入でもレシピの解析を省略できるようにする。
        output.withConditions(new ModLoadedCondition("magitech")).accept(
                ItemRegistry.MAGI_ENGINEERS_STAFF.getId(),
                new InfusionRecipe("", SizedIngredient.of(GRAYBEARD_STAFF.get(), 1), List.of(
                        SizedIngredient.of(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(), 8),
                        SizedIngredient.of(ItemInit.HIGH_PURITY_FLUORITE.get(), 8),
                        SizedIngredient.of(ItemInit.FLUXIUM_INGOT.get(), 8)
                ), 500_000L, new ItemStack(ItemRegistry.MAGI_ENGINEERS_STAFF.get())),
                null);
    }
}
