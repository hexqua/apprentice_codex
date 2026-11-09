package jp.aquafactory.apprenticecodex.gametest.equipment.flask;

import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ItemStack;

public final class FlaskDoseTestSupport {
    private FlaskDoseTestSupport() {}

    public static ItemStack createFilledAlchemistsFlask(ItemStack storedItem, int doseCount, int glowEnergyLevel) {
        var flask = new ItemStack(ItemRegistry.ALCHEMISTS_FLASK.get());
        if (EnchantmentRegistry.GLOW_ENERGY.isPresent() && glowEnergyLevel > 0) {
            flask.enchant(EnchantmentRegistry.GLOW_ENERGY.get(), glowEnergyLevel);
        }
        return SpellcastersFlask.copyWithAddedDoses(flask, storedItem, doseCount);
    }
}
