package jp.aquafactory.apprenticecodex.spell.thermalprocess;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.IMagiAgentSuitAffectedSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ThermalProcess extends AbstractSummonWeaponSpell<ThermalProcessThrowerEntity> implements ICraftsmansDelightAffectedSpell, IMagiAgentSuitAffectedSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "thermal_process");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .build();

    public ThermalProcess() {
        super(ThermalProcessThrowerEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 150;
        baseManaCost = 10;
        manaCostPerLevel = 5;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1)),
                Component.translatable("ui.apprenticecodex.process_item_per_second", Utils.stringTruncation(getBurnItemPerSecond(spellLevel, caster), 1))
        );
    }

    static float getDamage(float spellPower) {
        var rawDamage = 0.5f + 1.5f * spellPower / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.THERMAL_PROCESS);
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getDamage(getSpellPower(spellLevel, entity));
    }

    private float getRange() {
        return 12f;
    }

    private float getBurnItemPerSecond(int spellLevel, LivingEntity entity) {
        var baseProcessSpeed = 2 * getSpellPower(spellLevel, entity) / 100.0f;
        if (!isCraftsmansDelightProcessSpeedBonusEnabled()) {
            return baseProcessSpeed;
        }

        return CraftsmansDelight.applyProcessSpeedBonus(baseProcessSpeed, entity);
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public boolean isCraftsmansDelightCastingMobilityEnabled() {
        return true;
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
    public boolean canBeInterrupted(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player player) {
        return canBeInterruptedWithMagiAgentSuit(this, player, super.canBeInterrupted(player));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(jp.aquafactory.apprenticecodex.registry.SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_BREATH_LOOP.get());
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
    public ThermalProcessThrowerEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new ThermalProcessThrowerEntity(EntityRegistry.THERMAL_PROCESS_THROWER.get(), level, entity);
        summonWeapon.setSpellLevel(spellLevel);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRange(getRange());
        summonWeapon.setBurnItemPerSecond(getBurnItemPerSecond(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        if (isCraftsmansDelightCastingMobilityEnabled()) {
            CraftsmansDelight.applyCastingMobility(entity);
        }
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull ThermalProcessThrowerEntity weapon) {
        if (isCraftsmansDelightCastingMobilityEnabled()) {
            CraftsmansDelight.applyCastingMobility(entity);
        }
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull ThermalProcessThrowerEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
