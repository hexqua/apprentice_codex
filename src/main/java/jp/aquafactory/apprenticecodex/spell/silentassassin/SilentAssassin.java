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
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import jp.aquafactory.apprenticecodex.spell.IMagiAgentSuitAffectedSpell;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SilentAssassin extends AbstractSummonWeaponSpell<SilentAssassinRifleEntity> implements IMagiAgentSuitAffectedSpell {
    private static final double AWARENESS_SUPPRESSION_RADIUS = 16.0D;
    private static final float ALMOST_FULL_HEALTH_RATIO = 0.995F;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "silent_assassin");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(16)
            .build();

    public SilentAssassin() {
        super(SilentAssassinRifleEntity.class);
        baseSpellPower = 40;
        spellPowerPerLevel = 60;
        baseManaCost = 100;
        manaCostPerLevel = 40;
        castTime = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        var spellPower = getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(), 2)),
                Component.translatable("ui.apprenticecodex.headshot_damage_multiplier", getHeadshotPercent(spellPower)),
                Component.translatable("ui.apprenticecodex.almost_full_health_damage_multiplier", getFullHealthDamageBonusPercent()),
                Component.translatable("ui.irons_spellbooks.distance", getRange())
        );
    }

    private float getDamage() {
        var rawDamage = 15;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SILENT_ASSASSIN);
    }

    private int getHeadshotPercent(float spellPower) {
        return Math.min(1000, 100 + Math.round(spellPower));
    }

    private int getFullHealthDamageBonusPercent() {
        return 200;
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
    public boolean canBeInterrupted(@Nullable Player player) {
        return canBeInterruptedWithMagiAgentSuit(this, player, super.canBeInterrupted(player));
    }

    @Override
    public final Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
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

        if (result.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(result.hitEntity());
            var currentSpellPower = getSpellPower(spellLevel, entity);
            var finalDamage = getDamage();
            if (isHeadShot) {
                finalDamage *= getHeadshotPercent(currentSpellPower) / 100.0f;
            }

            // AIの攻撃対象ではなく、軽減前の初撃条件でボーナスを決める。
            boolean hasAlmostFullHealthBonus = isHeadShot && target instanceof LivingEntity livingTarget
                    && livingTarget.getHealth() >= livingTarget.getMaxHealth() * ALMOST_FULL_HEALTH_RATIO;
            if (hasAlmostFullHealthBonus) {
                finalDamage *= getFullHealthDamageBonusPercent() / 100.0f;
            }

            weapon.damageTarget(target, finalDamage, level);
            // 耐性・防具・無敵による不発を暗殺成功と扱わず、実際に倒した場合だけ認識を解除する。
            if (hasAlmostFullHealthBonus && target instanceof LivingEntity livingTarget && livingTarget.isDeadOrDying()) {
                SummonedFirearmTools.suppressNearbyAwareness(level, entity, target, AWARENESS_SUPPRESSION_RADIUS);
            }
        }

        var hitType = switch (result.hitType()) {
            case NONE -> SilentAssassinRifleEntity.HitTypes.MISS;
            case BLOCK -> SilentAssassinRifleEntity.HitTypes.BLOCK;
            case LIVING_ENTITY -> SilentAssassinRifleEntity.HitTypes.ENTITY;
        };

        weapon.fire(result.hitPosition(), level, hitType, isHeadShot);
        return CompleteCastTypes.KEEP_WEAPON;
    }
}
