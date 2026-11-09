package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

final class PotionTestSupport {
    private PotionTestSupport() {}

    static FluidStack createCreateManaPotionFluid(Potion potion, int amountMb) {
        var createPotion = ForgeRegistries.FLUIDS.getValue(ResourceLocation.fromNamespaceAndPath("create", "potion"));
        if (createPotion == null) {
            return FluidStack.EMPTY;
        }

        var fluid = new FluidStack(createPotion, amountMb);
        var tag = fluid.getOrCreateTag();
        tag.putString("Potion", BuiltInRegistries.POTION.getKey(potion).toString());
        tag.putString("Bottle", "REGULAR");
        return fluid;
    }
}
