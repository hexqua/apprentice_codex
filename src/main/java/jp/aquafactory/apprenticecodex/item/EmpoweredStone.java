package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

//　オフハンドサンプルアイテム.
public class EmpoweredStone extends AbstractOffhandMagicItem {
    public EmpoweredStone() {
        super(
                SpellRegistry.MAGE_LIGHT,
                1,
                "empowered_stone",
                bonus(AttributeRegistry.MAX_MANA, 100.0D, AttributeModifier.Operation.ADDITION),
                bonus(AttributeRegistry.MANA_REGEN, 0.10D, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }
}
