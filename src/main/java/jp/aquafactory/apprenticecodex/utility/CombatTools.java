package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.event.KnockbackControlEvent;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;

public final class CombatTools {
    private CombatTools() {}

    public enum KnockbackTypes {
        DEFAULT,
        NO_KNOCKBACK,
    }

    private static volatile long lastEpicFightCompatLogMs = 0L;
    private static final long EPICFIGHT_COMPAT_LOG_COOLDOWN_MS = 10_000L;

    public static DamageSource getDamageSource(Level level, Entity entity, ResourceKey<DamageType> damageType) {
        var reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = reg.getHolder(damageType)
                .orElseGet(() -> (Holder.Reference<DamageType>) level.damageSources().genericKill().typeHolder());

        return new DamageSource(holder, entity);
    }

    public static DamageSource getDamageSource(Level level, Entity projectile, Entity owner, ResourceKey<DamageType> damageType) {
        var reg = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = reg.getHolder(damageType)
                .orElseGet(() -> (Holder.Reference<DamageType>) level.damageSources().genericKill().typeHolder());

        return new DamageSource(holder, projectile, owner);
    }

    public static Entity resolutePartEntity(Entity raw) {
        // パーツ系モブの解決.
        if (raw instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return raw;
    }

    public static boolean isValidCombatTarget(Entity target, @Nullable Entity owner) {
        if (target == owner) return false;

        // 例外的に対象にする特殊エンティティを指定.
        if (target instanceof EndCrystal) return true;

        // エンダードラゴン系の解決は内部で行う.
        var resolvedTarget = resolutePartEntity(target);

        // 基本的にはLivingEntityのみを対象.
        return resolvedTarget instanceof LivingEntity;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean applyDamage(Entity target, float baseAmount, DamageSource source, SchoolType magicSchool,
                                      KnockbackTypes type) {
        if (target instanceof LivingEntity livingTarget) {
            var multicastAdjustment = MulticastEchoStaffAttackHandler.adjustCombatDamage(target, baseAmount, source);
            baseAmount = multicastAdjustment.baseAmount();

            // Iron'sの召喚ダメージはLivingEntityのみ対象なので合わせる.
            if (source.is(DamageTypeTagGenerator.SUMMON_DAMAGE) && source.getEntity() instanceof LivingEntity caster){
                var attributeInstance = caster.getAttribute(AttributeRegistry.SUMMON_DAMAGE);
                if (attributeInstance != null){
                    baseAmount *= (float) attributeInstance.getValue();
                }
            }

            if (type == KnockbackTypes.NO_KNOCKBACK) {
                KnockbackControlEvent.markIgnoreNextKnockback(livingTarget);
            }

            var amount = baseAmount * getResistAttribute(livingTarget, magicSchool);

            // Epicfight関連は例外握りつぶしを行う.
            boolean applied;
            if (isEpicFightLikeEnvironment()) {
                try {
                    applied = livingTarget.hurt(source, amount);
                } catch (Throwable t) {
                    logEpicFightCompatOncePerInterval("LivingEntity#hurt", livingTarget, source, amount, t);
                    return false;
                }
            } else {
                applied = livingTarget.hurt(source, amount);
            }

            if (applied && multicastAdjustment.ignoreIframe()) {
                livingTarget.invulnerableTime = multicastAdjustment.postHitIframeTicks();
            }
            return applied;

        } else {
            var multicastAdjustment = MulticastEchoStaffAttackHandler.adjustCombatDamage(target, baseAmount, source);
            baseAmount = multicastAdjustment.baseAmount();
            return target.hurt(source, baseAmount);
        }
    }

    private static boolean isEpicFightLikeEnvironment() {
        // Epicfight及びそれのでかいアドオンを監視.
        return ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("efn");
    }

    private static void logEpicFightCompatOncePerInterval(
            String stage, Entity target, DamageSource source, float amount, Throwable t
    ) {
        var now = System.currentTimeMillis();
        var last = lastEpicFightCompatLogMs;

        if (now - last < EPICFIGHT_COMPAT_LOG_COOLDOWN_MS) {
            return;
        }

        lastEpicFightCompatLogMs = now;
        ApprenticeCodex.LOGGER.warn(
                "[Compat] Suppressed crash from other mod during {}. " +
                        "This often happens with EpicFight / EpicFight Nightfall / Connector environments. " +
                        "target={}, source={}, amount={}",
                stage, target, source, amount, t
        );
    }

    public static boolean canBeHostileToMe(Entity target, LivingEntity player) {
        if (player == null || target == null) return false;
        if (target == player) return false;
        if (!target.isAlive()) return false;

        if (player.isAlliedTo(target) || target.isAlliedTo(player)) {
            return false;
        }
        if (target instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) {
            return false;
        }

        if (target instanceof Enemy){
            return true;
        }

        if (player.getLastHurtByMob() == target){
            return true;
        }

        if (target instanceof Mob mob && mob.getTarget() == player){
            return true;
        }

        if (target instanceof NeutralMob neutral) {
            var angerTarget = neutral.getPersistentAngerTarget();
            if (angerTarget != null && angerTarget.equals(player.getUUID())){
                return true;
            }

            return neutral.isAngryAt(player);
        }

        return false;
    }

    private static float getResistAttribute(LivingEntity entity, SchoolType damageSchool) {
        var baseResist = entity.getAttributeValue(AttributeRegistry.SPELL_RESIST);
        if (damageSchool == null) {
            return 2 - (float) Utils.softCapFormula(baseResist);
        } else {
            return 2 - (float) Utils.softCapFormula(damageSchool.getResistanceFor(entity) * baseResist);
        }
    }

    public static boolean isHeadShot(LivingEntity target, Vec3 hitPosition) {
        // todo:今はLivingEntity全てにしているが、これに制限を入れるかどうかを検討.
        var height = target.getBoundingBox().getYsize();
        var headShotMargin = Math.max(height / 3, 0.5);
        var eyeY = target.getEyeY();

        // 判定は気持ちよさ優先でやや雑に.
        return (!(hitPosition.y <= eyeY - headShotMargin / 2)) && (!(hitPosition.y >= eyeY + headShotMargin / 2));
    }
}

