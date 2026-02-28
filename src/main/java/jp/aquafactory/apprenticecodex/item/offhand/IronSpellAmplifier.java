package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class IronSpellAmplifier extends AbstractOffhandMagicItem {
    public IronSpellAmplifier() {
        super(
                "iron_spell_amplifier",
                bonus(AttributeRegistry.MAX_MANA, 50.0, AttributeModifier.Operation.ADDITION),
                bonus(AttributeRegistry.MANA_REGEN, -0.10, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }
}
