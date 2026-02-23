package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PhalanxCharge extends AbstractSummonWeaponSpell<PhalanxWeaponryEntity> {
    private static final int GUARD_EFFECT_REFRESH_TICK = 5;
    private static final float GUARD_MOVE_SPEED_MULTIPLIER = 0.85f;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "phalanx_charge");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(8)
            .build();

    public PhalanxCharge() {
        super(PhalanxWeaponryEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 0;
        baseManaCost = 3;
        manaCostPerLevel = 1;
        castTime = 200;
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
    public boolean canBeInterrupted(@Nullable Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.PHALANX.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public PhalanxWeaponryEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summon = new PhalanxWeaponryEntity(EntityRegistry.PHALANX_WEAPONRY.get(), level, entity);
        level.addFreshEntity(summon);
        applyGuardState(level, entity);
        return summon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull PhalanxWeaponryEntity weapon) {
        applyGuardState(level, entity);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull PhalanxWeaponryEntity weapon) {
        clearGuardState(level, entity);
        return CompleteCastTypes.RELEASE_WEAPON;
    }

    private void applyGuardState(Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return;
        }

        var amplifier = PhalanxStance.toAmplifier(GUARD_MOVE_SPEED_MULTIPLIER);
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.PHALANX_STANCE.get(),
                GUARD_EFFECT_REFRESH_TICK,
                amplifier,
                false,
                false,
                true
        ));
    }

    private void clearGuardState(Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return;
        }

        entity.removeEffect(EffectRegistry.PHALANX_STANCE.get());
    }
}
