package jp.aquafactory.apprenticecodex.spell.bloodbrand;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastContext;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class BloodBrand extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "blood_brand");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public BloodBrand() {
        baseSpellPower = 100;
        spellPowerPerLevel = 75;
        baseManaCost = 35;
        manaCostPerLevel = 10;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getExplodeDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getExplodeRange(), 0))
        );
    }

    public float getDamage(int spellLevel, LivingEntity entity) {
        // ダメージそのものは低く、デバフがメイン.
        var rawDamage = 1 + getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.BLOOD_BRAND);
    }

    public float getExplodeDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 8 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.BLOOD_BRAND);
    }

    public int getExplodeRange() {
        return 5;
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
            var geometry = resolveLaunchGeometry(entity);
            var projectile = new BloodBrandKunai(EntityRegistry.BLOOD_BRAND_KUNAI.get(), serverLevel, entity);
            projectile.setPos(geometry.position());
            projectile.setDamage(getDamage(spellLevel, entity));
            projectile.setBurstDamage(getExplodeDamage(spellLevel, entity));
            projectile.setBurstRange(getExplodeRange());
            projectile.setProjectileVelocity(geometry.forward());
            serverLevel.addFreshEntity(projectile);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private static LaunchGeometry resolveLaunchGeometry(LivingEntity caster) {
        if (caster instanceof ServerPlayer serverPlayer) {
            var remoteContext = RemoteOwnerCastContext.get(serverPlayer);
            if (remoteContext != null) {
                var forward = normalizeOrFallback(remoteContext.forward(), caster);
                return new LaunchGeometry(calculateLaunchPosition(remoteContext.eyePosition(), forward), forward);
            }
        }

        var forward = normalizeOrFallback(caster.getLookAngle(), caster);
        return new LaunchGeometry(calculateLaunchPosition(caster.getEyePosition(), forward), forward);
    }

    private static Vec3 normalizeOrFallback(Vec3 direction, LivingEntity caster) {
        return direction.lengthSqr() > 1.0E-8D ? direction.normalize() : RotationTools.getFlatForward(caster);
    }

    private static Vec3 calculateLaunchPosition(Vec3 eyePosition, Vec3 forward) {
        return eyePosition.add(forward.scale(0.4D)).add(0.0D, -0.25D, 0.0D);
    }

    private record LaunchGeometry(Vec3 position, Vec3 forward) {
    }
}
