package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import net.minecraft.world.entity.player.Player;

public final class WeaponImbueCooldownHelper {
    private WeaponImbueCooldownHelper() {
    }

    public static int getEffectiveSpellCooldown(
            AbstractSpell spell,
            Player player,
            CastSource castSource
    ) {
        var baseCooldown = resolveLimitedBaseCooldown(spell, player);
        var playerCooldownModifier = player.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION);
        var itemCooldownModifier = castSource == CastSource.SWORD ? ServerConfigs.SWORDS_CD_MULTIPLIER.get().floatValue() : 1.0f;
        return (int) (baseCooldown * (2 - Utils.softCapFormula(playerCooldownModifier)) * itemCooldownModifier);
    }

    private static int resolveLimitedBaseCooldown(AbstractSpell spell, Player player) {
        var baseCooldown = Math.max(0, spell.getSpellCooldown());
        if (baseCooldown == 0) {
            return 0;
        }

        var craftsmansDelightCooldown = CraftsmansDelightSpellSupport.isCooldownReductionTarget(spell)
                ? CraftsmansDelight.applyCooldownDiscount(baseCooldown, player)
                : baseCooldown;
        var magiAgentSuitCooldown = MagiAgentSuitEffects.applyBootsCooldownDiscount(baseCooldown, spell, player);
        return selectStrongestLimitedBaseCooldown(baseCooldown, craftsmansDelightCooldown, magiAgentSuitCooldown);
    }

    public static int selectStrongestLimitedBaseCooldown(int baseCooldown, int... candidateCooldowns) {
        var selectedCooldown = Math.max(0, baseCooldown);
        if (selectedCooldown == 0) {
            return 0;
        }

        for (var candidateCooldown : candidateCooldowns) {
            if (candidateCooldown > 0 && candidateCooldown < selectedCooldown) {
                selectedCooldown = candidateCooldown;
            }
        }
        return selectedCooldown;
    }
}
