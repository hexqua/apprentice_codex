package jp.aquafactory.apprenticecodex.spell.commencefire;

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
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponRecastSpell;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CommenceFire extends AbstractSummonWeaponRecastSpell<CommenceFireRifleEntity> {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "commence_fire");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public CommenceFire() {
        super(CommenceFireRifleEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 30;
        baseManaCost = 100;
        manaCostPerLevel = 30;
        castTime = 30;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.recast_count", getActivateCount(spellLevel, caster)),
                Component.translatable("ui.apprenticecodex.headshot_damage_multiplier", getHeadshotPercent(spellLevel, caster))
        );
    }

    private float getOverSpellPower(int spellLevel, LivingEntity entity){
        return getSpellPower(spellLevel, entity) - baseSpellPower;
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 3 * (getSpellPower(spellLevel, entity) / 100.0f);
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.COMMENCE_FIRE);
    }

    @Override
    public int getActivateCount(int spellLevel, LivingEntity entity) {
        return Math.min(10, 4 + Math.round(2 * (getOverSpellPower(spellLevel, entity) / 100.0f)));
    }

    @Override
    public int getDurationTick() {
        return 20 * 10;
    }

    @Override
    public Optional<SoundEvent> getPreFireSound() {
        return Optional.of(SoundEvents.ARMOR_EQUIP_NETHERITE.value());
    }
    @Override
    public Optional<SoundEvent> getPreSummonSound() {
        return Optional.of(getSchoolType().getCastSound());
    }
    @Override
    public Optional<SoundEvent> getFireSound() {
        return Optional.empty();
    }
    @Override
    public Optional<SoundEvent> getSummonSound() {
        return Optional.of(SoundEvents.SHULKER_TELEPORT);
    }

    private int getRange(){
        // DMRイメージなので中距離(4チャンク程度)
        return 16 * 4;
    }

    private int getHeadshotPercent(int spellLevel, LivingEntity entity) {
        return Math.min(400, 150 + Math.round(30 * (getOverSpellPower(spellLevel, entity) / 100.0f)));
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
        if (entity == null) {
            return getCastTime(spellLevel);
        }

        var magicData = MagicData.getPlayerMagicData(entity);
        if (magicData == null){
            return super.getEffectiveCastTime(spellLevel, entity);
        }

        var recasts = magicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this)){
            return super.getEffectiveCastTime(spellLevel, entity);
        }

        // 射撃は固定値(0.5秒)
        return 10;
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
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        var summon = getSummonEntityFromMagicData(playerMagicData, level);
        if (summon == null) {
            return;
        }

        var result = resolvePlayerAim(entity);

        // 上の判定式で非nullが保証.
        //noinspection DataFlowIssue
        var castTick = playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining();
        summon.setCastingReticleEffect(castTick, playerMagicData.getCastDuration(), result.hitPosition());
    }

    @Override
    protected boolean onPreRecastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull CommenceFireRifleEntity weapon) {
        if (weapon.duringRecoil()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.commence_fire.during_recoil", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        return true;
    }

    @Override
    protected boolean onPreRecastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return false;
    }

    @Override
    public CompleteRecastTypes onRecastFinishedWithWeapon(Level level, ServerPlayer serverPlayer, @NotNull CommenceFireRifleEntity weapon) {
        return CompleteRecastTypes.RELEASE_WEAPON;
    }

    @Override
    public void onCastWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull CommenceFireRifleEntity weapon){
        var result = resolvePlayerAim(entity);
        var isHeadShot = SummonedFirearmTools.isHeadShot(result);
        if (result.hitEntity() != null) {
            weapon.damageTarget(result.hitEntity(), isHeadShot, level);
        }

        var hitType = switch (result.hitType()) {
            case NONE -> CommenceFireRifleEntity.HitTypes.MISS;
            case BLOCK -> CommenceFireRifleEntity.HitTypes.BLOCK;
            case LIVING_ENTITY -> CommenceFireRifleEntity.HitTypes.ENTITY;
        };

        weapon.fire(result.hitPosition(), level, hitType, isHeadShot);
    }

    @Override
    public CommenceFireRifleEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData){
        var summonWeapon = new CommenceFireRifleEntity(EntityRegistry.COMMENCE_FIRE_RIFLE.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity), getHeadshotPercent(spellLevel, entity));
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    private RaycastTools.TargetResult resolvePlayerAim(LivingEntity caster) {
        return resolvePlayerAim(caster, e -> CombatTools.isValidCombatTarget(e, caster));
    }

    private RaycastTools.TargetResult resolvePlayerAim(LivingEntity caster, Predicate<Entity> predicate) {
        return SummonedFirearmTools.resolveAssistedAim(caster, getRange(), predicate);
    }
}
