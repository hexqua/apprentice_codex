package jp.aquafactory.apprenticecodex.item.spellthrowablecard;

import jp.aquafactory.apprenticecodex.entity.spellthrowablecard.AbstractSpellThrowableCardEntity;
import jp.aquafactory.apprenticecodex.entity.spellthrowablecard.SpellInvokeCardEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SpellInvokeCard extends AbstractSpellThrowableCardItem {
    @Override
    protected AbstractSpellThrowableCardEntity createProjectile(Level level, Player owner, ItemStack thrownStack) {
        return new SpellInvokeCardEntity(EntityRegistry.SPELL_INVOKE_CARD.get(), level, owner, thrownStack);
    }
}
