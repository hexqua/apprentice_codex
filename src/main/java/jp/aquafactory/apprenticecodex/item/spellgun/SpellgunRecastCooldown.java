package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleCastContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** 初回の即時化コストをRecastと同じ寿命で保持し、持ち替え後も一度だけ適用する。 */
public record SpellgunRecastCooldown(boolean fullauto, int castTime) {
    public interface Holder {
        @Nullable SpellgunRecastCooldown apprenticecodex$getCooldown();
        void apprenticecodex$setCooldown(@Nullable SpellgunRecastCooldown cooldown);
    }

    public static @Nullable SpellgunRecastCooldown capture(ServerPlayer player, MagicData magic, RecastInstance recast) {
        if (!recast.getSpellId().equals(magic.getCastingSpellId())) return null;
        var stack = magic.getPlayerCastingItem();
        var spell = SpellRegistry.getSpell(recast.getSpellId());
        boolean fullauto = stack.getItem() instanceof FullautoRapidcastSpellrifle
                && FullautoRapidcastSpellrifleCastContext.isActiveFor(player.getUUID(), stack, spell);
        boolean longGun = stack.getItem() instanceof AbstractSpellGunItem gun && gun.addsInstantCastTimeToCooldown();
        if (!fullauto && !longGun) return null;
        int castTime = spell.getCastType() == CastType.LONG
                ? spell.getEffectiveCastTime(recast.getSpellLevel(), player) : 0;
        return new SpellgunRecastCooldown(fullauto, castTime);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("fullauto", fullauto);
        tag.putInt("castTime", castTime);
        return tag;
    }

    public static SpellgunRecastCooldown load(CompoundTag tag) {
        return new SpellgunRecastCooldown(tag.getBoolean("fullauto"), Math.max(0, tag.getInt("castTime")));
    }
}
