package jp.aquafactory.apprenticecodex.spell.catchflame;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmoker;
import jp.aquafactory.apprenticecodex.block.essencesmoker.EssenceSmokerBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserCastContext;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastContext;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CatchFlame extends AbstractSpell {
    private static final double EFFECT_HALF_SIZE = 0.5D;
    private static final double BLOCK_SURFACE_EPSILON = 1.0E-4D;
    private static final int FIRE_PARTICLE_COUNT = 6;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "catch_flame");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(8)
            .setCooldownSeconds(0.5)
            .build();

    public CatchFlame() {
        baseSpellPower = 800;
        spellPowerPerLevel = 75;
        baseManaCost = 10;
        manaCostPerLevel = 3;
        castTime = 10;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0)),
                Component.translatable("ui.apprenticecodex.burn_time", Utils.timeFromTicks(getBurnDuration(spellLevel), 1))
        );
    }

    float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0F;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.CATCH_FLAME);
    }

    int getBurnDuration(int spellLevel) {
        // Lv1は見た目だけの炎上(1秒おきのダメージのため)、Lv2以上で炎上ダメージを期待させる.
        return Math.max((spellLevel - 1) * 20, 10);
    }

    static float getRange() {
        // バニラの手の長さに依存せず、かなり短め.
        return 3.0F;
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
        return Optional.of(SoundRegistry.FIRE_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_IMPACT.get());
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
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            var target = resolveCastTarget(level, caster);
            tryIgniteBlock(level, caster, target);
            affectEntities(level, spellLevel, caster, target);
            spawnImpactParticles(level, target.impactPosition());
            spawnImpactEffect(level, caster, target.impactPosition());
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    private CastTarget resolveCastTarget(Level level, LivingEntity caster) {
        var start = caster.getEyePosition(1.0F);
        var forward = caster.getViewVector(1.0F).normalize();
        var combatActor = resolveCombatActor(level, caster);
        var result = RaycastTools.raycast(
                caster,
                forward,
                getRange(),
                0.0D,
                candidate -> candidate instanceof LivingEntity
                        && CombatTools.isValidCombatTarget(candidate, combatActor)
        );

        if (result.hitEntity() != null) {
            return new CastTarget(result.hitPosition(), null, null);
        }

        if (result.hitType() == RaycastTools.TargetType.BLOCK && result.hitBlock() != null) {
            var blockHit = clipBlock(level, caster, start, start.add(forward.scale(getRange())));
            return new CastTarget(result.hitPosition(), result.hitBlock(), blockHit.getDirection());
        }

        return new CastTarget(start.add(forward.scale(getRange())), null, null);
    }

    private static BlockHitResult clipBlock(Level level, LivingEntity caster, Vec3 start, Vec3 end) {
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
    }

    private void affectEntities(Level level, int spellLevel, LivingEntity caster, CastTarget castTarget) {
        var combatActor = resolveCombatActor(level, caster);
        var effectBounds = new AABB(castTarget.impactPosition(), castTarget.impactPosition()).inflate(EFFECT_HALF_SIZE);
        var sightOrigin = castTarget.lineOfSightOrigin();
        var rawTargets = level.getEntities(caster, effectBounds, Entity::isAlive);

        for (var target : CombatTools.resolveUniqueCombatTargets(rawTargets)) {
            if (!target.getBoundingBox().intersects(effectBounds)
                    || !CombatTools.isValidCombatTarget(target, combatActor)
                    || !RaycastTools.hasLineOfSight(level, caster, sightOrigin, target)) {
                continue;
            }

            var damageType = target.getRemainingFireTicks() > 0
                    ? DamageTypes.CATCH_FLAME_PENETRATE
                    : DamageTypes.CATCH_FLAME;
            var source = combatActor != caster
                    ? CombatTools.getDamageSource(level, caster, combatActor, damageType)
                    : CombatTools.getDamageSource(level, caster, damageType);
            var damaged = CombatTools.applyDamage(
                    target,
                    getDamage(spellLevel, caster),
                    source,
                    getSchoolType(),
                    CombatTools.KnockbackTypes.DEFAULT
            );
            var burnDuration = getBurnDuration(spellLevel);
            if (damaged && target.getRemainingFireTicks() < burnDuration) {
                target.setRemainingFireTicks(burnDuration);
            }
        }
    }

    private static void tryIgniteBlock(Level level, LivingEntity caster, CastTarget target) {
        if (target.blockPosition() == null) {
            return;
        }

        if (level.getBlockState(target.blockPosition()).getBlock() instanceof EssenceSmoker) {
            var blockEntity = level.getBlockEntity(target.blockPosition());
            if (canIgniteBlocks(level, caster) && blockEntity instanceof EssenceSmokerBlockEntity essenceSmoker) {
                essenceSmoker.ignite(level.getGameTime());
            }
            return;
        }

        if (!canIgniteBlocks(level, caster) || target.hitFace() == null) {
            return;
        }

        var firePosition = target.blockPosition().relative(target.hitFace());
        if (!level.getBlockState(firePosition).isAir() || isAdjacentToPortalFrame(level, firePosition)) {
            return;
        }

        var fireState = BaseFireBlock.getState(level, firePosition);
        if (!fireState.canSurvive(level, firePosition)) {
            return;
        }

        level.setBlock(firePosition, fireState, 11);
        level.gameEvent(caster, GameEvent.BLOCK_PLACE, firePosition);
    }

    private static boolean isAdjacentToPortalFrame(Level level, BlockPos position) {
        for (var direction : Direction.values()) {
            var adjacentPosition = position.relative(direction);
            if (CatchFlameLoaderHooks.isPortalFrame(level, adjacentPosition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canIgniteBlocks(Level level, LivingEntity caster) {
        var config = ApprenticeCodexServerConfig.catchFlameConfig();
        if (SpellDispenserCastContext.isActive()) {
            return config.allowSpellDispenserIgnition();
        }

        if (isRemoteOwnerCast(caster)) {
            var remoteOwner = resolveRemoteOwner(level, caster);
            return remoteOwner != null && canOwnerIgnite(remoteOwner, config.allowNonPlayerIgnition());
        }

        return canOwnerIgnite(caster, config.allowNonPlayerIgnition());
    }

    private static boolean canOwnerIgnite(Entity owner, boolean allowNonPlayerIgnition) {
        return CatchFlameLoaderHooks.isRealPlayer(owner) || allowNonPlayerIgnition;
    }

    private static Entity resolveCombatActor(Level level, LivingEntity caster) {
        var remoteOwner = resolveRemoteOwner(level, caster);
        return remoteOwner != null ? remoteOwner : caster;
    }

    private static boolean isRemoteOwnerCast(LivingEntity caster) {
        return caster instanceof RemoteOwnerCastAnchorEntity
                || caster instanceof ServerPlayer player && RemoteOwnerCastContext.get(player) != null;
    }

    private static @Nullable Entity resolveRemoteOwner(Level level, LivingEntity caster) {
        if (caster instanceof ServerPlayer player && RemoteOwnerCastContext.get(player) != null) {
            return player;
        }
        if (caster instanceof RemoteOwnerCastAnchorEntity anchor
                && level instanceof ServerLevel serverLevel
                && anchor.getCombatOwnerUuid() != null) {
            return serverLevel.getPlayerByUUID(anchor.getCombatOwnerUuid());
        }
        return null;
    }

    private static void spawnImpactParticles(Level level, Vec3 impactPosition) {
        MagicManager.spawnParticles(
                level,
                ParticleHelper.FIRE,
                impactPosition.x,
                impactPosition.y,
                impactPosition.z,
                FIRE_PARTICLE_COUNT,
                0.08D,
                0.08D,
                0.08D,
                0.12D,
                false
        );
    }

    private static void spawnImpactEffect(Level level, LivingEntity caster, Vec3 impactPosition) {
        var effect = new CatchFlameImpactEntity(EntityRegistry.CATCH_FLAME_IMPACT.get(), level);
        effect.setPos(impactPosition);
        effect.setup(impactPosition.subtract(caster.getEyePosition(1.0F)));
        level.addFreshEntity(effect);
    }

    private record CastTarget(Vec3 impactPosition, @Nullable BlockPos blockPosition, @Nullable Direction hitFace) {
        private Vec3 lineOfSightOrigin() {
            return hitFace == null
                    ? impactPosition
                    : impactPosition.add(Vec3.atLowerCornerOf(hitFace.getNormal()).scale(BLOCK_SURFACE_EPSILON));
        }
    }
}
