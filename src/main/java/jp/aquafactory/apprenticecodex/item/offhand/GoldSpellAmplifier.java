package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class GoldSpellAmplifier extends AbstractOffhandMagicItem {
    public GoldSpellAmplifier() {
        super(
                "gold_spell_amplifier",
                bonus(AttributeRegistry.MANA_REGEN, 0.50D, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }
}
