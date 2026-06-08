package jp.aquafactory.apprenticecodex.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.HeavenlyFistPulsePacket;
import jp.aquafactory.apprenticecodex.particle.ImpactTremorBlockParticleOptions;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class HeavenlyFistFistEntity extends Entity implements GeoEntity, TraceableEntity {
    static final int ATTACK_START_TICK = 4;
    static final int IMPACT_TICK = 25;
    static final int DISCARD_TICK = 30;
    private static final int GRAVITY_BOUND_DURATION_TICKS = 20 * 10;
    private static final int TREMOR_BLOCK_LIMIT = 18;
    private static final double TREMOR_RADIUS = 2.35D;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerUuid;
    private @Nullable Entity cachedOwner;
    private Vec3 lockedCenter = Vec3.ZERO;
    private float damage;
    private float radius;
    private int maxProcessCount;
    private boolean impacted;

    public HeavenlyFistFistEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public HeavenlyFistFistEntity(EntityType<?> entityType, Level level, LivingEntity owner, Vec3 lockedCenter,
                                  float damage, float radius, int maxProcessCount) {
        this(entityType, level);
        setOwner(owner);
        setLockedCenter(lockedCenter);
        this.damage = damage;
        this.radius = Math.max(0.0F, radius);
        this.maxProcessCount = Math.max(0, maxProcessCount);
    }

    @Override
    protected void defineSynchedData() {
        // no synced data.
    }

    @Override
    public void onClientRemoval(){
        var level = level();
        EffectTools.createStickParticle(
                position(),
                new Vec3(0,-1,0),
                2.5,
                24,
                0.5f,
                0.02,
                ParticleTypes.END_ROD, level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && lockedCenter == Vec3.ZERO) {
            lockedCenter = position();
        }
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        setPos(lockedCenter.x, lockedCenter.y, lockedCenter.z);

        if (level().isClientSide) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            tickServer(serverLevel);
        }
    }

    private void tickServer(ServerLevel level) {
        if (tickCount >= IMPACT_TICK && !impacted) {
            impacted = true;
            impact(level);
        }

        if (tickCount >= DISCARD_TICK) {
            discard();
        }
    }

    private void impact(ServerLevel level) {
        var owner = getOwner();
        var area = new AABB(lockedCenter, lockedCenter).inflate(radius);
        var source = owner != null
                ? CombatTools.getDamageSource(level, this, owner, DamageTypes.HEAVENLY_FIST)
                : CombatTools.getDamageSource(level, this, DamageTypes.HEAVENLY_FIST);

        var targets = CombatTools.resolveUniqueCombatTargets(
                level.getEntities(this, area, entity -> CombatTools.isValidCombatTarget(entity, owner))
        );
        for (var target : targets) {
            var damaged = CombatTools.applyDamage(
                    target,
                    damage,
                    source,
                    SpellRegistry.HEAVENLY_FIST.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK
            );
            if (damaged && target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(
                        EffectRegistry.GRAVITY_BOUND.get(),
                        GRAVITY_BOUND_DURATION_TICKS,
                        0,
                        false,
                        true,
                        true
                ));
            }
        }

        HeavenlyFistPressingProcessor.processItems(level, lockedCenter, radius, maxProcessCount);
        if (owner instanceof LivingEntity livingOwner) {
            HeavenlyFistCrystalHarvestProcessor.harvest(level, livingOwner, lockedCenter, radius);
        }
        playImpactEffects(level);
    }

    private void playImpactEffects(ServerLevel level) {
        AudioTools.playSoundFromPosition(level, lockedCenter, SoundRegistry.AMETHYST_FIST.get(), SoundSource.PLAYERS, 0.9F, 1.0F, 0.08F);
        Networks.sendToTrackingEntityAndSelf(this, new HeavenlyFistPulsePacket(lockedCenter.add(0.0D, 0.05D, 0.0D), radius + 0.25F));
        spawnTremorBlocks(level);
    }

    private void spawnTremorBlocks(ServerLevel level) {
        if (findVisibleGroundBlock(level, lockedCenter.x, lockedCenter.y - 0.05D, lockedCenter.z, 3) == null) {
            return;
        }

        var blocks = new java.util.LinkedHashSet<BlockPos>();
        for (var i = 0; i < TREMOR_BLOCK_LIMIT * 2 && blocks.size() < TREMOR_BLOCK_LIMIT; i++) {
            var angle = level.random.nextDouble() * Math.PI * 2.0D;
            var distance = TREMOR_RADIUS * Math.sqrt(level.random.nextDouble());
            var x = lockedCenter.x + Math.cos(angle) * distance;
            var z = lockedCenter.z + Math.sin(angle) * distance;
            var blockPos = findVisibleGroundBlock(level, x, lockedCenter.y + 1.0D, z, 4);
            if (blockPos != null) {
                blocks.add(blockPos);
            }
        }

        for (var blockPos : blocks) {
            spawnTremorBlock(level, blockPos, 0.24F + level.random.nextFloat() * 0.1F);
        }
    }

    private static void spawnTremorBlock(ServerLevel level, BlockPos blockPos, float impulseStrength) {
        var above = blockPos.above();
        if (!level.getBlockState(above).isAir() && !level.getBlockState(above.above()).isAir()) {
            return;
        }

        sendTremorParticle(level, blockPos, blockPos, impulseStrength);
        if (!level.getBlockState(above).isAir()) {
            sendTremorParticle(level, above, above, impulseStrength);
        }
    }

    private static void sendTremorParticle(ServerLevel level, BlockPos statePos, BlockPos particlePos, float impulseStrength) {
        var state = level.getBlockState(statePos);
        if (state.isAir()) {
            return;
        }

        level.sendParticles(
                new ImpactTremorBlockParticleOptions(
                        state,
                        new Vec3(0.0D, impulseStrength, 0.0D),
                        ImpactTremorBlockParticleOptions.Source.HEAVENLY_FIST
                ),
                particlePos.getX() + 0.5D,
                particlePos.getY(),
                particlePos.getZ() + 0.5D,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static @Nullable BlockPos findVisibleGroundBlock(ServerLevel level, double x, double startY, double z, int searchDepth) {
        var start = BlockPos.containing(x, startY, z);
        for (var offset = 0; offset <= searchDepth; offset++) {
            var pos = start.below(offset);
            var state = level.getBlockState(pos);
            if (!state.isAir()
                    && state.getRenderShape() == RenderShape.MODEL
                    && !state.getCollisionShape(level, pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    @Override
    public @Nullable Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }

        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerUuid);
            return cachedOwner;
        }

        return null;
    }

    public void setOwner(Entity owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
    }

    public void setLockedCenter(Vec3 lockedCenter) {
        this.lockedCenter = lockedCenter;
        setPos(lockedCenter.x, lockedCenter.y, lockedCenter.z);
    }

    public float getCoreRedProgress(float partialTick) {
        var age = tickCount + partialTick;
        return net.minecraft.util.Mth.clamp((age - 15.0F) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        ownerUuid = null;
        cachedOwner = null;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        // 短寿命の演出/判定エンティティなので保存しない。
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public void push(@NotNull Entity entity) {
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        var maxDistance = 96.0D;
        return distanceSqr < maxDistance * maxDistance;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this,
                "main",
                state -> {
                    state.setAnimation(tickCount >= ATTACK_START_TICK ? ANIM_ATTACK : ANIM_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
