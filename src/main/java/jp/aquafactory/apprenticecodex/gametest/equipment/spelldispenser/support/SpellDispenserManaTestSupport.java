package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.fluids.FluidStack;

public final class SpellDispenserManaTestSupport {
    private SpellDispenserManaTestSupport() {}

    public static ItemStack createInstantManaPotion(Potion potion) {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
    }

    static FluidStack createIronsManaPotionFluid(Potion potion, int amountMb) {
        var fluid = PotionFluid.from(PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
        fluid.setAmount(amountMb);
        return fluid;
    }

    public static ItemStack createFilledSpellcastersFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }
}
