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
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
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

public class BulletStream extends AbstractSummonWeaponSpell<BulletStreamMinigunEntity> implements IMagiAgentSuitAffectedSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "bullet_stream");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(12)
            .build();

    public BulletStream() {
        super(BulletStreamMinigunEntity.class);
        baseSpellPower = 20;
        spellPowerPerLevel = 100;
        manaCostPerLevel = 2;
        baseManaCost = 3;
        castTime = 600;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.bullet_stream.spinup_time", Utils.timeFromTicks(getSpinUpDelayTick(), 1)),
                Component.translatable("ui.irons_spellbooks.distance", getRange())
        );
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        // 長時間照射そのものを強みにするため、装備による詠唱時間の増減は乗せない.
        return getCastTime(spellLevel);
    }

    static float getDamage(float spellPower) {
        var rawDamage = 0.5f + spellPower / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.BULLET_STREAM);
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getDamage(getSpellPower(spellLevel, entity));
    }

    private int getRange() {
        return 16 * 2;
    }

    private int getSpinUpDelayTick(){
        return 40;
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
    public boolean canBeInterrupted(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player player) {
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
        return AnimationHolder.none();
    }

    @Override
    public BulletStreamMinigunEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new BulletStreamMinigunEntity(EntityRegistry.BULLET_STREAM_MINIGUN.get(), level, entity);
        summonWeapon.setSpellLevel(spellLevel);
        summonWeapon.setDamage(getDamage(spellLevel, entity));
        summonWeapon.setRange(getRange());
        summonWeapon.setSpinUpDelayTick(getSpinUpDelayTick());
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
