package jp.aquafactory.apprenticecodex.spell.higanbana;

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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Higanbana extends AbstractSummonWeaponSpell<HiganbanaKatanaEntity> {
    private static final int FIRST_SLASH_DELAY_TICK = 5;
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "higanbana");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.BLOOD_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public Higanbana() {
        super(HiganbanaKatanaEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 40;
        manaCostPerLevel = 20;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.slash_count", getSlashCount(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 1 + getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.HIGANBANA);
    }

    public int getSlashCount(int spellLevel, @Nullable LivingEntity entity) {
        // Recast 時代の初回発動を含む総斬撃回数を維持する。
        return Math.min(8, Math.max(1, Math.round(getSpellPower(spellLevel, entity) / 200.0f))) + 1;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
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
        return CastType.INSTANT;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData,
                                     @NotNull HiganbanaKatanaEntity weapon) {
    }

    @Override
    public HiganbanaKatanaEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRemainingSlashCount(getSlashCount(spellLevel, entity));
        summonWeapon.setFirstSlashStandby(FIRST_SLASH_DELAY_TICK);
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                                       MagicData playerMagicData, boolean cancelled,
                                                       @NotNull HiganbanaKatanaEntity weapon) {
        return cancelled ? CompleteCastTypes.RELEASE_WEAPON : CompleteCastTypes.KEEP_WEAPON;
    }
}
