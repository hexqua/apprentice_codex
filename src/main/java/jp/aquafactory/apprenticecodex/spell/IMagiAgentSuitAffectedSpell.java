package jp.aquafactory.apprenticecodex.spell;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface IMagiAgentSuitAffectedSpell {
    default boolean canBeInterruptedWithMagiAgentSuit(
            AbstractSpell spell,
            @Nullable Player player,
            boolean originalCanBeInterrupted
    ) {
        return MagiAgentSuitEffects.canBeInterrupted(spell, player, originalCanBeInterrupted);
    }
}
