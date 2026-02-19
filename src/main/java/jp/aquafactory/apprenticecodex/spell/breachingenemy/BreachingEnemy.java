package jp.aquafactory.apprenticecodex.spell.breachingenemy;

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

public class BreachingEnemy extends AbstractSummonWeaponSpell<BreachingEnemyShotgunEntity> {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "breaching_enemy");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public BreachingEnemy() {
        super(BreachingEnemyShotgunEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        manaCostPerLevel = 15;
        baseManaCost = 60;
        castTime = 20;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getPellets(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return 1 + 3 * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getPellets(int spellLevel, LivingEntity entity){
        return 4 + Math.round(1.5f * getSpellPower(spellLevel, entity) / 100.0f);
    }

    private int getRange(){
        // ショットガンイメージなので近距離(1チャンク未満)
        return 10;
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
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        // 詠唱時間短縮をガン積みしてもかならず10tickは残す.
        return Math.max(super.getEffectiveCastTime(spellLevel, entity), 10);
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
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public BreachingEnemyShotgunEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new BreachingEnemyShotgunEntity(EntityRegistry.BREACHING_ENEMY_SHOTGUN.get(),level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRange(getRange());
        summonWeapon.setCount(getPellets(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull BreachingEnemyShotgunEntity weapon) {
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull BreachingEnemyShotgunEntity weapon) {
        if (!cancelled) {
            weapon.fire(level);
            return CompleteCastTypes.KEEP_WEAPON;
        }
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
