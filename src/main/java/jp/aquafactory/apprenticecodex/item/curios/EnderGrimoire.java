package jp.aquafactory.apprenticecodex.item.curios;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class EnderGrimoire extends SpellBook {
    public EnderGrimoire() {
        super(12);
        // マナ上昇はエンドコンテンツ系なので200提供、あとの追加は機能寄せなので能力としては無し.
        withSpellbookAttributes(
                new AttributeContainer(
                        AttributeRegistry.MAX_MANA,
                        200,
                        AttributeModifier.Operation.ADDITION
                )
        );
    }
}
