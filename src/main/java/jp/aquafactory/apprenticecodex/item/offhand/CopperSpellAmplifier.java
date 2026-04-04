package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;

public class CopperSpellAmplifier extends AbstractSpellAmplifierItem {
    public CopperSpellAmplifier() {
        super(
                Rarity.UNCOMMON,
                "copper_spell_amplifier",
                bonus(AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                bonus(AttributeRegistry.LIGHTNING_SPELL_POWER, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
