package jp.aquafactory.apprenticecodex.spell.precisionjack;

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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class PrecisionJack extends AbstractSummonWeaponSpell<PrecisionJackKnifeEntity> {
    private static final int MAX_LOOTING_BONUS = 5;
    private static final int MAX_DUPLICATE_DROP_CHANCE_PERCENT = 30;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "precision_jack");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(1)
            .build();

    public PrecisionJack() {
        super(PrecisionJackKnifeEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 60;
        manaCostPerLevel = 30;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(), 2)),
                Component.translatable("ui.apprenticecodex.looting_level", getLootingBonus(spellLevel, caster)),
                Component.translatable("ui.apprenticecodex.duplicate_drop_chance", getDuplicateDropChancePercent(spellLevel, caster))
        );
    }

    private float getDamage() {
        return 6 * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.PRECISION_JACK);
    }

    private int getLootingBonus(int spellLevel, LivingEntity entity) {
        return Math.min(MAX_LOOTING_BONUS, Math.max(1, Math.round(getSpellPower(spellLevel, entity) / 100.0f)));
    }

    private int getDuplicateDropChancePercent(int spellLevel, LivingEntity entity) {
        return Math.min(MAX_DUPLICATE_DROP_CHANCE_PERCENT, Math.max(0, Math.round(getSpellPower(spellLevel, entity) / 10.0f)));
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
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public boolean canBeInterrupted(@Nullable net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_NETHERITE.value());
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
    public PrecisionJackKnifeEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new PrecisionJackKnifeEntity(EntityRegistry.PRECISION_JACK_KNIFE.get(), level, entity);
        summonWeapon.setDamage(getDamage());
        summonWeapon.setLootingBonus(getLootingBonus(spellLevel, entity));
        summonWeapon.setDuplicateDropChancePercent(getDuplicateDropChancePercent(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull PrecisionJackKnifeEntity weapon) {
        weapon.prepare();
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull PrecisionJackKnifeEntity weapon) {
        if (cancelled) {
            return CompleteCastTypes.RELEASE_WEAPON;
        }

        weapon.slice(level);
        return CompleteCastTypes.KEEP_WEAPON;
    }
}
