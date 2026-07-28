package jp.aquafactory.apprenticecodex.spell.precisionjack;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class PrecisionJackKnifeEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {
    private static final int STAY_SLASHED_TICK = 10;
    private static final int UNPREPARED_TIMEOUT_TICKS = 5;
    private static final int PREPARED_TIMEOUT_TICKS = 80;
    private static final double SLICE_RANGE = 1.25;
    public static final String TRAIL_CACHE_KEY = "blade";

    private static final DustParticleOptions REMOVAL_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.9f, 0.15f), 1.0f);
    private static final EntityDataAccessor<Boolean> SHOW_TRAIL =
            SynchedEntityData.defineId(PrecisionJackKnifeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    private static final RawAnimation ANIM_PREPARE = RawAnimation.begin().thenLoop("prepare");
    private static final RawAnimation ANIM_SLICE = RawAnimation.begin().thenPlayAndHold("slice");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int lootingBonus = 1;
    private int duplicateDropChancePercent;
    private int lifeTick;
    private int unresolvedCastTick;
    private boolean isPrepared;

    public PrecisionJackKnifeEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public PrecisionJackKnifeEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOW_TRAIL, false);
    }

    @Override
    public void onClientRemoval() {
        spawnRemovalLineParticle();
        GeoBonePoseCache.remove(getUUID());
        super.onClientRemoval();
    }

    private void spawnRemovalLineParticle() {
        var pose = GeoBonePoseCache.getPrev(getUUID(), TRAIL_CACHE_KEY);
        if (pose == null) {
            return;
        }

        var yawDeg = RotationTools.calculateYawPitchByEntity(this, 1.0f).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var rootLocal = pose.root().subtract(position());
        var tipLocal = pose.tip().subtract(position());
        var rootWorld = rootLocal.yRot(yawRad).add(position());
        var tipWorld = tipLocal.yRot(yawRad).add(position());
        EffectTools.createLineParticle(rootWorld, tipWorld, 0.2, 0.08, 0.01, REMOVAL_PARTICLE, level());
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (lifeTick > 0) {
            --lifeTick;
            if (lifeTick <= 0) {
                discard();
                return;
            }
        } else if (tickUnresolvedCast()) {
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());
        setYRot(owner.getYRot());
        setXRot(0);
        setRot(getYRot(), getXRot());
    }

    public void prepare() {
        if (isPrepared) {
            return;
        }

        entityData.set(SHOW_TRAIL, true);
        triggerAnim("main", "prepare");
        isPrepared = true;
        unresolvedCastTick = 0;
    }

    public void slice(Level level) {
        if (!isPrepared) {
            return;
        }

        triggerAnim("main", "slice");
        lifeTick = STAY_SLASHED_TICK;
        unresolvedCastTick = 0;
        isPrepared = false;

        if (!(getOwner() instanceof LivingEntity owner)) {
            return;
        }

        var point = getLookAngle().normalize().scale(0.75);
        var source = createCombatDamageSource(DamageTypes.PRECISION_JACK);

        var hitResult = RaycastTools.hitsAabb(
                level,
                position().add(point),
                SLICE_RANGE,
                e -> e != owner && CombatTools.isValidCombatTarget(e, owner)
        );

        AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);

        for (var hit : hitResult) {
            CombatTools.applyDamage(hit, damage, source, SpellRegistry.PRECISION_JACK.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setLootingBonus(int lootingBonus) {
        this.lootingBonus = Math.max(0, lootingBonus);
    }

    public int getLootingBonus() {
        return lootingBonus;
    }

    public void setDuplicateDropChancePercent(int duplicateDropChancePercent) {
        this.duplicateDropChancePercent = Mth.clamp(duplicateDropChancePercent, 0, 100);
    }

    public int getDuplicateDropChancePercent() {
        return duplicateDropChancePercent;
    }

    private boolean tickUnresolvedCast() {
        ++unresolvedCastTick;
        // Spell Dispenser のマナ不足などで完了通知へ到達しない場合でも、召喚ナイフだけを残さない。
        var timeoutTicks = isPrepared ? PREPARED_TIMEOUT_TICKS : UNPREPARED_TIMEOUT_TICKS;
        return unresolvedCastTick >= timeoutTicks;
    }

    @Override
    public boolean isTrailActive() {
        return entityData.get(SHOW_TRAIL);
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFFFFE45A;
    }

    @Override
    public List<TrailBonePair> getTrailBonePairs() {
        return List.of(new TrailBonePair(TRAIL_CACHE_KEY, "blade_top", "blade_root"));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        lootingBonus = pCompound.getInt("LootingBonus");
        duplicateDropChancePercent = pCompound.getInt("DuplicateDropChancePercent");
        lifeTick = pCompound.getInt("LifeTick");
        unresolvedCastTick = pCompound.getInt("UnresolvedCastTick");
        isPrepared = pCompound.getBoolean("IsPrepared");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putInt("LootingBonus", lootingBonus);
        pCompound.putInt("DuplicateDropChancePercent", duplicateDropChancePercent);
        pCompound.putInt("LifeTick", lifeTick);
        pCompound.putInt("UnresolvedCastTick", unresolvedCastTick);
        pCompound.putBoolean("IsPrepared", isPrepared);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 0, 0, -0.75);
        }

        return Vec3.ZERO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "main", state -> {
            state.getController().setAnimation(ANIM_IDLE);
            return PlayState.CONTINUE;
        })
                .triggerableAnim("prepare", ANIM_PREPARE)
                .triggerableAnim("slice", ANIM_SLICE)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

