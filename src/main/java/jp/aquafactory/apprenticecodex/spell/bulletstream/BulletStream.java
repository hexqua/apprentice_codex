package jp.aquafactory.apprenticecodex.spell.bulletstream;

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

import java.util.List;
import java.util.Optional;

public class BulletStream extends AbstractSummonWeaponSpell<BulletStreamMinigunEntity> {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bullet_stream");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(20)
            .build();

    public BulletStream() {
        super(BulletStreamMinigunEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        manaCostPerLevel = 5;
        baseManaCost = 10;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.start_up_time", Utils.timeFromTicks(getWarmUpStartTick(spellLevel,caster), 1)),
                Component.translatable("ui.apprenticecodex.warm_up_time", Utils.timeFromTicks(getWarmUpFinishTick(spellLevel,caster), 1)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // スペルパワーはintのため、設定値をそもそも100倍として考える.
        return 1 * getSpellPower(spellLevel, entity) / 100.0f;
    }

    private int getRange() {
        return 16 * 2;
    }

    private int getWarmUpBaseDelayTick(){
        return 8;
    }

    private int getWarmUpStartTick(int spellLevel, LivingEntity entity) {
        return Math.round(Math.max(5f, 30f - 10f * getSpellPower(spellLevel, entity) / 100.0f));
    }

    private int getWarmUpFinishTick(int spellLevel, LivingEntity entity){
        return Math.round(Math.max(20f, 80f - 20f * getSpellPower(spellLevel, entity) / 100.0f));
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
        return AnimationHolder.none();
    }

    @Override
    public BulletStreamMinigunEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new BulletStreamMinigunEntity(EntityRegistry.BULLET_STREAM_MINIGUN.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRange(getRange());
        summonWeapon.setTickSettings(getWarmUpBaseDelayTick(), getWarmUpStartTick(spellLevel, entity), getWarmUpFinishTick(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull BulletStreamMinigunEntity weapon) {
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull BulletStreamMinigunEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
