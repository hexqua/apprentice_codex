package jp.aquafactory.apprenticecodex.spell.mysticshield;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MysticShieldDefenseEvent {
    private static final double FRONT_DOT_THRESHOLD = 0.25;
    private static final int SAME_SOURCE_ACCUMULATION_COOLDOWN_TICKS = 10;
    private static final double SHIELD_DISTANCE = 1.15;
    private static final double SHIELD_Y_OFFSET = -0.65;
    private static final double BLOCK_EFFECT_PLANE_OFFSET = 0.25;
    private static final String ACCUMULATED_DAMAGE_TAG = "ApprenticeCodexMysticShieldAccumulatedDamage";
    private static final String SOURCE_COOLDOWNS_TAG = "ApprenticeCodexMysticShieldSourceCooldowns";
    private static final String SHIELD_ENTITY_ID_TAG = "ApprenticeCodexMysticShieldEntityId";

    private MysticShieldDefenseEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        var target = event.getEntity();
        //noinspection resource
        if (target.level().isClientSide) {
            return;
        }

        var activeShield = getActiveMysticShield(target);
        if (activeShield == null) {
            return;
        }

        var source = event.getSource();
        if (!isFromFront(target, source)) {
            return;
        }

        event.setCanceled(true);
        if (shouldAccumulateDamage(target, source)) {
            addStoredDamage(target, event.getAmount());
        }
        spawnBlockEffect(target, source);
        discardDirectProjectile(source);
        AudioTools.playSoundFromEntity(target.level(), target, SoundRegistry.MYSTIC_SHIELD_BLOCK.get(), SoundSource.PLAYERS, 0.75f, 1.0f, 0.05f);
    }

    public static void resetStoredDamage(LivingEntity entity) {
        var tag = entity.getPersistentData();
        tag.remove(ACCUMULATED_DAMAGE_TAG);
        tag.remove(SOURCE_COOLDOWNS_TAG);
    }

    public static boolean spawnShieldEntity(Level level, LivingEntity caster) {
        var existingShield = getStoredShieldEntity(level, caster);
        if (existingShield != null && !existingShield.isFading()) {
            existingShield.snapToOwner();
            return false;
        }

        fadeStoredShieldEntity(level, caster);
        var shield = new MysticShieldShieldEntity(EntityRegistry.MYSTIC_SHIELD_SHIELD.get(), level, caster);
        shield.snapToOwner();
        level.addFreshEntity(shield);
        caster.getPersistentData().putInt(SHIELD_ENTITY_ID_TAG, shield.getId());
        return true;
    }

    public static void releaseStoredDamage(Level level, int spellLevel, LivingEntity caster, boolean cancelled) {
        fadeStoredShieldEntity(level, caster);
        var accumulatedDamage = getStoredDamage(caster);
        resetStoredDamage(caster);

        if (level.isClientSide || accumulatedDamage <= 0.0f) {
            return;
        }

        var spell = SpellRegistry.MYSTIC_SHIELD.get();
        if (!(spell instanceof MysticShield mysticShield)) {
            return;
        }

        var damage = accumulatedDamage * mysticShield.getReflectDamageMultiplier(spellLevel, caster);
        if (damage <= 0.0f) {
            return;
        }

        var direction = normalizeOrFallback(caster.getLookAngle(), new Vec3(0.0, 0.0, 1.0));
        var projectile = new MysticShieldProjectileEntity(EntityRegistry.MYSTIC_SHIELD_PROJECTILE.get(), level, caster);
        var spawnPosition = caster.getEyePosition().add(direction.scale(0.6)).add(0.0, -0.15, 0.0);
        projectile.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        projectile.setDamage(damage);
        projectile.shoot(direction);
        level.addFreshEntity(projectile);
        spawnReleaseShootEffect(level, spawnPosition);
        AudioTools.playSoundFromEntity(level, caster, SoundRegistry.MYSTIC_SHIELD_SHOOT.get(), SoundSource.PLAYERS, 0.85f, 1.0f, 0.04f);
    }

    static float getStoredDamage(LivingEntity entity) {
        return entity.getPersistentData().getFloat(ACCUMULATED_DAMAGE_TAG);
    }

    private static void fadeStoredShieldEntity(Level level, LivingEntity caster) {
        var shield = getStoredShieldEntity(level, caster);
        if (shield != null) {
            shield.snapToOwner();
            shield.startFade();
        }
        caster.getPersistentData().remove(SHIELD_ENTITY_ID_TAG);
    }

    private static @Nullable MysticShieldShieldEntity getStoredShieldEntity(Level level, LivingEntity caster) {
        var tag = caster.getPersistentData();
        if (!tag.contains(SHIELD_ENTITY_ID_TAG)) {
            return null;
        }

        var entity = level.getEntity(tag.getInt(SHIELD_ENTITY_ID_TAG));
        if (entity instanceof MysticShieldShieldEntity shield) {
            return shield;
        }
        tag.remove(SHIELD_ENTITY_ID_TAG);
        return null;
    }

    private static void addStoredDamage(LivingEntity entity, float amount) {
        if (amount <= 0.0f) {
            return;
        }
        var tag = entity.getPersistentData();
        tag.putFloat(ACCUMULATED_DAMAGE_TAG, tag.getFloat(ACCUMULATED_DAMAGE_TAG) + amount);
    }

    private static boolean shouldAccumulateDamage(LivingEntity defender, DamageSource source) {
        var sourceKey = buildDamageSourceKey(source);
        if (sourceKey.isBlank()) {
            return true;
        }

        //noinspection resource
        var now = defender.level().getGameTime();
        var cooldowns = defender.getPersistentData().getCompound(SOURCE_COOLDOWNS_TAG);
        var lastTick = cooldowns.getLong(sourceKey);
        if (lastTick > 0L && now - lastTick < SAME_SOURCE_ACCUMULATION_COOLDOWN_TICKS) {
            return false;
        }

        cooldowns.putLong(sourceKey, now);
        defender.getPersistentData().put(SOURCE_COOLDOWNS_TAG, cooldowns);
        return true;
    }

    private static void discardDirectProjectile(DamageSource source) {
        if (source.getDirectEntity() instanceof Projectile projectile && !projectile.isRemoved()) {
            projectile.discard();
        }
    }

    private static void spawnBlockEffect(LivingEntity defender, DamageSource source) {
        if (!(defender.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var shieldCenter = resolveShieldCenter(defender);
        var shieldNormal = resolveHorizontalFacing(defender);

        var sourceVector = resolveIncomingOrigin(defender, source);
        var planeVector = sourceVector.subtract(shieldNormal.scale(sourceVector.dot(shieldNormal)));
        var effectPosition = isUsableDirection(planeVector)
                ? shieldCenter.add(planeVector.normalize().scale(BLOCK_EFFECT_PLANE_OFFSET))
                : shieldCenter;
        spawnOrangeGuardSparks(serverLevel, effectPosition, 12);
    }

    private static void spawnReleaseShootEffect(Level level, Vec3 position) {
        if (level instanceof ServerLevel serverLevel) {
            spawnOrangeGuardSparks(serverLevel, position, 24);
        }
    }

    private static void spawnOrangeGuardSparks(ServerLevel level, Vec3 position, int count) {
        var colors = new SparkColor[]{
                new SparkColor(1.0f, 0.9f, 0.62f),
                new SparkColor(1.0f, 0.56f, 0.12f),
                new SparkColor(1.0f, 0.24f, 0.04f)
        };
        for (var i = 0; i < colors.length; i++) {
            var color = colors[i];
            var colorCount = count / colors.length + (i < count % colors.length ? 1 : 0);
            level.sendParticles(
                    createOrangeGuardSpark(color),
                    position.x, position.y, position.z,
                    colorCount,
                    0.18, 0.18, 0.18,
                    0.08
            );
        }
    }

    private static AdditiveGlowParticleOptions createOrangeGuardSpark(SparkColor color) {
        return new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.12f,
                color.red(),
                color.green(),
                color.blue(),
                1,
                4,
                3,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                true
        );
    }

    private static Vec3 resolveShieldCenter(LivingEntity defender) {
        var look = normalizeOrFallback(defender.getLookAngle(), new Vec3(0.0, 0.0, 1.0));
        return defender.getEyePosition().add(look.scale(SHIELD_DISTANCE)).add(0.0, SHIELD_Y_OFFSET, 0.0);
    }

    private static boolean isFromFront(LivingEntity defender, DamageSource source) {
        var defenderForward = resolveHorizontalFacing(defender);

        var incomingOrigin = resolveIncomingOrigin(defender, source);
        if (!isUsableDirection(incomingOrigin)) {
            return false;
        }

        var horizontalIncomingOrigin = horizontal(incomingOrigin);
        if (!isUsableDirection(horizontalIncomingOrigin)) {
            return false;
        }

        return defenderForward.dot(horizontalIncomingOrigin.normalize()) >= FRONT_DOT_THRESHOLD;
    }

    private static Vec3 resolveHorizontalFacing(LivingEntity defender) {
        var look = horizontal(defender.getLookAngle());
        if (isUsableDirection(look)) {
            return look.normalize();
        }
        return horizontal(Vec3.directionFromRotation(0.0f, defender.getYRot())).normalize();
    }

    private static Vec3 resolveIncomingOrigin(LivingEntity defender, DamageSource source) {
        var directEntity = source.getDirectEntity();
        if (directEntity != null && directEntity != defender) {
            return directEntity.getBoundingBox().getCenter().subtract(defender.getBoundingBox().getCenter());
        }

        var attacker = source.getEntity();
        if (attacker != null && attacker != defender) {
            return attacker.getBoundingBox().getCenter().subtract(defender.getBoundingBox().getCenter());
        }

        var sourcePosition = source.getSourcePosition();
        if (sourcePosition != null) {
            return sourcePosition.subtract(defender.getBoundingBox().getCenter());
        }

        if (directEntity != null) {
            return directEntity.getDeltaMovement().reverse();
        }
        return Vec3.ZERO;
    }

    private static String buildDamageSourceKey(DamageSource source) {
        var typeId = source.type().msgId();
        var directEntity = source.getDirectEntity();
        if (directEntity != null) {
            return typeId + "|direct:" + directEntity.getUUID();
        }

        var attackerEntity = source.getEntity();
        if (attackerEntity != null) {
            return typeId + "|attacker:" + attackerEntity.getUUID();
        }

        var sourcePosition = source.getSourcePosition();
        if (sourcePosition != null) {
            return typeId + "|pos:"
                    + Math.round(sourcePosition.x * 16.0)
                    + "," + Math.round(sourcePosition.y * 16.0)
                    + "," + Math.round(sourcePosition.z * 16.0);
        }

        return typeId + "|environment";
    }

    private static @Nullable ActiveMysticShield getActiveMysticShield(LivingEntity entity) {
        var magicData = MagicData.getPlayerMagicData(entity);
        if (magicData == null || !magicData.isCasting()) {
            return null;
        }

        if (!SpellRegistry.MYSTIC_SHIELD.get().getSpellId().equals(magicData.getCastingSpellId())) {
            return null;
        }

        return new ActiveMysticShield(Math.max(1, magicData.getCastingSpellLevel()));
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0, vector.z);
    }

    private static boolean isUsableDirection(Vec3 vector) {
        return vector.lengthSqr() > 1.0e-6;
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector != null && vector.lengthSqr() > 1.0e-6) {
            return vector.normalize();
        }
        return fallback.normalize();
    }

    private record ActiveMysticShield(int spellLevel) {
    }

    private record SparkColor(float red, float green, float blue) {
    }
}
