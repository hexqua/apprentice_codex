package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunCastContext;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = AbstractSpell.class, priority = 900, remap = false)
public abstract class AbstractSpellManaBypassMixin {
    @ModifyVariable(
            method = "canBeCastedBy",
            at = @At("STORE"),
            require = 0,
            expect = 1,
            remap = false,
            name = "playerMana")
    private float apprentice_codex$ensureSpellgunManaThreshold(
            float visibleMana,
            int spellLevel,
            CastSource castSource,
            MagicData playerMagicData,
            Player player
    ) {
        var spell = (AbstractSpell) (Object) this;
        // 外部 MOD が換算した可視マナを破棄せず、最大マナ迂回中の Spellgun に必要な下限だけを合成する。
        return SpellgunCastContext.shouldBypassManaCheck(spell, player)
                ? Math.max(visibleMana, spell.getManaCost(spellLevel))
                : visibleMana;
    }
}
