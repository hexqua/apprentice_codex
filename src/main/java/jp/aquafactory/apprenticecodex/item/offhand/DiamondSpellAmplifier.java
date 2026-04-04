package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class DiamondSpellAmplifier extends AbstractSpellAmplifierItem {
    public DiamondSpellAmplifier() {
        super(
                Rarity.UNCOMMON,
                "diamond_spell_amplifier",
                bonus(AttributeRegistry.MANA_REGEN, 0.20, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 10;
    }
}
