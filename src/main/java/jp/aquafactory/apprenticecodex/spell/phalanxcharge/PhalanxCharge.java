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
import jp.aquafactory.apprenticecodex.effect.PhalanxStance;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class PhalanxCharge extends AbstractSummonWeaponSpell<PhalanxWeaponryEntity> {
    private static final int GUARD_EFFECT_REFRESH_TICK = 5;
    private static final float GUARD_MOVE_SPEED_MULTIPLIER = 0.85f;
    private static final int MINIMUM_CHARGE_TIME_TICKS = 20;
    private static final int MAXIMUM_CHARGE_TIME_TICKS = 160;
    private static final int LOWER_ANCHOR_CHARGE_RATE_PERCENT = 200;
    private static final int UPPER_ANCHOR_CHARGE_RATE_PERCENT = 400;
    private static final int LOWER_ANCHOR_CHARGE_TIME_TICKS = 50;
    private static final int UPPER_ANCHOR_CHARGE_TIME_TICKS = 80;
    private static final DustParticleOptions MAX_CHARGE_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.15f), 1.0f);
    private static final int MAX_CHARGE_PARTICLE_COUNT = 12;
    private static final double MAX_CHARGE_PARTICLE_SPEED = 0.01D;

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
        spellPowerPerLevel = 25;
        baseManaCost = 7;
        manaCostPerLevel = 1;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getBaseBeamLength(spellLevel, caster), 1)),
                Component.translatable("ui.apprenticecodex.maximum_thrust_charge_time", Utils.timeFromTicks(getMaximumChargeTime(spellLevel,caster), 1)),
                Component.translatable("ui.apprenticecodex.maximum_thrust_charge_rate", getMaximumChargeRatePercent(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return 5 * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private float getBaseBeamLength(int spellLevel, LivingEntity entity) {
        return 3.0f + getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getRequiredChargeTime(){
        return MINIMUM_CHARGE_TIME_TICKS;
    }

    private int getMaximumChargeTime(int spellLevel, LivingEntity entity){
        var maximumChargeRatePercent = getMaximumChargeRatePercent(spellLevel, entity);
        var interpolation = (maximumChargeRatePercent - LOWER_ANCHOR_CHARGE_RATE_PERCENT)
                / (double) (UPPER_ANCHOR_CHARGE_RATE_PERCENT - LOWER_ANCHOR_CHARGE_RATE_PERCENT);
        var interpolatedChargeTime = (int) Math.round(LOWER_ANCHOR_CHARGE_TIME_TICKS
                + interpolation * (UPPER_ANCHOR_CHARGE_TIME_TICKS - LOWER_ANCHOR_CHARGE_TIME_TICKS));
        return Mth.clamp(interpolatedChargeTime, MINIMUM_CHARGE_TIME_TICKS, MAXIMUM_CHARGE_TIME_TICKS);
    }

    private int getMaximumChargeRatePercent(int spellLevel, LivingEntity entity){
        return 100 + Math.round(getSpellPower(spellLevel, entity));
    }

    private int getChargeRatePercent(int spellLevel, LivingEntity entity, int castDurationTicks) {
        var minimumChargeTime = getRequiredChargeTime();
        var maximumChargeTime = getMaximumChargeTime(spellLevel, entity);
        var maximumChargeRatePercent = getMaximumChargeRatePercent(spellLevel, entity);

        if (maximumChargeTime <= minimumChargeTime) {
            return maximumChargeRatePercent;
        }

        var interpolation = Mth.clamp(
                (castDurationTicks - minimumChargeTime) / (float) (maximumChargeTime - minimumChargeTime),
                0.0f,
                1.0f
        );

        return Math.round(100 + (maximumChargeRatePercent - 100) * interpolation);
    }

    private int getCurrentCastDurationTicks(MagicData playerMagicData) {
        return Math.max(0, playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining());
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
        var castDurationTicks = getCurrentCastDurationTicks(playerMagicData);
        var maximumCharged = castDurationTicks >= getMaximumChargeTime(spellLevel, entity);
        playMaxChargeReachedSoundIfNeeded(entity, weapon, maximumCharged);
        spawnMaxChargeParticlesIfNeeded(level, entity, maximumCharged);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull PhalanxWeaponryEntity weapon) {
        var castDurationTicks = getCurrentCastDurationTicks(playerMagicData);
        var chargeRatePercent = getChargeRatePercent(spellLevel, entity, castDurationTicks);
        var chargeRate = chargeRatePercent / 100.0f;

        var damage = getDamage(spellLevel, entity) * chargeRate;
        var thrustBeamLength = getBaseBeamLength(spellLevel, entity) * chargeRate;
        var maximumCharged = castDurationTicks >= getMaximumChargeTime(spellLevel, entity);

        weapon.startThrustSequence(damage, thrustBeamLength, maximumCharged);
        return CompleteCastTypes.KEEP_WEAPON;
    }

    private void playMaxChargeReachedSoundIfNeeded(LivingEntity entity, PhalanxWeaponryEntity weapon, boolean maximumCharged) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (weapon.hasNotifiedMaxChargeReached()) {
            return;
        }
        if (!maximumCharged) {
            return;
        }

        serverPlayer.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.0f);
        weapon.markMaxChargeReachedNotified();
    }

    private void spawnMaxChargeParticlesIfNeeded(Level level, LivingEntity entity, boolean maximumCharged) {
        if (!maximumCharged || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        //負荷が気になるので間隔を少し開ける.
        if (entity.tickCount % 3 != 0){
            return;
        }

        var horizontalSpread = Math.max(0.2D, entity.getBbWidth() * 0.45D);
        var verticalSpread = Math.max(0.35D, entity.getBbHeight() * 0.45D);
        var centerY = entity.getY() + entity.getBbHeight() * 0.5D;
        serverLevel.sendParticles(
                MAX_CHARGE_PARTICLE,
                entity.getX(),
                centerY,
                entity.getZ(),
                MAX_CHARGE_PARTICLE_COUNT,
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                MAX_CHARGE_PARTICLE_SPEED
        );
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

}
