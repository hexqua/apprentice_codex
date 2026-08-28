package jp.aquafactory.apprenticecodex.spell.shiden;

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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Shiden extends AbstractSummonWeaponSpell<ShidenKatanaEntity> {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "shiden");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(10)
            .build();

    public Shiden() {
        super(ShidenKatanaEntity.class);
        baseSpellPower = 1600;
        spellPowerPerLevel = 400;
        baseManaCost = 50;
        manaCostPerLevel = 15;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1)),
                Component.translatable("ui.apprenticecodex.block_penetration_damage_multiplier", Utils.stringTruncation(getBlockPenetrationDamageMultiplierPercent(spellLevel), 0))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SHIDEN);
    }

    private double getRange(){
        // あくまでも理想値であり、壁向きに張り付いて使うとmoveでめり込まない都合上射程が短くなるが仕様.
        return ShidenKatanaEntity.getAttackDepth();
    }

    private int getBlockPenetrationDamageMultiplierPercent(int spellLevel){
        return Mth.clamp(20 + spellLevel * 5, 10, 90);
    }

    private float getBlockPenetrationDamageMultiplier(int spellLevel) {
        return getBlockPenetrationDamageMultiplierPercent(spellLevel) / 100.0F;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public ShidenKatanaEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setBlockPenetrationDamageMultiplier(getBlockPenetrationDamageMultiplier(spellLevel));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    protected void onInitialCastWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                           MagicData playerMagicData, @NotNull ShidenKatanaEntity weapon) {
        // 発動前にチャージできる魔法道具の完了時補正を、直後に行う抜刀攻撃へ反映する。
        weapon.setDamage(getDamage(spellLevel, entity));
        weapon.setBlockPenetrationDamageMultiplier(getBlockPenetrationDamageMultiplier(spellLevel));
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull ShidenKatanaEntity weapon) {
        if (!weapon.isStandby()){
            weapon.setStandby(getCastTimeSpeedScale(spellLevel, playerMagicData));
        }
    }

    private float getCastTimeSpeedScale(int spellLevel, @Nullable MagicData playerMagicData) {
        var baseCastTime = Math.max(0, getCastTime(spellLevel));
        if (baseCastTime <= 0 || playerMagicData == null) {
            return 1.0f;
        }

        var actualCastTime = Math.max(1, playerMagicData.getCastDuration());
        return Math.max(1.0f, baseCastTime / (float) actualCastTime);
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull ShidenKatanaEntity weapon) {
        if (cancelled) {
            return CompleteCastTypes.RELEASE_WEAPON;
        }

        weapon.slash(level);
        return CompleteCastTypes.KEEP_WEAPON;
    }
}
