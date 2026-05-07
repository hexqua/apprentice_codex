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
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MoonLight extends AbstractSummonWeaponSpell<MoonLightKatanaEntity> {
    private static final int STANDBY_START_DELAY_TICK = 10;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "moon_light");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public MoonLight() {
        super(MoonLightKatanaEntity.class);
        baseSpellPower = 1200;
        spellPowerPerLevel = 900;
        baseManaCost = 120;
        manaCostPerLevel = 30;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getDistance(spellLevel), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MOON_LIGHT);
    }

    private double getDistance(int spellLevel) {
        return 4 + (spellLevel - 1) * 2.5;
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
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_HOLD_WEAPON.get());
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

        weapon.setChargingEffectActive(true);
        weapon.setFullyChargedEffect(false);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull MoonLightKatanaEntity weapon) {
        weapon.setChargingEffectActive(false);
        weapon.setFullyChargedEffect(false);

        if (cancelled) {
            return CompleteCastTypes.RELEASE_WEAPON;
        }

        var damage = getDamage(spellLevel, entity);
        weapon.setDamage(damage);
        weapon.slash(level);
        spawnChargeCut(level, spellLevel, entity, damage);
        return CompleteCastTypes.KEEP_WEAPON;
    }

    private void spawnChargeCut(Level level, int spellLevel, LivingEntity caster, float damage) {
        if (level.isClientSide) {
            return;
        }

        var direction = caster.getLookAngle();
        if (direction.lengthSqr() < 1.0e-6) {
            direction = Vec3.directionFromRotation(caster.getXRot(), caster.getYRot());
        }
        direction = direction.normalize();

        var startPosition = caster.position().add(direction.scale(
                MoonLightChargeCutEntity.START_OFFSET_BLOCKS + MoonLightChargeCutEntity.SURFACE_OFFSET_BLOCKS
        ));
        var cutArea = new MoonLightChargeCutEntity(EntityRegistry.MOON_LIGHT_CHARGE_CUT.get(), level, caster);
        cutArea.setPos(startPosition.x, startPosition.y, startPosition.z);
        cutArea.setYRot(caster.getYRot());
        cutArea.setXRot(caster.getXRot());
        cutArea.setup((float) getDistance(spellLevel), damage);
        level.addFreshEntity(cutArea);
    }
}
