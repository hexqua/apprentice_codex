package jp.aquafactory.apprenticecodex.spell.precisionjack;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PrecisionJackKnifeEntity extends SummonWeaponEntity implements GeoEntity {
    private static final int STAY_SLASHED_TICK = 10;
    private static final double SLICE_RANGE = 1.25;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    private static final RawAnimation ANIM_PREPARE = RawAnimation.begin().thenLoop("prepare");
    private static final RawAnimation ANIM_SLICE = RawAnimation.begin().thenPlayAndHold("slice");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int lootingBonus = 1;
    private int lifeTick;
    private boolean isPrepared;

    public PrecisionJackKnifeEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public PrecisionJackKnifeEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
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

        triggerAnim("main", "prepare");
        isPrepared = true;
    }

    public void slice(Level level) {
        if (!isPrepared) {
            return;
        }

        triggerAnim("main", "slice");
        lifeTick = STAY_SLASHED_TICK;
        isPrepared = false;

        if (!(getOwner() instanceof LivingEntity owner)) {
            return;
        }

        var point = getLookAngle().normalize().scale(0.75);
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.PRECISION_JACK);

        var hitResult = RaycastTools.hitsSphere(
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

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        lootingBonus = pCompound.getInt("LootingBonus");
        lifeTick = pCompound.getInt("LifeTick");
        isPrepared = pCompound.getBoolean("IsPrepared");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putInt("LootingBonus", lootingBonus);
        pCompound.putInt("LifeTick", lifeTick);
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

