package jp.aquafactory.apprenticecodex.spell.featherrush;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class FeatherRush extends AbstractSummonWeaponSpell<FeatherRushWingEntity> {
    private static final double FIRE_SIDE_OFFSET = 0.5;
    private static final float BACKWARD_YAW_BIAS_DEG = 35.0f;
    private static final float BACKWARD_YAW_RANDOM_DEG = 45.0f;
    private static final float BACKWARD_PITCH_RANDOM_DEG = 60.0f;
    private static final int MIN_FIRE_INTERVAL_TICKS = 2;
    private static final int MIN_PROJECTILES_PER_BURST = 2;
    private static final float SPELL_POWER_LOW = 100.0f;
    private static final float SPELL_POWER_HIGH = 400.0f;
    private static final float RPM_AT_LOW_POWER = 300.0f;
    private static final float RPM_AT_HIGH_POWER = 900.0f;
    private static final float TICKS_PER_MINUTE = 1200.0f;
    private static final long ACTIVE_TICK_GRACE = 2L;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "feather_rush");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(12)
            .build();

    public FeatherRush() {
        super(FeatherRushWingEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 75;
        baseManaCost = 4;
        manaCostPerLevel = 2;
        castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        var burstSettings = getBurstSettings(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.feather_rpm", burstSettings.rpm())
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 1 + 2.5f * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.FEATHER_RUSH);
    }

    private int getTargetRpm(int spellLevel, LivingEntity entity) {
        var spellPower = getSpellPower(spellLevel, entity);
        var scale = (spellPower - SPELL_POWER_LOW) / (SPELL_POWER_HIGH - SPELL_POWER_LOW);
        var targetRpm = RPM_AT_LOW_POWER + (RPM_AT_HIGH_POWER - RPM_AT_LOW_POWER) * scale;
        return Math.max(1, Math.round(targetRpm));
    }

    private BurstSettings getBurstSettings(int spellLevel, LivingEntity entity) {
        var targetRpm = getTargetRpm(spellLevel, entity);
        var maxIntervalTicks = Math.max(
                MIN_FIRE_INTERVAL_TICKS,
                (int) Math.ceil((TICKS_PER_MINUTE * MIN_PROJECTILES_PER_BURST) / Math.max(1, targetRpm)) + 2
        );

        var bestIntervalTicks = MIN_FIRE_INTERVAL_TICKS;
        var bestProjectilesPerBurst = MIN_PROJECTILES_PER_BURST;
        var bestError = Double.MAX_VALUE;

        for (var fireIntervalTicks = MIN_FIRE_INTERVAL_TICKS; fireIntervalTicks <= maxIntervalTicks; ++fireIntervalTicks) {
            var idealProjectiles = targetRpm * fireIntervalTicks / TICKS_PER_MINUTE;
            var floorProjectiles = Math.max(MIN_PROJECTILES_PER_BURST, (int) Math.floor(idealProjectiles));
            var ceilProjectiles = Math.max(MIN_PROJECTILES_PER_BURST, (int) Math.ceil(idealProjectiles));
            var floorResult = evaluateBurstCandidate(targetRpm, fireIntervalTicks, floorProjectiles);
            if (isBetterCandidate(floorResult, bestError, bestProjectilesPerBurst, bestIntervalTicks)) {
                bestError = floorResult.error();
                bestIntervalTicks = fireIntervalTicks;
                bestProjectilesPerBurst = floorProjectiles;
            }

            if (ceilProjectiles != floorProjectiles) {
                var ceilResult = evaluateBurstCandidate(targetRpm, fireIntervalTicks, ceilProjectiles);
                if (isBetterCandidate(ceilResult, bestError, bestProjectilesPerBurst, bestIntervalTicks)) {
                    bestError = ceilResult.error();
                    bestIntervalTicks = fireIntervalTicks;
                    bestProjectilesPerBurst = ceilProjectiles;
                }
            }
        }

        var rpm = Math.round(TICKS_PER_MINUTE * bestProjectilesPerBurst / bestIntervalTicks);
        return new BurstSettings(bestIntervalTicks, bestProjectilesPerBurst, rpm);
    }

    private CandidateResult evaluateBurstCandidate(int targetRpm, int fireIntervalTicks, int projectilesPerBurst) {
        var rpm = TICKS_PER_MINUTE * projectilesPerBurst / fireIntervalTicks;
        var error = Math.abs(targetRpm - rpm);
        return new CandidateResult(error, fireIntervalTicks, projectilesPerBurst);
    }

    private boolean isBetterCandidate(CandidateResult candidate, double bestError, int bestProjectiles, int bestIntervalTicks) {
        if (candidate.error() < bestError) {
            return true;
        }

        if (candidate.error() > bestError) {
            return false;
        }

        if (candidate.projectilesPerBurst() != bestProjectiles) {
            return candidate.projectilesPerBurst() < bestProjectiles;
        }

        return candidate.fireIntervalTicks() < bestIntervalTicks;
    }

    private record BurstSettings(int fireIntervalTicks, int projectilesPerBurst, int rpm) {
    }

    private record CandidateResult(double error, int fireIntervalTicks, int projectilesPerBurst) {
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FLAPPED.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public FeatherRushWingEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWing = new FeatherRushWingEntity(EntityRegistry.FEATHER_RUSH_WING.get(), level, entity);
        level.addFreshEntity(summonWing);
        markActiveState(level, entity, summonWing);
        return summonWing;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull FeatherRushWingEntity weapon) {
        markActiveState(level, entity, weapon);

        var burstSettings = getBurstSettings(spellLevel, entity);
        if (weapon.tickCount % burstSettings.fireIntervalTicks() != 0) {
            return;
        }

        var fireCount = burstSettings.projectilesPerBurst();
        var burstIndex = weapon.tickCount / burstSettings.fireIntervalTicks();
        for (var index = 0; index < fireCount; ++index) {
            var fromRightWing = (((burstIndex * fireCount) + index) & 1) == 0;
            var projectile = new FeatherRushProjectileEntity(EntityRegistry.FEATHER_RUSH_PROJECTILE.get(), level, entity);
            projectile.setPos(getProjectileSpawnPosition(level, weapon, fromRightWing));
            projectile.setDamage(getDamage(spellLevel, entity));

            var shootAngle = RaycastTools.randomRotateInCone(entity.getLookAngle().normalize(), 45f, level.random);
            projectile.setStraightFlightDirections(
                    getBackwardDirection(level, entity, fromRightWing),
                    shootAngle,
                    level.random.nextInt(1, 4)
            );
            level.addFreshEntity(projectile);
        }
        AudioTools.playSoundFromEntity(level, entity, SoundRegistry.VANILLA_FEATHER_SHOOT.get(), SoundSource.PLAYERS);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull FeatherRushWingEntity weapon) {
        clearActiveState(level, entity);
        return CompleteCastTypes.RELEASE_WEAPON;
    }

    private void markActiveState(Level level, LivingEntity entity, FeatherRushWingEntity wing) {
        if (level.isClientSide) {
            return;
        }

        Capabilities.withSpellData(entity, data -> data.edit(CodexSpellStateTypeRegister.FEATHER_RUSH_STATE, state -> {
            state.activeUntilGameTime = level.getGameTime() + ACTIVE_TICK_GRACE;
            state.wingEntityId = wing.getId();
        }));
    }

    private void clearActiveState(Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return;
        }

        Capabilities.withSpellData(entity, data -> data.edit(CodexSpellStateTypeRegister.FEATHER_RUSH_STATE, state -> {
            state.activeUntilGameTime = 0;
            state.wingEntityId = -1;
            if (state.noGravityApplied) {
                entity.setNoGravity(false);
                state.noGravityApplied = false;
            }
        }));
    }

    private Vec3 getProjectileSpawnPosition(Level level, FeatherRushWingEntity wing, boolean fromRightWing) {
        var random = level.random;
        var side = (fromRightWing ? FIRE_SIDE_OFFSET : -FIRE_SIDE_OFFSET) + (random.nextDouble() - 0.5) * 0.125;
        return RotationTools.calculateBehindPosition(wing, 0.0, side, -0.5 + (random.nextDouble() - 0.5) * 0.125);
    }

    private Vec3 getBackwardDirection(Level level, LivingEntity caster, boolean fromRightWing) {
        var backwardFlat = RotationTools.getFlatForward(caster).scale(-1);
        var yawBias = fromRightWing ? -BACKWARD_YAW_BIAS_DEG : BACKWARD_YAW_BIAS_DEG;
        var randomYaw = level.random.nextFloat() * BACKWARD_YAW_RANDOM_DEG - BACKWARD_YAW_RANDOM_DEG / 2;
        var randomPitch = level.random.nextFloat() * BACKWARD_PITCH_RANDOM_DEG; // 意図的に上にブレるように.
        var yawRotated = rotateYaw(backwardFlat, yawBias + randomYaw);
        return rotatePitch(yawRotated, -randomPitch).normalize(); // 意図的に上にブレるように.
    }

    private Vec3 rotateYaw(Vec3 direction, float degree) {
        var rad = degree * Mth.DEG_TO_RAD;
        var cos = Mth.cos(rad);
        var sin = Mth.sin(rad);
        var x = direction.x * cos - direction.z * sin;
        var z = direction.x * sin + direction.z * cos;
        return new Vec3(x, direction.y, z);
    }

    private Vec3 rotatePitch(Vec3 direction, float degree) {
        var axis = new Vec3(direction.z, 0.0, -direction.x).normalize();
        if (axis.lengthSqr() <= 1.0e-6) {
            return direction;
        }

        var rad = degree * Mth.DEG_TO_RAD;
        var cos = Math.cos(rad);
        var sin = Math.sin(rad);
        var term1 = direction.scale(cos);
        var term2 = axis.cross(direction).scale(sin);
        var term3 = axis.scale(axis.dot(direction) * (1.0 - cos));
        return term1.add(term2).add(term3);
    }
}
