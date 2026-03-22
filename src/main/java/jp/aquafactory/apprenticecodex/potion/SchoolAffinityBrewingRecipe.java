package jp.aquafactory.apprenticecodex.potion;

import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class SchoolAffinityBrewingRecipe implements IBrewingRecipe {
    @Override
    public boolean isInput(ItemStack input) {
        if (input.isEmpty() || input.getCount() != 1) {
            return false;
        }

        var potion = PotionUtils.getPotion(input);
        if (potion == PotionRegistry.INTELLIGENCE.get()
                || potion == PotionRegistry.LONG_INTELLIGENCE.get()
                || potion == PotionRegistry.STRONG_INTELLIGENCE.get()) {
            return true;
        }

        return SchoolAffinityRegistry.findByPotion(potion).isPresent();
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        if (ingredient.isEmpty()) {
            return false;
        }

        return ingredient.is(Items.REDSTONE)
                || ingredient.is(Items.GLOWSTONE_DUST)
                || SchoolAffinityRegistry.findByCatalyst(ingredient).isPresent();
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }

        var inputPotion = PotionUtils.getPotion(input);

        if (inputPotion == PotionRegistry.INTELLIGENCE.get()
                || inputPotion == PotionRegistry.LONG_INTELLIGENCE.get()
                || inputPotion == PotionRegistry.STRONG_INTELLIGENCE.get()) {
            var definition = SchoolAffinityRegistry.findByCatalyst(ingredient).orElse(null);
            if (definition == null) {
                return ItemStack.EMPTY;
            }

            if (inputPotion == PotionRegistry.INTELLIGENCE.get()) {
                return createOutput(input, definition.basePotion());
            }
            if (inputPotion == PotionRegistry.LONG_INTELLIGENCE.get()) {
                return createOutput(input, definition.longPotion());
            }
            return createOutput(input, definition.strongPotion());
        }

        var definition = SchoolAffinityRegistry.findByPotion(inputPotion).orElse(null);
        if (definition == null) {
            return ItemStack.EMPTY;
        }

        if (ingredient.is(Items.REDSTONE) && inputPotion == definition.basePotion()) {
            return createOutput(input, definition.longPotion());
        }
        if (ingredient.is(Items.GLOWSTONE_DUST) && inputPotion == definition.basePotion()) {
            return createOutput(input, definition.strongPotion());
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack createOutput(ItemStack input, net.minecraft.world.item.alchemy.Potion potion) {
        var output = input.copy();
        output.setCount(1);
        PotionUtils.setPotion(output, potion);
        return output;
    }
}
