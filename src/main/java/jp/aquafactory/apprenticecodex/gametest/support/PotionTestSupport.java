package jp.aquafactory.apprenticecodex.gametest.support;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.fluids.FluidStack;

public final class PotionTestSupport {
    private PotionTestSupport() {}

    public static ItemStack createInstantManaPotion(Potion potion) {
        return PotionContentsHelper.createPotionStack(Items.POTION, potion);
    }

    static FluidStack createIronsManaPotionFluid(Potion potion, int amountMb) {
        var fluid = PotionFluid.from(PotionContentsHelper.createPotionStack(Items.POTION, potion));
        fluid.setAmount(amountMb);
        return fluid;
    }

    static FluidStack createCreateManaPotionFluid(Potion potion, int amountMb) {
        var createPotion = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("create", "potion"));
        if (createPotion == null) {
            return FluidStack.EMPTY;
        }

        var fluid = new FluidStack(createPotion, amountMb);
        fluid.set(DataComponents.POTION_CONTENTS, PotionContentsHelper.createPotionStack(Items.POTION, potion).get(DataComponents.POTION_CONTENTS));
        return fluid;
    }
}
