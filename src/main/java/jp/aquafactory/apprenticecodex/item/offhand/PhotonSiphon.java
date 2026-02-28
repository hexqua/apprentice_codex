package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class PhotonSiphon extends AbstractOffhandMagicItem implements UniqueItem {
    public PhotonSiphon() {
        super(
                SpellRegistry.MANA_CHARGE,
                1,
                "photon_siphon",
                bonus(AttributeRegistry.MAX_MANA, 100.0D, AttributeModifier.Operation.ADDITION)
        );
    }
}
