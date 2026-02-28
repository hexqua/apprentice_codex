package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ForceFieldState;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceField;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ForceFieldDefenseEvent {
    private static final double INTERCEPT_RADIUS = 3.0;
    private static final double INTERCEPT_RADIUS_SQ = INTERCEPT_RADIUS * INTERCEPT_RADIUS;
    private static final double MIN_APPROACH_SPEED_SQ = 0.0025;
    private static final double DEFLECT_MIN_SPEED = 0.75;
    private static final float MELEE_KNOCKBACK_STRENGTH = 2.2f;
    private static final String DEFLECT_COUNT_TAG = "ApprenticeCodexForceFieldDeflectCount";
    private static final String CAPTURED_TAG = "ApprenticeCodexForceFieldCaptured";

    private ForceFieldDefenseEvent() {
    }

    public static void interceptNearbyProjectiles(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData magicData) {
        if (level.isClientSide || magicData == null || spellLevel <= 0) {
            return;
        }

        var forceField = new ActiveForceField(magicData, spellLevel);
        var searchBox = caster.getBoundingBox().inflate(INTERCEPT_RADIUS);
        var projectiles = level.getEntitiesOfClass(
                Projectile.class,
                searchBox,
                projectile -> shouldInterceptProjectile(caster, projectile)
        );

        for (var projectile : projectiles) {
            neutralizeProjectile(caster, forceField, projectile);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        var target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }

        var forceField = getActiveForceField(target);
        if (forceField == null) {
            return;
        }

        var source = event.getSource();
        if (!isShieldBlockableIgnoringDirection(source)) {
            return;
        }

        event.setCanceled(true);

        if (isMeleeAttack(source)) {
            applyMeleeKnockback(target, source.getEntity());
            onForceFieldIntercept(target, forceField, getMeleeInterceptPosition(target, source.getEntity()), ForceFieldState.INTERCEPT_KIND_MELEE);
            return;
        }

        var directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            if (neutralizeProjectile(target, forceField, projectile)) {
                return;
            }
        } else if (directEntity != null && tryCounterspellEquivalent(target, forceField.magicData(), directEntity)) {
            onForceFieldIntercept(target, forceField, directEntity.position(), ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return;
        }

        onForceFieldIntercept(target, forceField, target.position(), ForceFieldState.INTERCEPT_KIND_PROJECTILE);
    }

    private static boolean shouldInterceptProjectile(LivingEntity caster, Projectile projectile) {
        if (projectile.isRemoved() || projectile.getPersistentData().getBoolean(CAPTURED_TAG)) {
            return false;
        }

        if (projectile.position().distanceToSqr(caster.position()) > INTERCEPT_RADIUS_SQ) {
            return false;
        }

        var owner = projectile.getOwner();
        if (owner == caster || (owner != null && owner.isAlliedTo(caster))) {
            return false;
        }

        var velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < MIN_APPROACH_SPEED_SQ) {
            return false;
        }

        var toCaster = caster.getEyePosition().subtract(projectile.position());
        return velocity.dot(toCaster) > 0.0;
    }

    private static boolean neutralizeProjectile(LivingEntity caster, ActiveForceField forceField, Projectile projectile) {
        if (projectile.isRemoved()) {
            return false;
        }

        if (tryCounterspellEquivalent(caster, forceField.magicData(), projectile)) {
            onForceFieldIntercept(caster, forceField, projectile.position(), ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return true;
        }

        var deflectCount = projectile.getPersistentData().getInt(DEFLECT_COUNT_TAG);
        if (deflectCount <= 0 && tryDeflectProjectile(caster, projectile)) {
            projectile.getPersistentData().putInt(DEFLECT_COUNT_TAG, 1);
            onForceFieldIntercept(caster, forceField, projectile.position(), ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return true;
        }

        if (tryCatchProjectile(caster, projectile)) {
            onForceFieldIntercept(caster, forceField, projectile.position(), ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return true;
        }

        projectile.discard();
        onForceFieldIntercept(caster, forceField, projectile.position(), ForceFieldState.INTERCEPT_KIND_PROJECTILE);
        return true;
    }

    private static boolean tryDeflectProjectile(LivingEntity caster, Projectile projectile) {
        var velocity = projectile.getDeltaMovement();
        var speed = velocity.length();
        if (speed < 0.01) {
            return false;
        }

        var away = projectile.position().subtract(caster.getEyePosition());
        if (away.lengthSqr() < 1.0e-4) {
            away = velocity;
        }
        if (away.lengthSqr() < 1.0e-4) {
            return false;
        }

        away = away.normalize();
        var newSpeed = Math.max(speed, DEFLECT_MIN_SPEED);
        var deflectedVelocity = away.scale(newSpeed).add(0.0, 0.08, 0.0);
        var escape = caster.getEyePosition().add(away.scale(INTERCEPT_RADIUS + 0.6));
        projectile.setPos(escape.x, escape.y, escape.z);
        projectile.setOwner(caster);
        projectile.setDeltaMovement(deflectedVelocity);
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        return true;
    }

    private static boolean tryCatchProjectile(LivingEntity caster, Projectile projectile) {
        var away = projectile.position().subtract(caster.getEyePosition());
        if (away.lengthSqr() < 1.0e-4) {
            away = caster.getLookAngle().reverse();
        }
        if (away.lengthSqr() < 1.0e-4) {
            return false;
        }

        var holdPos = caster.getEyePosition().add(away.normalize().scale(INTERCEPT_RADIUS + 0.25));
        projectile.setPos(holdPos.x, holdPos.y, holdPos.z);
        projectile.setOwner(caster);
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.setNoGravity(true);
        projectile.getPersistentData().putBoolean(CAPTURED_TAG, true);

        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(0.0);
            arrow.setPierceLevel((byte) 0);
        }

        return true;
    }

    private static boolean tryCounterspellEquivalent(LivingEntity caster, MagicData magicData, Entity target) {
        if (!(target instanceof AntiMagicSusceptible antiMagicTarget)) {
            return false;
        }

        // CounterspellSpell と同じキャンセル可能イベントを経由して互換性を維持する。
        if (MinecraftForge.EVENT_BUS.post(new CounterSpellEvent(caster, target))) {
            return false;
        }

        if (antiMagicTarget instanceof IMagicSummon summon) {
            if (summon.getSummoner() == caster) {
                if (summon instanceof Mob mob && mob.getTarget() == null) {
                    antiMagicTarget.onAntiMagic(magicData);
                }
            } else {
                antiMagicTarget.onAntiMagic(magicData);
            }
        } else {
            antiMagicTarget.onAntiMagic(magicData);
        }

        if (target instanceof LivingEntity livingEntity) {
            for (var mobEffect : livingEntity.getActiveEffectsMap().keySet().stream().toList()) {
                if (mobEffect instanceof MagicMobEffect) {
                    livingEntity.removeEffect(mobEffect);
                }
            }
        }

        return true;
    }

    private static boolean isShieldBlockableIgnoringDirection(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }

        var direct = source.getDirectEntity();
        if (direct instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return false;
        }

        return true;
    }

    private static boolean isMeleeAttack(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
    }

    private static void applyMeleeKnockback(LivingEntity defender, @Nullable Entity attackerEntity) {
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }

        attacker.knockback(
                MELEE_KNOCKBACK_STRENGTH,
                defender.getX() - attacker.getX(),
                defender.getZ() - attacker.getZ()
        );
        var currentVelocity = attacker.getDeltaMovement();
        attacker.setDeltaMovement(currentVelocity.x, Math.max(currentVelocity.y, 0.35), currentVelocity.z);
        attacker.hasImpulse = true;
        attacker.hurtMarked = true;
    }

    private static Vec3 getMeleeInterceptPosition(LivingEntity defender, @Nullable Entity attackerEntity) {
        if (attackerEntity == null) {
            return defender.position();
        }
        return defender.position().add(attackerEntity.position()).scale(0.5);
    }

    private static void onForceFieldIntercept(LivingEntity caster, ActiveForceField forceField, Vec3 position, int interceptKind) {
        drainManaOnIntercept(caster, forceField);
        storeInterceptPosition(caster, position, interceptKind);
    }

    private static void drainManaOnIntercept(LivingEntity caster, ActiveForceField forceField) {
        var drainMana = ForceField.getDrainManaPerHit(forceField.spellLevel(), caster);
        if (drainMana <= 0f) {
            return;
        }

        var magicData = forceField.magicData();
        magicData.setMana(Math.max(0f, magicData.getMana() - drainMana));
    }

    private static void storeInterceptPosition(LivingEntity caster, Vec3 position, int interceptKind) {
        Capabilities.withSpellData(caster, spellData -> spellData.edit(CodexSpellStateTypeRegister.FORCE_FIELD_STATE, state -> {
            state.hasInterceptPoint = true;
            state.lastInterceptX = position.x;
            state.lastInterceptY = position.y;
            state.lastInterceptZ = position.z;
            state.lastInterceptKind = interceptKind;
            state.lastInterceptGameTime = caster.level().getGameTime();
        }));
    }

    private static @Nullable ActiveForceField getActiveForceField(LivingEntity entity) {
        var magicData = MagicData.getPlayerMagicData(entity);
        if (magicData == null || !magicData.isCasting()) {
            return null;
        }

        if (!SpellRegistry.FORCE_FIELD.get().getSpellId().equals(magicData.getCastingSpellId())) {
            return null;
        }

        var spellLevel = Math.max(1, magicData.getCastingSpellLevel());
        return new ActiveForceField(magicData, spellLevel);
    }

    private record ActiveForceField(MagicData magicData, int spellLevel) {
    }
}
