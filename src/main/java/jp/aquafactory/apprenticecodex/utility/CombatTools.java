package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.event.KnockbackControlEvent;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public final class CombatTools {
    private CombatTools() {}

    public enum KnockbackTypes {
        DEFAULT,
        NO_KNOCKBACK,
    }

    public enum CombatTargetPolicy {
        PROTECT_SELF_AND_ALLIES(false),
        ALLOW_SELF_PROTECT_ALLIES(true);

        private final boolean allowsSelfDamage;

        CombatTargetPolicy(boolean allowsSelfDamage) {
            this.allowsSelfDamage = allowsSelfDamage;
        }

        public boolean allowsSelfDamage() {
            return allowsSelfDamage;
        }
    }

    private static volatile long lastEpicFightCompatLogMs = 0L;
    private static final long EPICFIGHT_COMPAT_LOG_COOLDOWN_MS = 10_000L;

    public static boolean isFireResistant(LivingEntity target) {
        return target.hasEffect(MobEffects.FIRE_RESISTANCE) || target.fireImmune();
    }

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

    public static List<Entity> resolveUniqueCombatTargets(Iterable<? extends Entity> rawTargets) {
        var resolvedTargets = new ArrayList<Entity>();
        var resolvedIds = new HashSet<UUID>();

        for (var rawTarget : rawTargets) {
            var resolvedTarget = resolutePartEntity(rawTarget);
            if (resolvedTarget.isAlive() && resolvedIds.add(resolvedTarget.getUUID())) {
                resolvedTargets.add(resolvedTarget);
            }
        }

        return resolvedTargets;
    }

    public static boolean isValidCombatTarget(Entity target, @Nullable Entity owner) {
        return isValidCombatTarget(target, owner, CombatTargetPolicy.PROTECT_SELF_AND_ALLIES);
    }

    public static boolean isValidCombatTarget(Entity target, @Nullable Entity owner, CombatTargetPolicy policy) {
        var resolvedTarget = resolutePartEntity(target);
        var combatOwner = resolveCombatActor(owner);

        if (resolvedTarget == combatOwner && !policy.allowsSelfDamage()) return false;

        // 例外的に対象にする特殊エンティティを指定.
        if (resolvedTarget instanceof EndCrystal) return true;

        // 箒を含む車両はバニラボート等と同じ非LivingEntityとして対象外にし、
        // 騎乗者への攻撃可否は騎乗者自身のPvP・team friendly fire判定へ委ねる。
        return resolvedTarget instanceof LivingEntity && !isProtectedCombatTarget(resolvedTarget, combatOwner, policy);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean applyDamage(Entity target, float baseAmount, DamageSource source, SchoolType magicSchool,
                                      KnockbackTypes type) {
        return applyDamage(target, baseAmount, source, magicSchool, type, CombatTargetPolicy.PROTECT_SELF_AND_ALLIES);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean applyDamage(Entity target, float baseAmount, DamageSource source, SchoolType magicSchool,
                                      KnockbackTypes type, CombatTargetPolicy policy) {
        var resolvedTarget = resolutePartEntity(target);
        var combatOwner = resolveDamageOwner(source);
        if (isProtectedCombatTarget(resolvedTarget, combatOwner, policy)) {
            return false;
        }

        target = resolvedTarget;
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

            var suppressKnockback = type == KnockbackTypes.NO_KNOCKBACK;
            if (suppressKnockback) {
                KnockbackControlEvent.markIgnoreNextKnockback(livingTarget);
            }

            var amount = baseAmount * getResistAttribute(livingTarget, magicSchool);

            boolean applied;
            try {
                // Epicfight関連は例外握りつぶしを行う.
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
            } finally {
                if (suppressKnockback) {
                    // hurtが早期終了してイベントを発火しない場合も、後続の無関係なノックバックを抑止しない.
                    KnockbackControlEvent.clearIgnoreNextKnockback(livingTarget);
                }
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

    public static boolean isProtectedCombatTarget(Entity target, @Nullable Entity owner, CombatTargetPolicy policy) {
        if (owner == null) {
            return false;
        }

        if (target == owner) {
            return !policy.allowsSelfDamage();
        }

        // 同じroot vehicleに属する車両本体と全同乗者を保護し、複数席のMOD車両にも対応する.
        if (owner.getRootVehicle() == target.getRootVehicle()) {
            return true;
        }

        if (isOwnedBy(target, owner)) {
            return true;
        }

        if (owner instanceof Player playerOwner && target instanceof Player playerTarget
                && !playerOwner.canHarmPlayer(playerTarget)) {
            return true;
        }

        var ownerTeam = owner.getTeam();
        var targetTeam = target.getTeam();
        var scoreboardAllied = ownerTeam != null && ownerTeam.isAlliedTo(targetTeam);
        if (scoreboardAllied) {
            return !ownerTeam.isAllowFriendlyFire();
        }

        // scoreboard teamは上でfriendly fire設定を確定済み。ここでは所有・勢力由来のoverrideだけを拾う.
        return owner.isAlliedTo(target) || target.isAlliedTo(owner);
    }

    private static boolean isOwnedBy(Entity target, Entity owner) {
        if (target instanceof OwnableEntity ownable) {
            var ownerUuid = ownable.getOwnerUUID();
            if (ownerUuid != null && ownerUuid.equals(owner.getUUID())) {
                return true;
            }
        }

        if (target instanceof IMagicSummon summon) {
            var summoner = summon.getSummoner();
            if (summoner != null && summoner.getUUID().equals(owner.getUUID())) {
                return true;
            }
        }

        return target instanceof CombatOwnerUuidSource source
                && source.getCombatOwnerUuid() != null
                && source.getCombatOwnerUuid().equals(owner.getUUID());
    }

    private static @Nullable Entity resolveDamageOwner(DamageSource source) {
        return resolveCombatActor(source.getEntity());
    }

    private static @Nullable Entity resolveCombatActor(@Nullable Entity entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof CombatOwnerUuidSource source && source.getCombatOwnerUuid() != null) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                return serverLevel.getPlayerByUUID(source.getCombatOwnerUuid());
            }
            return null;
        }

        if (entity instanceof IMagicSummon summon) {
            return resolveCombatActor(summon.getSummoner());
        }

        if (entity instanceof Projectile projectile) {
            return resolveCombatActor(projectile.getOwner());
        }

        return entity;
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
