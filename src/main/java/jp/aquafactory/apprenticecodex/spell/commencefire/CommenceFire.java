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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CommenceFire extends AbstractSummonWeaponRecastSpell<CommenceFireRifleEntity> {
    private static final double AIM_SEARCH_WIDTH = 0.875D;
    private static final double AIM_ASSIST_INFLATE = 0.35D;
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
        spellPowerPerLevel = 25;
        manaCostPerLevel = 25;
        baseManaCost = 150;
        castTime = 40;
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
        return Math.min(300, 150 + Math.round(25 * (getOverSpellPower(spellLevel, entity) / 100.0f)));
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
        var isHeadShot = result.hitEntity() instanceof LivingEntity living && CombatTools.isHeadShot(living, result.hitPosition());
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
        var level = caster.level();
        var look = caster.getViewVector(1.0F);
        var start = caster.getEyePosition(1.0F);
        var end = start.add(look.scale(getRange()));

        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));

        var effectiveEnd = blockHit.getType() == HitResult.Type.MISS
                ? end
                : blockHit.getLocation();
        var searchBox = caster.getBoundingBox()
                .expandTowards(look.scale(getRange()))
                .inflate(AIM_SEARCH_WIDTH / 2);

        var entityHit = resolveEntityHit(level, caster, start, effectiveEnd, searchBox, predicate);
        if (entityHit.isPresent()) {
            var hit = entityHit.get();
            return new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY,
                    hit.hitPosition(),
                    hit.entity(),
                    null
            );
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            return new RaycastTools.TargetResult(
                    RaycastTools.TargetType.BLOCK,
                    blockHit.getLocation(),
                    null,
                    blockHit.getBlockPos()
            );
        }

        return new RaycastTools.TargetResult(RaycastTools.TargetType.NONE, end, null, null);
    }

    private Optional<EntityAimHit> resolveEntityHit(
            Level level,
            LivingEntity caster,
            Vec3 start,
            Vec3 end,
            AABB searchBox,
            Predicate<Entity> predicate
    ) {
        EntityAimHit closest = null;
        var closestDistanceSqr = Double.MAX_VALUE;

        for (var candidate : level.getEntities(caster, searchBox, e -> e.isAlive() && predicate.test(e))) {
            // getEntityHitResult は命中座標がエンティティ基準位置へ落ちるため使わず、少し太らせたAABBと視線の交点だけを採用する.
            var hitPosition = candidate.getBoundingBox().inflate(AIM_ASSIST_INFLATE).clip(start, end);
            if (hitPosition.isEmpty()) {
                continue;
            }

            var distanceSqr = start.distanceToSqr(hitPosition.get());
            if (distanceSqr < closestDistanceSqr) {
                closest = new EntityAimHit(candidate, hitPosition.get());
                closestDistanceSqr = distanceSqr;
            }
        }

        return Optional.ofNullable(closest);
    }

    private record EntityAimHit(Entity entity, Vec3 hitPosition) {}
}
