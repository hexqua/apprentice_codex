package jp.aquafactory.apprenticecodex.spell.thermalprocess;

import io.redspace.ironsspellbooks.util.ParticleHelper;
import jp.aquafactory.apprenticecodex.compat.create.CreateExposedItemProcessingBridge;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.ThermalProcessing;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ThermalProcessThrowerEntity extends SummonWeaponEntity {
    private static final int ATTACK_START_DELAY_TICKS = 10;
    private static final int ITEM_PROCESS_INTERVAL_TICKS = 20;
    private static final double ATTACK_RADIUS = 0.25;
    private static final double ITEM_PROCESS_RADIUS = 0.5;
    private static final double ATTACK_SAMPLE_STEP = 0.2;
    private static final double FIRE_OFFSET = 0.65;
    private static final EntityDataAccessor<Boolean> IS_ATTACKING =
            SynchedEntityData.defineId(ThermalProcessThrowerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RANGE_SYNC =
            SynchedEntityData.defineId(ThermalProcessThrowerEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private float range;
    private float burnItemPerSecond;
    private int spellLevel;
    private float pendingItemProcessBudget;
    private int startupTick;
    private final Set<UUID> skipProcessingItemIds = new HashSet<>();
    private final Set<Object> skipProcessingTransportedItems = Collections.newSetFromMap(new IdentityHashMap<>());

    public ThermalProcessThrowerEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public ThermalProcessThrowerEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(IS_ATTACKING, false);
        entityData.define(RANGE_SYNC, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        range = pCompound.getFloat("Range");
        burnItemPerSecond = pCompound.getFloat("BurnItemPerSecond");
        pendingItemProcessBudget = pCompound.getFloat("PendingItemProcessBudget");
        entityData.set(RANGE_SYNC, range);
        startupTick = pCompound.getInt("StartupTick");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Range", range);
        pCompound.putFloat("BurnItemPerSecond", burnItemPerSecond);
        pCompound.putFloat("PendingItemProcessBudget", pendingItemProcessBudget);
        pCompound.putInt("StartupTick", startupTick);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                0.7,
                10,
                0.04f,
                0.01,
                ParticleTypes.END_ROD,
                level
        );
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();
        if (level.isClientSide) {
            if (firstTick) {
                EffectTools.createRingParticle(
                        position(),
                        getLookAngle(),
                        0.2f,
                        8,
                        0.01f,
                        0.01,
                        ParticleTypes.END_ROD,
                        level
                );
            }

            if (entityData.get(IS_ATTACKING)) {
                spawnBeamParticlesClient(level);
            }
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var standbyPosition = getStandbyPosition();
        setDeltaMovement(Vec3.ZERO);
        setPos(standbyPosition.x, standbyPosition.y, standbyPosition.z);

        var aimResult = RaycastTools.raycastFromEye(owner, range, 0.5, this::canAimAt);
        var beamStart = getBeamStartPosition();
        var aimVector = aimResult.hitPosition().subtract(beamStart);
        if (aimVector.lengthSqr() > 1.0e-6) {
            var yawPitch = RotationTools.calculateYawPitchByDirection(aimVector);
            setYRot(yawPitch.yaw());
            setXRot(yawPitch.pitch());
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }

        if (startupTick < ATTACK_START_DELAY_TICKS) {
            ++startupTick;
            return;
        }

        if (!entityData.get(IS_ATTACKING)) {
            entityData.set(IS_ATTACKING, true);
        }

        fireBeam(level, owner, aimResult.hitPosition());
    }

    private boolean canAimAt(Entity target) {
        if (target == this) {
            return false;
        }

        // アイテムに照射する機能のため、アイテムへ視線を向けられるようにする.
        if (target instanceof ItemEntity) {
            return true;
        }

        return CombatTools.isValidCombatTarget(target, this);
    }

    private Vec3 getBeamStartPosition() {
        var look = getLookAngle();
        if (look.lengthSqr() < 1.0e-6) {
            look = new Vec3(0, 0, 1);
        }
        return position().add(look.normalize().scale(FIRE_OFFSET));
    }

    private BlockHitResult resolveBeamBlockHit(Level level, Vec3 beamStart, Vec3 direction) {
        var maxBeamEnd = beamStart.add(direction.scale(getEffectiveRange()));
        return level.clip(new ClipContext(beamStart, maxBeamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    private Vec3 resolveBeamEnd(Level level, Vec3 beamStart, Vec3 direction) {
        var maxBeamEnd = beamStart.add(direction.scale(getEffectiveRange()));
        var blockHit = resolveBeamBlockHit(level, beamStart, direction);
        return blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maxBeamEnd;
    }

    private void fireBeam(ServerLevel level, LivingEntity owner, Vec3 aimPosition) {
        var beamStart = getBeamStartPosition();
        var toAim = aimPosition.subtract(beamStart);
        var direction = toAim.lengthSqr() > 1.0e-6 ? toAim.normalize() : getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0e-6) {
            return;
        }

        var blockHit = resolveBeamBlockHit(level, beamStart, direction);
        var beamEnd = blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation()
                : beamStart.add(direction.scale(getEffectiveRange()));
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.THERMAL_PROCESS);
        var school = SpellRegistry.THERMAL_PROCESS.get().getSchoolType();
        var currentDamage = resolveCurrentDamage(owner);
        var hits = RaycastTools.sampleBeamHits(
                level,
                beamStart,
                beamEnd,
                ATTACK_RADIUS,
                ATTACK_SAMPLE_STEP,
                e -> e != this && e != owner && CombatTools.isValidCombatTarget(e, owner)
        );

        for (var target : hits) {
            var applied = CombatTools.applyDamage(target, currentDamage, source, school, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            if (!applied || !(target instanceof LivingEntity livingTarget)) {
                continue;
            }

            applyOrUpdateThermalProcessing(livingTarget);
        }

        processBeamItems(level, beamStart, beamEnd, blockHit);
    }

    private void processBeamItems(ServerLevel level, Vec3 beamStart, Vec3 beamEnd, BlockHitResult blockHit) {
        if (tickCount % ITEM_PROCESS_INTERVAL_TICKS != 0) {
            return;
        }

        pendingItemProcessBudget += Math.max(0.0f, burnItemPerSecond);
        var maxProcessCount = Mth.floor(pendingItemProcessBudget);
        if (maxProcessCount <= 0) {
            return;
        }

        var hits = sampleBeamItemHits(level, beamStart, beamEnd);
        if (hits.size() > 1) {
            hits.sort(Comparator.comparingDouble(item -> item.position().distanceToSqr(beamStart)));
        }

        var processedCount = 0;
        for (var itemEntity : hits) {
            if (processedCount >= maxProcessCount) {
                break;
            }

            if (!itemEntity.isAlive()) {
                continue;
            }

            if (skipProcessingItemIds.contains(itemEntity.getUUID())) {
                continue;
            }

            processedCount += tryProcessItem(level, itemEntity, maxProcessCount - processedCount);
        }

        if (processedCount < maxProcessCount && blockHit.getType() == HitResult.Type.BLOCK) {
            processedCount += CreateExposedItemProcessingBridge.processBlocks(
                    level,
                    List.of(blockHit.getBlockPos()),
                    maxProcessCount - processedCount,
                    skipProcessingTransportedItems,
                    (inputStack, remainingBudget) -> tryBuildProcessingResult(level, inputStack, remainingBudget),
                    processedPos -> playCreateItemProcessedSound(level, processedPos)
            );
        }

        pendingItemProcessBudget = Math.max(0.0f, pendingItemProcessBudget - processedCount);
    }

    private List<ItemEntity> sampleBeamItemHits(ServerLevel level, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        var length = delta.length();
        if (length < 1.0e-6) {
            return new ArrayList<>();
        }

        var direction = delta.scale(1.0 / length);
        var broad = new AABB(start, end).inflate(ITEM_PROCESS_RADIUS + 0.5);
        var candidates = level.getEntitiesOfClass(
                ItemEntity.class,
                broad,
                item -> item.isAlive() && !item.getItem().isEmpty()
        );
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        var hits = new ArrayList<ItemEntity>();
        var steps = Math.max(1, (int) Math.ceil(length / ATTACK_SAMPLE_STEP));
        for (var candidate : candidates) {
            var hitBox = candidate.getBoundingBox().inflate(ITEM_PROCESS_RADIUS);
            for (var i = 0; i <= steps; i++) {
                var t = i / (double) steps;
                var point = start.add(direction.scale(length * t));
                if (!hitBox.contains(point)) {
                    continue;
                }

                hits.add(candidate);
                break;
            }
        }

        return hits;
    }

    private int tryProcessItem(ServerLevel level, ItemEntity itemEntity, int maxProcessCount) {
        if (maxProcessCount <= 0) {
            return 0;
        }

        var inputStack = itemEntity.getItem();
        if (inputStack.isEmpty()) {
            return 0;
        }

        var processingResult = tryBuildProcessingResult(level, inputStack, maxProcessCount);
        processingResult.ifPresent(result -> applyProcessingResult(
                level,
                itemEntity,
                inputStack,
                result.outputStacks(),
                result.processedCount()
        ));
        return processingResult.map(jp.aquafactory.apprenticecodex.utility.ItemStackProcessingResult::processedCount).orElse(0);
    }

    private Optional<jp.aquafactory.apprenticecodex.utility.ItemStackProcessingResult> tryBuildProcessingResult(
            ServerLevel level,
            ItemStack inputStack,
            int maxProcessCount
    ) {
        if (maxProcessCount <= 0 || inputStack.isEmpty()) {
            return Optional.empty();
        }

        var recipe = findProcessingRecipe(level, inputStack);
        if (recipe.isEmpty()) {
            return Optional.empty();
        }

        var singleInput = new SimpleContainer(inputStack.copyWithCount(1));
        var outputPerInput = recipe.get().assemble(singleInput, level.registryAccess());
        if (outputPerInput.isEmpty()) {
            return Optional.empty();
        }

        var processCount = Math.min(maxProcessCount, inputStack.getCount());
        var outputCount = outputPerInput.getCount() * processCount;
        if (outputCount <= 0) {
            return Optional.empty();
        }

        return Optional.of(new jp.aquafactory.apprenticecodex.utility.ItemStackProcessingResult(
                processCount,
                splitOutputStacks(outputPerInput, outputCount)
        ));
    }

    private Optional<? extends AbstractCookingRecipe> findProcessingRecipe(ServerLevel level, ItemStack inputStack) {
        var input = new SimpleContainer(inputStack.copyWithCount(1));
        return ProcessingRecipeDenylist.findThermalProcessRecipe(level.getRecipeManager(), input, level);
    }

    private void applyProcessingResult(ServerLevel level, ItemEntity sourceItem, ItemStack sourceStack, List<ItemStack> outputStacks, int processCount) {
        outputStacks = new ArrayList<>(outputStacks);
        var remainingInputCount = sourceStack.getCount() - processCount;

        if (remainingInputCount <= 0) {
            if (outputStacks.isEmpty()) {
                sourceItem.discard();
                return;
            }

            var firstOutput = outputStacks.remove(0);
            sourceItem.setItem(firstOutput);
            skipProcessingItemIds.add(sourceItem.getUUID());
        } else {
            var remain = sourceStack.copy();
            remain.setCount(remainingInputCount);
            sourceItem.setItem(remain);
        }

        for (var outputStack : outputStacks) {
            spawnProcessedOutput(level, sourceItem, outputStack);
        }

        playItemProcessedEffects(level, sourceItem, processCount);
    }

    private List<ItemStack> splitOutputStacks(ItemStack outputPrototype, int totalCount) {
        var stacks = new ArrayList<ItemStack>();
        var maxStackSize = Math.max(1, outputPrototype.getMaxStackSize());
        var remaining = totalCount;
        while (remaining > 0) {
            var stackCount = Math.min(maxStackSize, remaining);
            var split = outputPrototype.copy();
            split.setCount(stackCount);
            stacks.add(split);
            remaining -= stackCount;
        }
        return stacks;
    }

    private void spawnProcessedOutput(ServerLevel level, ItemEntity sourceItem, ItemStack outputStack) {
        if (outputStack.isEmpty()) {
            return;
        }

        var spawned = new ItemEntity(level, sourceItem.getX(), sourceItem.getY(), sourceItem.getZ(), outputStack);
        spawned.setDeltaMovement(sourceItem.getDeltaMovement());
        level.addFreshEntity(spawned);
        skipProcessingItemIds.add(spawned.getUUID());
    }

    private void playItemProcessedEffects(ServerLevel level, ItemEntity sourceItem, int processCount) {
        var smokeCount = Mth.clamp(4 + processCount * 2, 6, 24);
        AudioTools.playSoundFromEntity(level, sourceItem, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL);
        level.sendParticles(
                ParticleTypes.SMOKE,
                sourceItem.getX(),
                sourceItem.getY() + 0.1,
                sourceItem.getZ(),
                smokeCount,
                0.1,
                0.05,
                0.1,
                0.01
        );
    }

    private void playCreateItemProcessedSound(ServerLevel level, BlockPos sourcePos) {
        AudioTools.playSoundFromPosition(level, sourcePos.getCenter(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL);
    }

    private void applyOrUpdateThermalProcessing(LivingEntity target) {
        var current = target.getEffect(EffectRegistry.THERMAL_PROCESSING.get());
        var nextAmplifier = current == null
                ? 0
                : Math.min(current.getAmplifier() + 1, ThermalProcessing.MAX_AMPLIFIER);

        target.addEffect(new MobEffectInstance(
                EffectRegistry.THERMAL_PROCESSING.get(),
                ThermalProcessing.BASE_DURATION_TICKS,
                nextAmplifier,
                false,
                true,
                true
        ));

        if (nextAmplifier >= ThermalProcessing.MAX_AMPLIFIER) {
            target.setSecondsOnFire(ThermalProcessing.IGNITE_TICKS / 20);
        }
    }

    private void spawnBeamParticlesClient(Level level) {
        var direction = getLookAngle().normalize();
        if (direction.lengthSqr() < 1.0e-6) {
            return;
        }

        if (tickCount % 2 != 0 ){
            return;
        }

        var beamStart = getBeamStartPosition();
        var beamEnd = resolveBeamEnd(level, beamStart, direction);
        var beamLength = beamStart.distanceTo(beamEnd);
        var particleCount = Math.max(3, (int) Math.ceil(beamLength * 3));
        var random = level.getRandom();

        for (var i = 0; i <= particleCount; i++) {
            var t = i / (double) particleCount;
            var pos = beamStart.add(beamEnd.subtract(beamStart).scale(t));
            var jitterScale = 0.03;
            var vx = direction.x * (0.05 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.01;
            var vy = direction.y * (0.05 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.01;
            var vz = direction.z * (0.05 + random.nextDouble() * 0.04) + (random.nextDouble() - 0.5) * 0.01;
            level.addParticle(
                    ParticleHelper.FIRE_EMITTER,
                    pos.x + (random.nextDouble() - 0.5) * jitterScale,
                    pos.y + (random.nextDouble() - 0.5) * jitterScale,
                    pos.z + (random.nextDouble() - 0.5) * jitterScale,
                    vx,
                    vy,
                    vz
            );
        }
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.5, 0.7, -0.25);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSpellLevel(int spellLevel) {
        this.spellLevel = spellLevel;
    }

    public void setBurnItemPerSecond(float burnItemPerSecond) {
        this.burnItemPerSecond = Math.max(0.0f, burnItemPerSecond);
        pendingItemProcessBudget = 0.0f;
    }

    public void setRange(float range) {
        this.range = range;
        entityData.set(RANGE_SYNC, range);
    }

    private float getEffectiveRange() {
        var synced = entityData.get(RANGE_SYNC);
        return synced > 0 ? synced : range;
    }

    private float resolveCurrentDamage(LivingEntity owner) {
        if (spellLevel > 0) {
            return ThermalProcess.getDamage(SpellRegistry.THERMAL_PROCESS.get().getSpellPower(spellLevel, owner));
        }

        return damage;
    }
}
