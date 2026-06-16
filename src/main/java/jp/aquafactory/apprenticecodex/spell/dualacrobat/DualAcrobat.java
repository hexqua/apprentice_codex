package jp.aquafactory.apprenticecodex.spell.dualacrobat;

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
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class DualAcrobat extends AbstractSummonWeaponSpell<DualAcrobatSmgEntity> {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "dual_acrobat");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public DualAcrobat() {
        super(DualAcrobatSmgEntity.class);
        baseSpellPower = 500;
        spellPowerPerLevel = 100;
        baseManaCost = 5;
        manaCostPerLevel = 5;
        castTime = 100;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, @Nullable LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(), 2)),
                Component.translatable("ui.apprenticecodex.charge_up_ammo_per_second", Utils.stringTruncation(getLoadAmmoCountSpeed(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.charge_up_ammo_maximum", getMaximumLoadAmmoCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.distance", getRange())
        );
    }

    private float getDamage() {
        var rawDamage = 2;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.DUAL_ACROBAT);
    }

    private float getLoadAmmoCountSpeed(int spellLevel, @Nullable LivingEntity entity) {
        return getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getMaximumLoadAmmoCount(int spellLevel, @Nullable LivingEntity entity) {
        return Math.round(getLoadAmmoCountSpeed(spellLevel, entity) * 5);
    }

    public int getRange(){
        return 24;
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
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
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
    public DualAcrobatSmgEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new DualAcrobatSmgEntity(EntityRegistry.DUAL_ACROBAT_SMG.get(), level, entity);
        summonWeapon.setDamage(getDamage());
        summonWeapon.setRange(getRange());
        summonWeapon.setLoadAmmoCountSpeed(getLoadAmmoCountSpeed(spellLevel, entity));
        summonWeapon.setMaximumLoadAmmoCount(getMaximumLoadAmmoCount(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData,
                                     @NotNull DualAcrobatSmgEntity weapon) {
        weapon.loadAmmo();
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                                      MagicData playerMagicData, boolean cancelled,
                                                      @NotNull DualAcrobatSmgEntity weapon) {
        if (DualAcrobatCounterSpellEvent.consumeCounterspellInterrupted(entity)) {
            weapon.startCounterspellInterruptedShooting();
            return CompleteCastTypes.KEEP_WEAPON;
        }

        AudioTools.playSoundFromEntity(level, entity, SoundRegistry.VANILLA_HOLD_WEAPON.get(), SoundSource.PLAYERS);
        weapon.startShooting();
        return CompleteCastTypes.KEEP_WEAPON;
    }
}
