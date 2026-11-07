package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import io.redspace.ironsspellbooks.fluids.PotionFluid;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.fluids.FluidStack;

public final class SpellDispenserManaTestSupport {
    private SpellDispenserManaTestSupport() {}

    public static ItemStack createInstantManaPotion(Potion potion) {
        return PotionContentsHelper.createPotionStack(Items.POTION, potion);
    }

    public static ItemStack createFilledSpellcastersFlask(
            RegistryAccess registryAccess,
            ItemStack storedItem,
            int doseCount,
            int glowEnergyLevel
    ) {
        var flask = new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get());
        if (glowEnergyLevel > 0) {
            var enchantment = registryAccess.lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.GLOW_ENERGY);
            flask.enchant(enchantment, glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }


    public static FluidStack createIronsManaPotionFluid(Potion potion, int amountMb) {
        var fluid = PotionFluid.from(PotionContentsHelper.createPotionStack(Items.POTION, potion));
        fluid.setAmount(amountMb);
        return fluid;
    }
}
