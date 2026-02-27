package jp.aquafactory.apprenticecodex.spell.compoundphial;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class CompoundPhial extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "compound_phial");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(1)
            .build();

    public CompoundPhial() {
        baseSpellPower = 100;
        spellPowerPerLevel = 15;
        manaCostPerLevel = 4;
        baseManaCost = 10;
        castTime = 10;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getSplashRadius(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.splash_reduce", Utils.stringTruncation(getSplashReducedPercent(spellLevel, caster), 2))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 2 + 2 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(ApprenticeCodexServerConfig.DamageMultiplierKey.COMPOUND_PHIAL);
    }

    private int getSplashReducedPercent(int spellLevel, LivingEntity entity) {
        return 50 + Math.min(50 , Math.round(15 * getSpellPower(spellLevel, entity) / 100.0f));
    }

    private float getSplashRadius(int spellLevel, LivingEntity entity) {
        return 1.5f * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private float getSpeed() {
        return 1.0f;
    }

    private float getSplashDamage(int spellLevel, LivingEntity entity) {
        return getDamage(spellLevel, entity) * getSplashReducedPercent(spellLevel, entity) / 100.0f;
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
        return CastType.LONG;
    }
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BREWING_STAND_BREW);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.SPLASH_POTION_THROW);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_SPIT_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.THROW_SINGLE_ITEM;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var projectile = new CompoundPhialProjectileEntity(EntityRegistry.COMPOUND_PHIAL_PROJECTILE.get(), level, entity);

        projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
        projectile.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0f, getSpeed(), 0.0f);
        projectile.setDamage(getDamage(spellLevel, entity));
        projectile.setSplashDamage(getSplashDamage(spellLevel, entity));
        projectile.setSplashRadius(getSplashRadius(spellLevel, entity));
        projectile.setPotionColorRandom(level);
        level.addFreshEntity(projectile);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
