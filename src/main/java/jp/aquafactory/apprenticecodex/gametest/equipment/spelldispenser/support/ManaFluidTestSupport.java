package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.fluids.FluidStack;

public final class ManaFluidTestSupport {
    private ManaFluidTestSupport() {}

    public static FluidStack createIronsManaPotionFluid(Potion potion, int amountMb) {
        var fluid = PotionFluid.from(PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
        fluid.setAmount(amountMb);
        return fluid;
    }
}
