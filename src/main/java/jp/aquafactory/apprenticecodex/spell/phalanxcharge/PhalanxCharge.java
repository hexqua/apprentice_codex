package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

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
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PhalanxCharge extends AbstractSummonWeaponSpell<PhalanxWeaponryEntity> {
    private static final int GUARD_EFFECT_REFRESH_TICK = 5;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "phalanx_charge");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(4)
            .build();

    public PhalanxCharge() {
        super(PhalanxWeaponryEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 30;
        baseManaCost = 15;
        manaCostPerLevel = 5;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getBaseBeamLength(spellLevel, caster), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 3 + 6 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.PHALANX_CHARGE);
    }

    private float getBaseBeamLength(int spellLevel, LivingEntity entity) {
        return 6f + 1.5f * getSpellPower(spellLevel, entity) / 100.0f;
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
        var effectiveCastTime = getCastTime(spellLevel);
        if (ProtectionSpellSupporter.isEquippedBy(entity)) {
            return effectiveCastTime * 2;
        }
        return effectiveCastTime;
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
        var damage = getDamage(spellLevel, entity);
        var thrustBeamLength = getBaseBeamLength(spellLevel, entity);
        weapon.startThrustSequence(damage, thrustBeamLength);
        return CompleteCastTypes.KEEP_WEAPON;
    }

    private void applyGuardState(Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return;
        }

        var guardAmplifier = ProtectionSpellSupporter.isEquippedBy(entity)
                ? PhalanxStance.MOVE_SPEED_ENABLED_AMPLIFIER
                : PhalanxStance.FIXED_AMPLIFIER;
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.PHALANX_STANCE.get(),
                GUARD_EFFECT_REFRESH_TICK,
                guardAmplifier,
                false,
                false,
                true
        ));
    }
}
