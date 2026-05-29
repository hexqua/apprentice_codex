package jp.aquafactory.apprenticecodex.spell.inscribeice;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class InscribeIce extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "inscribe_ice");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(2)
            .build();

    public InscribeIce() {
        baseSpellPower = 100;
        spellPowerPerLevel = 30;
        baseManaCost = 50;
        manaCostPerLevel = 15;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.inscribe_ice.notched_frozen_damage", Utils.stringTruncation(getBurstDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getProjectileCount(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // ダメージそのものは低く、デバフがメイン.
        var rawDamage = 1 + 2 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.INSCRIBE_ICE);
    }

    public float getBurstDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 12 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.INSCRIBE_ICE);
    }

    public int getProjectileCount(int spellLevel, LivingEntity entity){
        // 正面に飛ぶように必ず奇数個.
        var baseCount = Math.round(3 * getSpellPower(spellLevel, entity) / 100.0f);
        return baseCount % 2 == 1 ? baseCount : baseCount + 1;
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
        return Optional.of(SoundRegistry.ICE_DAGGER_THROW.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var projectileCount = getProjectileCount(spellLevel, entity);
            var job = new InscribeIceDaggerThrowJob(
                    serverLevel,
                    entity,
                    projectileCount,
                    getDamage(spellLevel, entity),
                    getBurstDamage(spellLevel, entity)
            );
            job.tick(serverLevel);
            InscribeIceDaggerThrowJobManager.submit(serverLevel, job);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    static float getArcDegrees(int projectileCount) {
        return Mth.clamp(60.0F + (projectileCount - 3) * 10.0F, 60.0F, 120.0F);
    }

    static Vec3 getLookForward(LivingEntity entity) {
        var forward = entity.getLookAngle();
        if (forward.lengthSqr() <= 1.0E-8D) {
            return RotationTools.getFlatForward(entity);
        }
        return forward.normalize();
    }

    static Vec3 getRightVector(LivingEntity entity, Vec3 forward) {
        var right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() <= 1.0E-8D) {
            return new Vec3(RotationTools.getFlatForward(entity).z, 0.0D, -RotationTools.getFlatForward(entity).x)
                    .normalize();
        }
        return right.normalize();
    }

    static Vec3 calculateDaggerLaunchPosition(LivingEntity entity, Vec3 forward) {
        return entity.getEyePosition()
                .add(forward.scale(0.4D))
                .add(0.0D, -0.25D, 0.0D);
    }

    static Vec3 calculateProjectileDirection(Vec3 forward, Vec3 right, int projectileCount, float arcDegrees, int index) {
        if (projectileCount <= 1) {
            return forward;
        }

        var halfArcRadians = arcDegrees * Mth.DEG_TO_RAD * 0.5D;
        var progress = (double) index / (projectileCount - 1);
        var angle = Mth.lerp(progress, halfArcRadians, -halfArcRadians);
        return forward.scale(Math.cos(angle)).add(right.scale(Math.sin(angle))).normalize();
    }
}
