package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class PhotonSiphon extends AbstractOffhandMagicItem implements UniqueItem {
    public PhotonSiphon() {
        // todo:専用魔法を作る.
        super(
                SpellRegistry.MAGE_LIGHT,
                1,
                "photon_siphon",
                bonus(AttributeRegistry.MANA_REGEN, 1.00D, AttributeModifier.Operation.MULTIPLY_BASE)
        );
    }
}
