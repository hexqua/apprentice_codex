package jp.aquafactory.apprenticecodex.spell.higanbana;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.renderer.GeoBonePoseCache;
import jp.aquafactory.apprenticecodex.renderer.ISwordTrailEntity;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class HiganbanaKatanaEntity extends SummonWeaponEntity implements GeoEntity, ISwordTrailEntity {
    private static final int SLASH_EFFECT_TICK = 10;
    private static final int SLASH_STANDBY_TICK = 5;
    private static final DustParticleOptions DRAIN_DUST_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
    private static final int DRAIN_DUST_COUNT = 20;
    private static final double DRAIN_DUST_SPEED = 0.02D;

    private static final EntityDataAccessor<Boolean> SHOW_TRAIL =
            SynchedEntityData.defineId(HiganbanaKatanaEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    private static final RawAnimation ANIM_SLASH_0TO1 = RawAnimation.begin().thenPlayAndHold("slash_0to1");
    private static final RawAnimation ANIM_SLASH_1TO2 = RawAnimation.begin().thenPlayAndHold("slash_1to2");
    private static final RawAnimation ANIM_SLASH_2TO3 = RawAnimation.begin().thenPlayAndHold("slash_2to3");
    private static final RawAnimation ANIM_SLASH_3TO4 = RawAnimation.begin().thenPlayAndHold("slash_3to4");
    private static final RawAnimation ANIM_SLASH_4TO1 = RawAnimation.begin().thenPlayAndHold("slash_4to1");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int slashEffectTick;
    private int slashStandbyTick;
    private int remainingSlashCount;
    private int slashPhaseIndex;
    private int releaseDelayTick = -1;

    public HiganbanaKatanaEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public HiganbanaKatanaEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
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
        var pose = GeoBonePoseCache.getPrev(getUUID());
        if (pose == null) {
            return;
        }

        var yawDeg = RotationTools.calculateYawPitchByEntity(this, 1.0f).yaw();
        var yawRad = -yawDeg * Mth.DEG_TO_RAD;
        var rootLocal = pose.root().subtract(position());
        var tipLocal = pose.tip().subtract(position());
        var rootWorld = rootLocal.yRot(yawRad).add(position());
        var tipWorld = tipLocal.yRot(yawRad).add(position());
        EffectTools.createLineParticle(rootWorld, tipWorld, 0.25, 0.1, 0.01, ParticleTypes.END_ROD, level());
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (releaseDelayTick == 0) {
            discard();
            return;
        }
        if (releaseDelayTick > 0) {
            --releaseDelayTick;
        }

        if (slashEffectTick > 0) {
            --slashEffectTick;
            if (slashEffectTick <= 0) {
                entityData.set(SHOW_TRAIL, false);
            }
        }

        if (slashStandbyTick > 0) {
            --slashStandbyTick;
        }

        if (canSlash()) {
            slash(level);
        }
    }

    public boolean canSlash() {
        return remainingSlashCount > 0 && slashStandbyTick <= 0 && releaseDelayTick < 0;
    }

    public void slash(Level level) {
        if (!canSlash()) {
            return;
        }

        performSlash(level);
        --remainingSlashCount;
        slashStandbyTick = SLASH_STANDBY_TICK;
        if (remainingSlashCount <= 0) {
            // 最終斬撃直後に消すと演出が途切れるため、既存と同じだけ待ってから消す。
            scheduleRelease(SLASH_EFFECT_TICK);
        }
    }

    private void performSlash(Level level) {
        triggerSlashAnimation();
        entityData.set(SHOW_TRAIL, true);
        slashEffectTick = SLASH_EFFECT_TICK;

        if (getOwner() instanceof LivingEntity owner) {
            var point = getLookAngle().normalize().scale(0.75);
            var source = createCombatDamageSource(DamageTypes.HIGANBANA);
            var hitResult = RaycastTools.hitsAabb(
                    level,
                    position().add(point),
                    2.5,
                    e -> e != owner && CombatTools.isValidCombatTarget(e, owner)
            );
            AudioTools.playSoundFromEntity(level, this, SoundRegistry.KATANA_SLASH.get(), SoundSource.PLAYERS);
            AudioTools.playSoundFromEntity(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS);
            for (var hit : hitResult) {
                var applied = CombatTools.applyDamage(hit, damage, source, SpellRegistry.HIGANBANA.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
                if (applied) {
                    playDrainFeedback(level, owner);
                }
            }
        }
    }

    public void setFirstSlashStandby(int ticks) {
        if (ticks <= 0) {
            slash(level());
            return;
        }

        slashStandbyTick = ticks;
    }

    public void setRemainingSlashCount(int remainingSlashCount) {
        this.remainingSlashCount = Math.max(0, remainingSlashCount);
    }

    public int getRemainingSlashCount() {
        return remainingSlashCount;
    }

    private static void playDrainFeedback(Level level, LivingEntity owner) {
        AudioTools.playSoundFromEntity(level, owner, SoundRegistry.SLASH_DRAIN.get(), SoundSource.PLAYERS);

        if (!(level instanceof ServerLevel serverLevel) || !(owner instanceof ServerPlayer player)) {
            return;
        }

        serverLevel.sendParticles(
                DRAIN_DUST_PARTICLE,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                DRAIN_DUST_COUNT,
                Math.max(0.25D, player.getBbWidth() * 0.6D),
                Math.max(0.35D, player.getBbHeight() * 0.5D),
                Math.max(0.25D, player.getBbWidth() * 0.6D),
                DRAIN_DUST_SPEED
        );
    }

    private void triggerSlashAnimation() {
        switch (slashPhaseIndex) {
            case 0 -> triggerAnim("main", "slash_0to1");
            case 1 -> triggerAnim("main", "slash_1to2");
            case 2 -> triggerAnim("main", "slash_2to3");
            case 3 -> triggerAnim("main", "slash_3to4");
            default -> triggerAnim("main", "slash_4to1");
        }

        slashPhaseIndex = switch (slashPhaseIndex) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 1;
        };
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamageForGameTest() {
        return damage;
    }

    public void scheduleRelease(int delayTick) {
        if (delayTick <= 0) {
            discard();
            return;
        }

        if (releaseDelayTick < delayTick) {
            releaseDelayTick = delayTick;
        }
    }

    @Override
    public boolean isTrailActive() {
        return entityData.get(SHOW_TRAIL);
    }

    @Override
    public int getTrailColorARGB() {
        return 0xFFFF0000;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        slashStandbyTick = pCompound.getInt("SlashStandbyTick");
        remainingSlashCount = pCompound.getInt("RemainingSlashCount");
        slashPhaseIndex = pCompound.getInt("SlashPhase");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putInt("SlashStandbyTick", slashStandbyTick);
        pCompound.putInt("RemainingSlashCount", remainingSlashCount);
        pCompound.putInt("SlashPhase", slashPhaseIndex);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            // 彼岸花は水平方向の攻撃しかできないため、backOffset調整だけでよい.
            return RotationTools.calculateBehindPosition(owner, -0.9, 0, -0.75);
        }

        return Vec3.ZERO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, "main", state -> {
                    state.getController().setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                })
                        .triggerableAnim("slash_0to1", ANIM_SLASH_0TO1)
                        .triggerableAnim("slash_1to2", ANIM_SLASH_1TO2)
                        .triggerableAnim("slash_2to3", ANIM_SLASH_2TO3)
                        .triggerableAnim("slash_3to4", ANIM_SLASH_3TO4)
                        .triggerableAnim("slash_4to1", ANIM_SLASH_4TO1)
                        .setAnimationSpeedHandler(e -> 7.5) // 変動させないので決め打ちでよい.
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

