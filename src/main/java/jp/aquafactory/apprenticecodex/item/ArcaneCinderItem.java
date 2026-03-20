package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public class ArcaneCinderItem extends Item {
    private static final int BURN_TIME = 2400;

    public ArcaneCinderItem() {
        super(new Item.Properties());
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return BURN_TIME;
    }
}
