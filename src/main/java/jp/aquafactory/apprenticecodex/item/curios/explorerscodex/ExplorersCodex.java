package jp.aquafactory.apprenticecodex.item.curios.explorerscodex;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellDataRegistryHolder;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import jp.aquafactory.apprenticecodex.item.curios.FireResistantUniqueSpellBook;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ExplorersCodex extends FireResistantUniqueSpellBook {
    public ExplorersCodex() {
        super(SpellDataRegistryHolder.of(
                new SpellDataRegistryHolder(SpellRegistry.ASSIST_WINGS, 1),
                new SpellDataRegistryHolder(SpellRegistry.TERRA_RESONANCE, 1),
                new SpellDataRegistryHolder(SpellRegistry.SENSE_EVIL, 1),
                new SpellDataRegistryHolder(SpellRegistry.REMOTE_EYE, 1)
        ));
        withSpellbookAttributes(new AttributeContainer(
                AttributeRegistry.MAX_MANA,
                50,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

}
