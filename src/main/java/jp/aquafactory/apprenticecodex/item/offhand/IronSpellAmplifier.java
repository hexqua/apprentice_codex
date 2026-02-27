package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class IronSpellAmplifier extends AbstractOffhandMagicItem {
    public IronSpellAmplifier() {
        super(
                "iron_spell_amplifier",
                bonus(AttributeRegistry.MAX_MANA, 50.0D, AttributeModifier.Operation.ADDITION),
                bonus(AttributeRegistry.MANA_REGEN, -0.05D, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }
}
