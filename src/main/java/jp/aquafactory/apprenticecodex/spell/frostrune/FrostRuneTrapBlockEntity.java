package jp.aquafactory.apprenticecodex.spell.frostrune;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.BlockEntityRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.UUID;

public class FrostRuneTrapBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String DAMAGE_TAG = "Damage";
    private static final String SURVIVE_TICKS_TAG = "SurviveTicks";
    private static final String AGE_TICKS_TAG = "AgeTicks";
    private static final String DETONATING_TAG = "Detonating";
    private static final String DETONATE_AGE_TAG = "DetonateAge";
    private static final String EXPIRED_TAG = "Expired";
    private static final String VISUAL_NORTH_TAG = "VisualNorth";
    public static final int ARM_DELAY_TICKS = 60;
    private static final int DETONATE_REMOVE_DELAY_TICKS = 20;
    private static final int SLOW_DURATION_TICKS = 40;
    private static final int FROST_TRAPPED_DURATION_TICKS = 100;
    private static final int SLOW_AMPLIFIER = 4;
    private static final float DETECTION_TANGENT_HALF_EXTENT = 1.5F;
    private static final float DETECTION_NORMAL_EXTENT = 2.0F;
    private static final float BLAST_HALF_EXTENT = 2.5F;
    private static final RawAnimation PLACE_IDLE = RawAnimation.begin().thenPlay("place").thenLoop("idle");
    private static final RawAnimation DETONATE = RawAnimation.begin().thenPlayAndHold("detonate");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID ownerUuid;
    private Direction visualNorth = Direction.NORTH;
    private float damage;
    private int surviveTicks;
    private int ageTicks;
    private boolean detonating;
    private int detonateAge = -1;
    private boolean detonated;
    private boolean expired;
    private long clientAgeBaseGameTime = Long.MIN_VALUE;
    private long clientDetonateBaseGameTime = Long.MIN_VALUE;

    public FrostRuneTrapBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FROST_RUNE_TRAP.get(), pos, state);
    }

    public void initialize(LivingEntity owner, Direction visualNorth, float damage, int surviveTicks) {
        this.ownerUuid = owner.getUUID();
        this.visualNorth = visualNorth;
        this.damage = damage;
        this.surviveTicks = surviveTicks;
        this.ageTicks = 0;
        this.detonating = false;
        this.detonateAge = -1;
        this.detonated = false;
        this.expired = false;
        setChanged();
        syncToClient();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Direction getVisualNorth() {
        return visualNorth;
    }

    public int getAgeTicks() {
        return ageTicks;
    }

    public float getRenderAge(float partialTick) {
        if (level == null || !level.isClientSide) {
            return ageTicks + partialTick;
        }
        if (clientAgeBaseGameTime == Long.MIN_VALUE) {
            clientAgeBaseGameTime = level.getGameTime() - ageTicks;
        }
        return level.getGameTime() + partialTick - clientAgeBaseGameTime;
    }

    public boolean isDetonating() {
        return detonating;
    }

    public int getDetonateAge() {
        return detonateAge;
    }

    public float getRenderDetonateAge(float partialTick) {
        if (level == null || !level.isClientSide) {
            return Math.max(0, detonateAge) + partialTick;
        }
        if (clientDetonateBaseGameTime == Long.MIN_VALUE) {
            clientDetonateBaseGameTime = level.getGameTime() - Math.max(0, detonateAge);
        }
        return level.getGameTime() + partialTick - clientDetonateBaseGameTime;
    }

    public boolean shouldExplodeOnRemoval() {
        return !detonated && !expired;
    }

    public void detonateForRemoval() {
        detonate(false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FrostRuneTrapBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel)) {
            return;
        }

        if (blockEntity.detonating) {
            blockEntity.detonateAge++;
            if (blockEntity.detonateAge >= DETONATE_REMOVE_DELAY_TICKS) {
                blockEntity.expired = true;
                level.removeBlock(pos, false);
            } else {
                blockEntity.setChanged();
            }
            return;
        }

        blockEntity.ageTicks++;
        if (blockEntity.surviveTicks > 0 && blockEntity.ageTicks >= blockEntity.surviveTicks) {
            blockEntity.expired = true;
            level.removeBlock(pos, false);
            return;
        }

        if (blockEntity.ageTicks < ARM_DELAY_TICKS) {
            blockEntity.setChanged();
            return;
        }

        if (blockEntity.hasTriggerCandidate()) {
            blockEntity.detonate(true);
        } else {
            blockEntity.setChanged();
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.putFloat(DAMAGE_TAG, damage);
        tag.putInt(SURVIVE_TICKS_TAG, surviveTicks);
        tag.putInt(AGE_TICKS_TAG, ageTicks);
        tag.putBoolean(DETONATING_TAG, detonating);
        tag.putInt(DETONATE_AGE_TAG, detonateAge);
        tag.putBoolean(EXPIRED_TAG, expired);
        tag.putInt(VISUAL_NORTH_TAG, visualNorth.get3DDataValue());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        ownerUuid = tag.hasUUID(OWNER_UUID_TAG) ? tag.getUUID(OWNER_UUID_TAG) : null;
        damage = tag.getFloat(DAMAGE_TAG);
        surviveTicks = tag.getInt(SURVIVE_TICKS_TAG);
        ageTicks = tag.getInt(AGE_TICKS_TAG);
        detonating = tag.getBoolean(DETONATING_TAG);
        detonateAge = tag.contains(DETONATE_AGE_TAG) ? tag.getInt(DETONATE_AGE_TAG) : -1;
        detonated = detonating;
        expired = tag.getBoolean(EXPIRED_TAG);
        visualNorth = tag.contains(VISUAL_NORTH_TAG)
                ? Direction.from3DDataValue(tag.getInt(VISUAL_NORTH_TAG))
                : Direction.NORTH;
        clientAgeBaseGameTime = Long.MIN_VALUE;
        clientDetonateBaseGameTime = Long.MIN_VALUE;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", state -> {
            if (state.getAnimatable().isDetonating()) {
                return state.setAndContinue(DETONATE);
            }
            return state.setAndContinue(PLACE_IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private boolean hasTriggerCandidate() {
        if (level == null) {
            return false;
        }

        var owner = findOwnerEntity();
        var detectionBox = createDetectionBox();
        return !level.getEntities((Entity) null, detectionBox, entity -> isTriggerCandidate(entity, owner)).isEmpty();
    }

    private boolean isTriggerCandidate(Entity entity, @Nullable Entity owner) {
        if (!entity.isAlive() || entity instanceof ExperienceOrb) {
            return false;
        }
        if (ownerUuid != null && ownerUuid.equals(entity.getUUID())) {
            return false;
        }
        return CombatTools.isValidCombatTarget(entity, owner) || entity instanceof ItemEntity || entity instanceof Projectile;
    }

    private void detonate(boolean keepBlockForAnimation) {
        if (level == null || detonated) {
            return;
        }

        detonated = true;
        applyBlast();

        if (keepBlockForAnimation) {
            detonating = true;
            detonateAge = 0;
            setChanged();
            syncToClient();
        }
    }

    private void applyBlast() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var center = worldPosition.getCenter();
        var owner = findOwnerEntity();
        var damageSource = createDamageSource(serverLevel, owner);
        var damagedIds = new HashSet<Integer>();
        var area = new AABB(
                center.x - BLAST_HALF_EXTENT, center.y - BLAST_HALF_EXTENT, center.z - BLAST_HALF_EXTENT,
                center.x + BLAST_HALF_EXTENT, center.y + BLAST_HALF_EXTENT, center.z + BLAST_HALF_EXTENT
        );

        for (var rawTarget : serverLevel.getEntities((Entity) null, area, entity -> isBlastCandidate(entity, owner))) {
            var target = CombatTools.resolutePartEntity(rawTarget);
            if (!damagedIds.add(target.getId())) {
                continue;
            }
            var damaged = CombatTools.applyDamage(
                    target,
                    damage,
                    damageSource,
                    SpellRegistry.FROST_RUNE.get().getSchoolType(),
                    CombatTools.KnockbackTypes.NO_KNOCKBACK
            );
            if (damaged && target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, SLOW_AMPLIFIER, false, true, true));
                livingTarget.addEffect(new MobEffectInstance(EffectRegistry.FROST_TRAPPED.get(), FROST_TRAPPED_DURATION_TICKS, 0, false, false, true));
            }
        }

        spawnBlastParticles(serverLevel, center);
        AudioTools.playSoundFromPosition(serverLevel, center, SoundRegistry.FROZEN_RUNE.get(), SoundSource.PLAYERS, 1.0F, 1.0F, 0.04F);
    }

    private boolean isBlastCandidate(Entity entity, @Nullable Entity owner) {
        if (!entity.isAlive()) {
            return false;
        }
        var resolved = CombatTools.resolutePartEntity(entity);
        return CombatTools.isValidCombatTarget(resolved, owner);
    }

    private DamageSource createDamageSource(ServerLevel serverLevel, @Nullable Entity owner) {
        if (owner != null) {
            return CombatTools.getDamageSource(serverLevel, owner, owner, DamageTypes.FROST_RUNE);
        }

        var registry = serverLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var holder = registry.getHolder(DamageTypes.FROST_RUNE)
                .orElseGet(() -> (Holder.Reference<DamageType>) serverLevel.damageSources().genericKill().typeHolder());
        return new DamageSource(holder);
    }

    private void spawnBlastParticles(ServerLevel serverLevel, Vec3 center) {
        var random = serverLevel.random;
        var snowDust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SNOW.defaultBlockState());
        var spark = new AdditiveGlowParticleOptions(
                ParticleRegistry.ADDITIVE_SPARK.get(),
                0.22F,
                0.25F,
                1.0F,
                1.0F,
                3,
                18,
                8,
                0.65F,
                1.35F,
                0.7F,
                1.0F,
                0.1F,
                0.65F,
                0.15F,
                true
        );

        for (int i = 0; i < 96; i++) {
            var x = center.x + Mth.nextDouble(random, -BLAST_HALF_EXTENT, BLAST_HALF_EXTENT);
            var y = center.y + Mth.nextDouble(random, -BLAST_HALF_EXTENT, BLAST_HALF_EXTENT);
            var z = center.z + Mth.nextDouble(random, -BLAST_HALF_EXTENT, BLAST_HALF_EXTENT);
            serverLevel.sendParticles(snowDust, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
        serverLevel.sendParticles(spark, center.x, center.y, center.z, 64, BLAST_HALF_EXTENT, BLAST_HALF_EXTENT, BLAST_HALF_EXTENT, 0.03);
    }

    private AABB createDetectionBox() {
        var supportFacing = getBlockState().getValue(FrostRuneTrapBlock.FACING);
        var normal = supportFacing.getOpposite();
        var center = worldPosition.getCenter();
        var minX = center.x - DETECTION_TANGENT_HALF_EXTENT;
        var maxX = center.x + DETECTION_TANGENT_HALF_EXTENT;
        var minY = center.y - DETECTION_TANGENT_HALF_EXTENT;
        var maxY = center.y + DETECTION_TANGENT_HALF_EXTENT;
        var minZ = center.z - DETECTION_TANGENT_HALF_EXTENT;
        var maxZ = center.z + DETECTION_TANGENT_HALF_EXTENT;

        switch (normal) {
            case UP -> {
                minY = worldPosition.getY();
                maxY = worldPosition.getY() + DETECTION_NORMAL_EXTENT;
            }
            case DOWN -> {
                minY = worldPosition.getY() + 1.0D - DETECTION_NORMAL_EXTENT;
                maxY = worldPosition.getY() + 1.0D;
            }
            case NORTH -> {
                minZ = worldPosition.getZ() + 1.0D - DETECTION_NORMAL_EXTENT;
                maxZ = worldPosition.getZ() + 1.0D;
            }
            case SOUTH -> {
                minZ = worldPosition.getZ();
                maxZ = worldPosition.getZ() + DETECTION_NORMAL_EXTENT;
            }
            case WEST -> {
                minX = worldPosition.getX() + 1.0D - DETECTION_NORMAL_EXTENT;
                maxX = worldPosition.getX() + 1.0D;
            }
            case EAST -> {
                minX = worldPosition.getX();
                maxX = worldPosition.getX() + DETECTION_NORMAL_EXTENT;
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Nullable
    private Entity findOwnerEntity() {
        if (!(level instanceof ServerLevel serverLevel) || ownerUuid == null) {
            return null;
        }
        return serverLevel.getEntity(ownerUuid);
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
