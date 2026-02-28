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
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceField;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private static final double MELEE_INTERCEPT_DISTANCE = 0.5;
    private static final int AMBIENT_WALL_INTERVAL_TICKS = 5;
    private static final int AMBIENT_DIRECTION_SAMPLE_COUNT = 3;
    private static final double MIN_APPROACH_SPEED_SQ = 0.0025;
    private static final double DEFLECT_MIN_SPEED = 0.75;
    private static final float MELEE_KNOCKBACK_STRENGTH = 2.2f;
    private static final float DEFAULT_WALL_SIZE_SCALE = 1.0f;
    private static final float DEFAULT_WALL_LIFETIME_SCALE = 1.0f;
    private static final float MELEE_WALL_SIZE_SCALE = 2.0f;
    private static final float AMBIENT_WALL_LIFETIME_SCALE = 0.5f;
    private static final boolean DEFAULT_RENDER_WAVE = true;
    private static final boolean AMBIENT_RENDER_WAVE = false;
    private static final String DEFLECT_COUNT_TAG = "ApprenticeCodexForceFieldDeflectCount";
    private static final String CAPTURED_TAG = "ApprenticeCodexForceFieldCaptured";

    private ForceFieldDefenseEvent() {
    }

    public static void interceptNearbyProjectiles(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData magicData) {
        if (level.isClientSide || magicData == null || spellLevel <= 0) {
            return;
        }

        spawnAmbientWallVisual(caster);

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
        //noinspection resource
        if (target.level().isClientSide) {
            return;
        }

        var forceField = getActiveForceField(target);
        if (forceField == null) {
            return;
        }

        var source = event.getSource();
        if (!isBlockableByForceField(source)) {
            return;
        }

        event.setCanceled(true);

        if (isMeleeAttack(source)) {
            var attackerEntity = source.getEntity();
            if (isCloseRangeAttack(target, attackerEntity)) {
                applyMeleeKnockback(target, attackerEntity);
                var interceptPosition = getMeleeInterceptPosition(target, attackerEntity);
                var interceptNormal = getMeleeInterceptNormal(target, attackerEntity);
                onForceFieldIntercept(
                        target,
                        forceField,
                        interceptPosition,
                        interceptNormal,
                        ForceFieldState.INTERCEPT_KIND_MELEE,
                        MELEE_WALL_SIZE_SCALE,
                        DEFAULT_WALL_LIFETIME_SCALE
                );
            } else {
                var interceptPosition = getRangedInterceptPosition(target, attackerEntity, null);
                var interceptNormal = getInterceptNormal(target, interceptPosition, attackerEntity, null);
                onForceFieldIntercept(target, forceField, interceptPosition, interceptNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            }
            return;
        }

        var directEntity = source.getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            var interceptPosition = getRangedInterceptPosition(target, source.getEntity(), projectile.getDeltaMovement());
            if (neutralizeProjectile(target, forceField, projectile, interceptPosition, source.getEntity())) {
                return;
            }
        } else if (tryCounterspellEquivalent(target, forceField.magicData(), directEntity)) {
            var interceptPosition = directEntity.position();
            var interceptNormal = getInterceptNormal(target, interceptPosition, source.getEntity(), directEntity.getDeltaMovement());
            onForceFieldIntercept(target, forceField, interceptPosition, interceptNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return;
        }

        var fallbackPosition = getRangedInterceptPosition(target, source.getEntity(), null);
        var fallbackNormal = getInterceptNormal(target, fallbackPosition, source.getEntity(), null);
        onForceFieldIntercept(target, forceField, fallbackPosition, fallbackNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
    }

    private static boolean shouldInterceptProjectile(LivingEntity caster, Projectile projectile) {
        if (projectile.isRemoved() || projectile.getPersistentData().getBoolean(CAPTURED_TAG)) {
            return false;
        }

        if (projectile.position().distanceToSqr(caster.position()) > INTERCEPT_RADIUS_SQ) {
            return false;
        }

        if (!isProjectileBlockableByForceField(projectile)) {
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
        return neutralizeProjectile(caster, forceField, projectile, projectile.position(), projectile.getOwner());
    }

    private static boolean neutralizeProjectile(LivingEntity caster, ActiveForceField forceField, Projectile projectile,
                                                Vec3 interceptPosition, @Nullable Entity attackerEntity) {
        if (projectile.isRemoved()) {
            return false;
        }

        var interceptNormal = getInterceptNormal(caster, interceptPosition, attackerEntity, projectile.getDeltaMovement());

        if (tryCounterspellEquivalent(caster, forceField.magicData(), projectile)) {
            onForceFieldIntercept(caster, forceField, interceptPosition, interceptNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return true;
        }

        var deflectCount = projectile.getPersistentData().getInt(DEFLECT_COUNT_TAG);
        if (deflectCount <= 0 && tryDeflectProjectile(caster, projectile)) {
            projectile.getPersistentData().putInt(DEFLECT_COUNT_TAG, 1);
            onForceFieldIntercept(caster, forceField, interceptPosition, interceptNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return true;
        }

        if (tryCatchProjectile(caster, projectile)) {
            onForceFieldIntercept(caster, forceField, interceptPosition, interceptNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
            return true;
        }

        projectile.discard();
        onForceFieldIntercept(caster, forceField, interceptPosition, interceptNormal, ForceFieldState.INTERCEPT_KIND_PROJECTILE);
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

    private static boolean isBlockableByForceField(DamageSource source) {
        // 設定次第で盾貫通ダメージと貫通矢を防御対象外にする.
        if (ApprenticeCodexServerConfig.forceFieldCanBlockBypassShield()) {
            return true;
        }

        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }

        return !isPiercingArrow(source.getDirectEntity());
    }

    private static boolean isProjectileBlockableByForceField(Projectile projectile) {
        if (ApprenticeCodexServerConfig.forceFieldCanBlockBypassShield()) {
            return true;
        }

        return !isPiercingArrow(projectile);
    }

    private static boolean isPiercingArrow(@Nullable Entity entity) {
        return entity instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0;
    }

    private static boolean isMeleeAttack(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
    }

    private static boolean isCloseRangeAttack(LivingEntity defender, @Nullable Entity attackerEntity) {
        if (attackerEntity == null) {
            return true;
        }

        var defenderCenter = defender.getBoundingBox().getCenter();
        var attackerCenter = attackerEntity.getBoundingBox().getCenter();
        return attackerCenter.distanceToSqr(defenderCenter) <= INTERCEPT_RADIUS_SQ;
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
        var defenderCenter = defender.getBoundingBox().getCenter();

        var attackDirection = attackerEntity == null
                ? defender.getLookAngle()
                : attackerEntity.getBoundingBox().getCenter().subtract(defenderCenter);
        var horizontalDirection = new Vec3(attackDirection.x, 0.0, attackDirection.z);
        if (!isUsableDirection(horizontalDirection)) {
            var fallbackLook = defender.getLookAngle();
            horizontalDirection = new Vec3(fallbackLook.x, 0.0, fallbackLook.z);
        }
        if (!isUsableDirection(horizontalDirection)) {
            return defenderCenter;
        }
        return defenderCenter.add(horizontalDirection.normalize().scale(MELEE_INTERCEPT_DISTANCE));
    }

    private static Vec3 getMeleeInterceptNormal(LivingEntity defender, @Nullable Entity attackerEntity) {
        var defenderCenter = defender.getBoundingBox().getCenter();
        var direction = attackerEntity == null
                ? defender.getLookAngle()
                : attackerEntity.getBoundingBox().getCenter().subtract(defenderCenter);
        var horizontalDirection = new Vec3(direction.x, 0.0, direction.z);
        if (!isUsableDirection(horizontalDirection)) {
            var fallbackLook = defender.getLookAngle();
            horizontalDirection = new Vec3(fallbackLook.x, 0.0, fallbackLook.z);
        }
        if (!isUsableDirection(horizontalDirection)) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return horizontalDirection.normalize();
    }

    private static Vec3 getRangedInterceptPosition(LivingEntity defender, @Nullable Entity attackerEntity, @Nullable Vec3 incomingMotion) {
        var defenderCenter = defender.getBoundingBox().getCenter();
        Vec3 direction = null;
        var segmentLength = INTERCEPT_RADIUS;

        if (attackerEntity != null) {
            var attackerCenter = attackerEntity.getBoundingBox().getCenter();
            var toAttacker = attackerCenter.subtract(defenderCenter);
            if (isUsableDirection(toAttacker)) {
                direction = toAttacker;
                segmentLength = toAttacker.length();
            }
        }

        if (!isUsableDirection(direction) && isUsableDirection(incomingMotion)) {
            direction = incomingMotion.scale(-1);
            segmentLength = INTERCEPT_RADIUS;
        }

        if (!isUsableDirection(direction)) {
            direction = defender.getLookAngle();
            segmentLength = INTERCEPT_RADIUS;
        }

        if (!isUsableDirection(direction)) {
            return defenderCenter;
        }

        var interceptDistance = Math.min(INTERCEPT_RADIUS, Math.max(0.0, segmentLength));
        if (interceptDistance <= 1.0e-4) {
            return defenderCenter;
        }
        return defenderCenter.add(direction.normalize().scale(interceptDistance));
    }

    private static void onForceFieldIntercept(LivingEntity caster, ActiveForceField forceField, Vec3 position, Vec3 normal, int interceptKind) {
        onForceFieldIntercept(
                caster,
                forceField,
                position,
                normal,
                interceptKind,
                DEFAULT_WALL_SIZE_SCALE,
                DEFAULT_WALL_LIFETIME_SCALE,
                DEFAULT_RENDER_WAVE
        );
    }

    private static void onForceFieldIntercept(LivingEntity caster, ActiveForceField forceField, Vec3 position, Vec3 normal,
                                              int interceptKind, float sizeScale, float lifetimeScale) {
        onForceFieldIntercept(caster, forceField, position, normal, interceptKind, sizeScale, lifetimeScale, DEFAULT_RENDER_WAVE);
    }

    private static void onForceFieldIntercept(LivingEntity caster, ActiveForceField forceField, Vec3 position, Vec3 normal,
                                              int interceptKind, float sizeScale, float lifetimeScale, boolean renderWave) {
        drainManaOnIntercept(caster, forceField);
        storeInterceptPosition(caster, position, interceptKind);
        broadcastDefenseEffect(caster, position, normal, sizeScale, lifetimeScale, renderWave);
        playShieldBlockSound(caster, position);
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
            //noinspection resource
            state.lastInterceptGameTime = caster.level().getGameTime();
        }));
    }

    private static void broadcastDefenseEffect(LivingEntity caster, Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale,
                                               boolean renderWave) {
        var safeNormal = sanitizeInterceptNormal(caster, position, normal);
        Networks.sendToTrackingEntityAndSelf(caster, new ForceFieldDefenseEffectPacket(position, safeNormal, sizeScale, lifetimeScale, renderWave));
    }

    private static void playShieldBlockSound(LivingEntity caster, Vec3 position) {
        AudioTools.playSoundFromPosition(caster.level(), position, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS);
        AudioTools.playSoundFromPosition(caster.level(), position, SoundRegistry.FORCE_FIELD_DEFLECT.get(), SoundSource.PLAYERS);
    }

    private static Vec3 getInterceptNormal(LivingEntity caster, Vec3 interceptPosition, @Nullable Entity attackerEntity, @Nullable Vec3 incomingMotion) {
        if (isUsableDirection(incomingMotion)) {
            return incomingMotion.scale(-1).normalize();
        }

        if (attackerEntity != null) {
            var direction = attackerEntity.position().subtract(caster.getEyePosition());
            if (isUsableDirection(direction)) {
                return direction.normalize();
            }
        }

        var fallback = interceptPosition.subtract(caster.getEyePosition());
        if (isUsableDirection(fallback)) {
            return fallback.normalize();
        }

        return caster.getLookAngle();
    }

    private static Vec3 sanitizeInterceptNormal(LivingEntity caster, Vec3 interceptPosition, Vec3 normal) {
        if (isUsableDirection(normal)) {
            return normal.normalize();
        }
        return getInterceptNormal(caster, interceptPosition, null, null);
    }

    private static boolean isUsableDirection(@Nullable Vec3 vector) {
        return vector != null && vector.lengthSqr() > 1.0e-4;
    }

    private static void spawnAmbientWallVisual(LivingEntity caster) {
        if (caster.tickCount % AMBIENT_WALL_INTERVAL_TICKS != 0) {
            return;
        }

        var direction = getAmbientWallDirection(caster);
        if (!isUsableDirection(direction)) {
            return;
        }

        var position = caster.getBoundingBox().getCenter().add(direction.scale(INTERCEPT_RADIUS));
        var normal = direction.reverse();
        broadcastDefenseEffect(
                caster,
                position,
                normal,
                DEFAULT_WALL_SIZE_SCALE,
                AMBIENT_WALL_LIFETIME_SCALE,
                AMBIENT_RENDER_WAVE
        );
    }

    private static Vec3 getAmbientWallDirection(LivingEntity caster) {
        var look = caster.getLookAngle();
        if (!isUsableDirection(look)) {
            look = new Vec3(0, 0, 1);
        } else {
            look = look.normalize();
        }

        var bestDirection = look;
        var bestDot = -Double.MAX_VALUE;
        for (int i = 0; i < AMBIENT_DIRECTION_SAMPLE_COUNT; i++) {
            var candidate = randomUnitVector(caster);
            var dot = candidate.dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                bestDirection = candidate;
            }
        }

        var blended = bestDirection.scale(0.45).add(look.scale(0.55));
        if (!isUsableDirection(blended)) {
            return look;
        }
        return blended.normalize();
    }

    private static Vec3 randomUnitVector(LivingEntity entity) {
        var random = entity.getRandom();
        var azimuth = random.nextDouble() * Math.PI * 2.0;
        var y = random.nextDouble() * 2.0 - 1.0;
        var radial = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        return new Vec3(
                radial * Math.cos(azimuth),
                y,
                radial * Math.sin(azimuth)
        );
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
