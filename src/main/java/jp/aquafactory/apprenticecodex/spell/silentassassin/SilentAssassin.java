package jp.aquafactory.apprenticecodex.spell.silentassassin;

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
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class SilentAssassin extends AbstractSummonWeaponSpell<SilentAssassinRifleEntity> {
    private static final double AWARENESS_SUPPRESSION_RADIUS = 16.0D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "silent_assassin");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(16)
            .build();

    public SilentAssassin() {
        super(SilentAssassinRifleEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 100;
        manaCostPerLevel = 40;
        castTime = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        var spellPower = getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellPower), 2)),
                Component.translatable("ui.apprenticecodex.headshot_damage_multiplier", getHeadshotPercent(spellPower)),
                Component.translatable("ui.apprenticecodex.sneak_damage_multiplier", getSneakPercent(spellPower))
        );
    }

    private float getDamage(float spellPower) {
        var rawDamage = 10 * (spellPower / 100.0f);
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SILENT_ASSASSIN);
    }

    private int getHeadshotPercent(float spellPower) {
        return Math.min(500, 100 + Math.round(50 * (spellPower / 100.0f)));
    }

    private int getSneakPercent(float spellPower) {
        return Math.min(500, 100 + Math.round(75 * (spellPower / 100.0f)));
    }

    private int getRange(){
        // SRイメージなので超距離(8チャンク程度)
        return 16 * 8;
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
    public final Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public final Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public SilentAssassinRifleEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new SilentAssassinRifleEntity(EntityRegistry.SILENT_ASSASSIN_RIFLE.get(), level, entity);
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData,
                                     @NotNull SilentAssassinRifleEntity weapon) {
        var result = SummonedFirearmTools.resolveAssistedAim(entity, getRange(), e -> CombatTools.isValidCombatTarget(e, entity));
        var castTick = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        weapon.setCastingReticleEffect(castTick, playerMagicData.getCastDuration(), result.hitPosition());
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                                      MagicData playerMagicData, boolean cancelled,
                                                      @NotNull SilentAssassinRifleEntity weapon) {
        if (cancelled) {
            return CompleteCastTypes.RELEASE_WEAPON;
        }

        var result = SummonedFirearmTools.resolveAssistedAim(entity, getRange(), e -> CombatTools.isValidCombatTarget(e, entity));
        var isHeadShot = SummonedFirearmTools.isHeadShot(result);

        boolean hasUnawareBonus = false;
        if (result.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(result.hitEntity());
            var currentSpellPower = getSpellPower(spellLevel, entity);
            var finalDamage = getDamage(currentSpellPower);
            if (isHeadShot) {
                finalDamage *= getHeadshotPercent(currentSpellPower) / 100.0f;
            }

            hasUnawareBonus = SummonedFirearmTools.shouldApplyUnawareBonus(target, entity);
            if (hasUnawareBonus) {
                finalDamage *= getSneakPercent(currentSpellPower) / 100.0f;
            }

            weapon.damageTarget(target, finalDamage, level);
            if (hasUnawareBonus && target instanceof LivingEntity livingTarget && finalDamage >= livingTarget.getHealth()) {
                SummonedFirearmTools.suppressNearbyAwareness(level, entity, target, AWARENESS_SUPPRESSION_RADIUS);
            }
        }

        var hitType = switch (result.hitType()) {
            case NONE -> SilentAssassinRifleEntity.HitTypes.MISS;
            case BLOCK -> SilentAssassinRifleEntity.HitTypes.BLOCK;
            case LIVING_ENTITY -> SilentAssassinRifleEntity.HitTypes.ENTITY;
        };

        weapon.fire(result.hitPosition(), level, hitType, isHeadShot, hasUnawareBonus);
        return CompleteCastTypes.KEEP_WEAPON;
    }
}
