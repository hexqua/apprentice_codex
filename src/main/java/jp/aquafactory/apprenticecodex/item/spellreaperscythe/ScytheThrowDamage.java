package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityDamageMemoryAccessor;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ScytheThrowDamage {
    private ScytheThrowDamage() {}

    public static void hit(ServerLevel level, Entity projectile, Player owner, Entity target,
                           ItemStack weapon, float physical, float magic, boolean continuous) {
        target = CombatTools.resolutePartEntity(target);
        if (!(target instanceof LivingEntity living) || !CombatTools.isValidCombatTarget(target, owner)) return;
        var source = CombatTools.getDamageSource(level, projectile, owner, continuous
                ? DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS : DamageTypes.SPELL_REAPER_SCYTHE_THROW);
        // damage効果だけを評価し、耐久消費・炎上・スイープ等の近接post-attack処理は呼ばない。
        var amount = EnchantmentHelper.modifyDamage(level, weapon, target, source, physical);
        var multiplier = continuous ? 0.1f : 1f;
        var memory = (LivingEntityDamageMemoryAccessor) living;
        var savedTime = living.invulnerableTime;
        var savedDamage = memory.apprenticecodex$getLastHurt();
        try {
            var success = CombatTools.applyUnscaledDamage(target, amount * multiplier, source, continuous
                    ? CombatTools.KnockbackTypes.NO_KNOCKBACK : CombatTools.KnockbackTypes.DEFAULT);
            if (!continuous) {
                savedTime = living.invulnerableTime;
                savedDamage = memory.apprenticecodex$getLastHurt();
            }
            if (success && magic > 0 && !living.isDeadOrDying()) {
                var magicSource = CombatTools.getDamageSource(level, projectile, owner, continuous
                        ? DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS_MAGIC : DamageTypes.SPELL_REAPER_SCYTHE_THROW_MAGIC);
                CombatTools.applyUnscaledDamage(target, magic * multiplier, magicSource, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            }
        } finally {
            // 追加成分で通常攻撃のlastHurtを小さくしたり、停滞で無敵時間を延長したりしない。
            living.invulnerableTime = savedTime;
            memory.apprenticecodex$setLastHurt(savedDamage);
        }
    }
}
