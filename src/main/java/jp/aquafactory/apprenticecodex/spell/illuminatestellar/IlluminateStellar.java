package jp.aquafactory.apprenticecodex.spell.illuminatestellar;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class IlluminateStellar extends AbstractSpell {
    private static final int MIN_STAR_COUNT = 3;
    private static final int MAX_STAR_COUNT = 4;
    private static final float TOTAL_YAW_SPREAD_DEGREES = 120.0f;
    private static final float YAW_RANDOM_OFFSET_DEGREES = 10.0f;
    private static final float PITCH_RANDOM_OFFSET_DEGREES = 10.0f;
    private static final double FALLBACK_LOOK_RANGE = 64.0;
    private static final double FALLBACK_LOOK_BOX_WIDTH = 2.0;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "illuminate_stellar");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .setAllowCrafting(false)
            .build();

    public IlluminateStellar() {
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 50;
        manaCostPerLevel = 50;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 4.0f * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.ILLUMINATE_STELLAR);
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
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.STELLAR_FIRE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        // 武器固有なのでモーションはさせない.
        return AnimationHolder.pass();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        // 武器固有なのでモーションはさせない.
        return AnimationHolder.pass();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            spawnStars(level, spellLevel, entity);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnStars(Level level, int spellLevel, LivingEntity caster) {
        var count = level.random.nextInt(MAX_STAR_COUNT - MIN_STAR_COUNT + 1) + MIN_STAR_COUNT;
        var fallbackTarget = findFallbackTarget(caster);

        for (var index = 0; index < count; ++index) {
            var yawOffset = computeYawOffset(count, index, level);
            var pitchOffset = computePitchOffset(level);
            var direction = Vec3.directionFromRotation(caster.getXRot() + pitchOffset, caster.getYRot() + yawOffset);
            var star = new IlluminateStellarStarEntity(EntityRegistry.ILLUMINATE_STELLAR_STAR.get(), level, caster);
            var spawnPosition = pickSpawnPosition(level, caster, star, direction);

            star.setPos(spawnPosition);
            star.setDamage(getDamage(spellLevel, caster));
            star.setDriftProfile(direction);
            star.setFallbackTarget(fallbackTarget);
            level.addFreshEntity(star);
        }
    }

    private static float computeYawOffset(int count, int index, Level level) {
        var evenlyDistributed = count <= 1
                ? 0.0f
                : -TOTAL_YAW_SPREAD_DEGREES / 2.0f + (TOTAL_YAW_SPREAD_DEGREES * index) / (float) (count - 1);
        var randomOffset = (level.random.nextFloat() * 2.0f - 1.0f) * YAW_RANDOM_OFFSET_DEGREES;
        return evenlyDistributed + randomOffset;
    }

    private static float computePitchOffset(Level level) {
        return (level.random.nextFloat() * 2.0f - 1.0f) * PITCH_RANDOM_OFFSET_DEGREES;
    }

    private static Entity findFallbackTarget(LivingEntity caster) {
        var result = RaycastTools.raycastFromEye(caster, FALLBACK_LOOK_RANGE, FALLBACK_LOOK_BOX_WIDTH,
                entity -> CombatTools.isValidCombatTarget(entity, caster));
        var rawTarget = result.hitEntity();
        if (rawTarget == null) {
            return null;
        }

        var target = CombatTools.resolutePartEntity(rawTarget);
        return CombatTools.isValidCombatTarget(target, caster) ? target : null;
    }

    private static Vec3 pickSpawnPosition(Level level, LivingEntity caster, IlluminateStellarStarEntity star, Vec3 direction) {
        // ちょっと下に下げる.
        var basePosition = caster.getEyePosition().add(0, -0.4, 0);
        var distances = new double[]{0.55, 0.35, 0.18, 0.0};
        for (var distance : distances) {
            var candidate = basePosition.add(direction.scale(distance));
            if (level.noCollision(star, star.makeSpawnCheckAabb(candidate))) {
                return candidate;
            }
        }

        return basePosition;
    }
}
