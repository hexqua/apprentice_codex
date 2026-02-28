package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class CopperSpellAmplifier extends AbstractOffhandMagicItem {
    public CopperSpellAmplifier() {
        super(
                "copper_spell_amplifier",
                bonus(AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE),
                bonus(AttributeRegistry.LIGHTNING_SPELL_POWER, 0.05, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
