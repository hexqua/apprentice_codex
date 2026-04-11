package jp.aquafactory.apprenticecodex.item.curios.circlets;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Rarity;

public class AshenCirclet extends AbstractCircletItem {
    private static final AttributeContainer[] CIRCLET_ATTRIBUTES = {
            new AttributeContainer(
                    AttributeRegistry.MAX_MANA,
                    100,
                    AttributeModifier.Operation.ADD_VALUE
            ),
            new AttributeContainer(
                    AttributeRegistry.MANA_REGEN,
                    0.20D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ),
            new AttributeContainer(
                    Attributes.ATTACK_DAMAGE,
                    -0.20D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            )
    };

    public AshenCirclet() {
        super(Rarity.UNCOMMON, CIRCLET_ATTRIBUTES);
    }
}
