package jp.aquafactory.apprenticecodex.spell.moonlight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MoonLight extends AbstractSummonWeaponSpell<MoonLightKatanaEntity> {
    private static final int MINIMUM_CHARGE_TICK = 40;
    private static final int STANDBY_START_DELAY_TICK = 10;
    private static final int NO_CHARGE_MANA_COST = 50;
    private static final int NORMAL_CHARGE_MANA_COST = 100;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "moon_light");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(8)
            .setAllowCrafting(false)
            .build();

    public MoonLight() {
        super(MoonLightKatanaEntity.class);
        baseSpellPower = 800;
        spellPowerPerLevel = 600;
        baseManaCost = 30;
        manaCostPerLevel = 5;
        castTime = 500;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.required_full_charge_time", Utils.timeFromTicks(getRequiredChargeTime(spellLevel, caster), 1)),
                Component.translatable("ui.apprenticecodex.full_charge_power", getFullPowerRate(spellLevel, caster)),
                Component.literal("W.I.P.")
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // todo:バランス調整.
        return getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getRequiredChargeTime(int spellLevel, LivingEntity caster) {
        // todo:バランス調整.
        return 300;
    }

    private int getFullPowerRate(int spellLevel, LivingEntity caster) {
        // todo:バランス調整.
        return 1000;
    }

    private int getCurrentCastDurationTicks(@Nullable MagicData playerMagicData) {
        if (playerMagicData == null) {
            return 0;
        }

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
    public boolean canBeInterrupted(@Nullable Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_NETHERITE);
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
    public MoonLightKatanaEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new MoonLightKatanaEntity(EntityRegistry.MOON_LIGHT_KATANA.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull MoonLightKatanaEntity weapon) {
        if (!weapon.isStandby() && weapon.tickCount >= STANDBY_START_DELAY_TICK) {
            weapon.setStandby();
        }

        var castDurationTicks = getCurrentCastDurationTicks(playerMagicData);
        var requiredChargeTime = getRequiredChargeTime(spellLevel, entity);
        weapon.setChargingEffectActive(true);
        weapon.setFullyChargedEffect(castDurationTicks > requiredChargeTime);
        playChargeThresholdSound(level, entity, castDurationTicks, requiredChargeTime);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull MoonLightKatanaEntity weapon) {
        var castDurationTicks = getCurrentCastDurationTicks(playerMagicData);
        var requiredChargeTime = getRequiredChargeTime(spellLevel, entity);
        weapon.setChargingEffectActive(false);
        weapon.setFullyChargedEffect(false);

        if (!cancelled) {
            onCastTimedOut(level, entity, playerMagicData);
            return CompleteCastTypes.RELEASE_WEAPON;
        }

        if (castDurationTicks < MINIMUM_CHARGE_TICK) {
            weapon.setDamage(getDamage(spellLevel, entity));
            weapon.slash(level);
            consumeManaWithFloor(playerMagicData, NO_CHARGE_MANA_COST);
            return CompleteCastTypes.KEEP_WEAPON;
        }

        if (castDurationTicks < requiredChargeTime) {
            weapon.setDamage(getDamage(spellLevel, entity));
            weapon.slash(level);
            consumeManaWithFloor(playerMagicData, NORMAL_CHARGE_MANA_COST);
            return CompleteCastTypes.KEEP_WEAPON;
        }

        var fullPowerRate = getFullPowerRate(spellLevel, entity) / 100.0f;
        weapon.setDamage(getDamage(spellLevel, entity) * fullPowerRate);
        weapon.slash(level);
        setManaWithFloor(playerMagicData, 0.0f);
        return CompleteCastTypes.KEEP_WEAPON;
    }

    private void onCastTimedOut(Level level, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var centerY = entity.getY() + entity.getBbHeight() * 0.6D;
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    entity.getX(),
                    centerY,
                    entity.getZ(),
                    20,
                    0.25D,
                    0.2D,
                    0.25D,
                    0.02D
            );
        }

        playNotifyOrAreaSound(level, entity, SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
        reduceManaByHalf(playerMagicData);
    }

    private void playChargeThresholdSound(Level level, LivingEntity entity, int castDurationTicks, int requiredChargeTime) {
        if (castDurationTicks == MINIMUM_CHARGE_TICK) {
            playNotifyOrAreaSound(level, entity, SoundEvents.ITEM_PICKUP, 1.0f, 1.2f);
        }

        if (castDurationTicks == requiredChargeTime) {
            playNotifyOrAreaSound(level, entity, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.0f);
        }
    }

    private void playNotifyOrAreaSound(Level level, LivingEntity entity, SoundEvent soundEvent, float volume, float pitch) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(soundEvent, SoundSource.PLAYERS, volume, pitch);
            return;
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, SoundSource.PLAYERS, volume, pitch);
    }

    private void consumeManaWithFloor(@Nullable MagicData playerMagicData, float consumeAmount) {
        if (playerMagicData == null || consumeAmount <= 0.0f) {
            return;
        }

        var currentMana = Math.max(0.0f, playerMagicData.getMana());
        var nextMana = Math.max(0.0f, currentMana - consumeAmount);
        playerMagicData.setMana(nextMana);
    }

    private void reduceManaByHalf(@Nullable MagicData playerMagicData) {
        if (playerMagicData == null) {
            return;
        }

        var currentMana = Math.max(0.0f, playerMagicData.getMana());
        playerMagicData.setMana(currentMana * 0.5f);
    }

    private void setManaWithFloor(@Nullable MagicData playerMagicData, float mana) {
        if (playerMagicData == null) {
            return;
        }

        playerMagicData.setMana(Math.max(0.0f, mana));
    }
}
