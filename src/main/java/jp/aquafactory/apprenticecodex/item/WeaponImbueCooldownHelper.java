package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class WeaponImbueCooldownHelper {
    private WeaponImbueCooldownHelper() {
    }

    public static int getEffectiveSpellCooldown(
            AbstractSpell spell,
            Player player,
            CastSource castSource,
            @Nullable ItemStack castingStack
    ) {
        if (!shouldIgnoreWeaponImbueCooldownMultiplier(castingStack, spell, castSource)) {
            return MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        }

        var playerCooldownModifier = player.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION);
        return (int) (spell.getSpellCooldown() * (2 - Utils.softCapFormula(playerCooldownModifier)));
    }

    public static boolean shouldIgnoreWeaponImbueCooldownMultiplier(
            @Nullable ItemStack stack,
            @Nullable AbstractSpell spell,
            CastSource castSource
    ) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof WeaponImbueCooldownPolicyItem policyItem
                && policyItem.ignoresWeaponImbueCooldownMultiplier(stack, spell, castSource);
    }
}
