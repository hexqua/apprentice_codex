package jp.aquafactory.apprenticecodex.entity;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellPayload;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class ChargedTwinBladeStaffThrownEntity extends Projectile {
    private static final int THROWN_RENDER_CUSTOM_MODEL_DATA = 1;
    private static final EntityDataAccessor<Boolean> DATA_IMPACTED =
            SynchedEntityData.defineId(ChargedTwinBladeStaffThrownEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_IMPACT_YAW =
            SynchedEntityData.defineId(ChargedTwinBladeStaffThrownEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IMPACT_PITCH =
            SynchedEntityData.defineId(ChargedTwinBladeStaffThrownEntity.class, EntityDataSerializers.FLOAT);
    private static final String STACK_TAG = "WeaponStack";
    private static final String SPELL_PAYLOAD_TAG = "SpellPayload";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String IMPACTED_TAG = "Impacted";
    private static final String IMPACT_TICK_TAG = "ImpactTick";
    private static final String MAX_FLIGHT_TICKS_TAG = "MaxFlight";
    private static final String INITIAL_FORWARD_TAG = "InitialForward";
    private static final String VECTOR_X_TAG = "X";
    private static final String VECTOR_Y_TAG = "Y";
    private static final String VECTOR_Z_TAG = "Z";
    private static final int IMPACT_LIFETIME_TICKS = 20 * 5;
    private static final int MAX_FLIGHT_TICKS = 20 * 20;
    private static final int POSITION_HISTORY_LIMIT = 4;
    private static final double DRAG = 0.99D;
    private static final double GRAVITY = 0.05D;
    private static final double BLOCK_IMPACT_OFFSET = 0.05D;
    private static final double ENTITY_IMPACT_OFFSET = 0.01D;
    private static final double MIN_HISTORY_DISTANCE_SQR = 0.01D;
    private static final double MAX_HISTORY_DISTANCE_SQR = 256.0D;
    private static final double MIN_INITIAL_ALIGNMENT = -0.25D;

    private ItemStack weaponStack = new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get());
    private ChargedTwinBladeStaffSpellPayload spellPayload = ChargedTwinBladeStaffSpellPayload.EMPTY;
    private UUID ownerUuid;
    private Vec3 initialForward = Vec3.ZERO;
    private final Deque<Vec3> recentFlightPositions = new ArrayDeque<>(POSITION_HISTORY_LIMIT);
    private boolean impacted;
    private int impactTick = -1;
    private int maxFlightTicks = MAX_FLIGHT_TICKS;
    private boolean clientPredictedBlockImpact;
    private RotationTools.YawPitch clientPredictedImpactRotation;

    public ChargedTwinBladeStaffThrownEntity(EntityType<? extends ChargedTwinBladeStaffThrownEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ChargedTwinBladeStaffThrownEntity(
            EntityType<? extends ChargedTwinBladeStaffThrownEntity> entityType,
            Level level,
            LivingEntity owner,
            ItemStack weaponStack,
            ChargedTwinBladeStaffSpellPayload spellPayload
    ) {
        super(entityType, level);
        this.weaponStack = weaponStack.copy();
        this.spellPayload = spellPayload;
        this.ownerUuid = owner.getUUID();
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            updateClientImpactPrediction();
            return;
        }

        if (impacted) {
            if (impactTick >= 0 && tickCount - impactTick >= IMPACT_LIFETIME_TICKS) {
                spawnImpactExpiryParticles();
                discard();
            }
            return;
        }

        if (tickCount > maxFlightTicks) {
            discard();
            return;
        }

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hitResult)) {
            onHit(hitResult);
            if (impacted) {
                return;
            }
        }

        recordRecentFlightPosition(position());
        move(MoverType.SELF, getDeltaMovement());
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
        // トライデント寄せで、水中でも追加の減速を掛けず一定の drag だけで飛ばす。
        setDeltaMovement(getDeltaMovement().scale(DRAG).add(0.0D, -GRAVITY, 0.0D));
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        setInitialForwardFromMovement();
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity target) {
        return super.canHitEntity(target) && CombatTools.isValidCombatTarget(target, getOwner());
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (level().isClientSide) {
            return;
        }

        var hitEntity = hitResult.getEntity();
        var owner = getOwner();
        var damageSource = damageSources().trident(this, owner == null ? this : owner);
        var impactForward = resolveImpactForward(hitResult.getLocation());
        var damage = level() instanceof ServerLevel serverLevel
                ? ChargedTwinBladeStaff.resolveThrownDamage(serverLevel, weaponStack, hitEntity, damageSource)
                : (float) ChargedTwinBladeStaff.resolveThrownDamage(weaponStack);

        var protectedTarget = CombatTools.isProtectedCombatTarget(
                CombatTools.resolutePartEntity(hitEntity), owner,
                CombatTools.CombatTargetPolicy.PROTECT_SELF_AND_ALLIES
        );
        if (!protectedTarget && hitEntity.hurt(damageSource, damage) && hitEntity instanceof LivingEntity livingTarget) {
            if (level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, livingTarget, damageSource, weaponStack);
            }
        }

        finishImpact(hitResult.getLocation().subtract(impactForward.scale(ENTITY_IMPACT_OFFSET)), impactForward, SoundEvents.TRIDENT_HIT);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!level().isClientSide) {
            var blockImpactPosition = hitResult.getLocation().add(
                    Vec3.atLowerCornerOf(hitResult.getDirection().getNormal()).scale(BLOCK_IMPACT_OFFSET)
            );
            var impactForward = resolveImpactForward(blockImpactPosition);
            finishImpact(blockImpactPosition, impactForward, SoundEvents.TRIDENT_HIT_GROUND);
        }
    }

    private void finishImpact(Vec3 impactPosition, Vec3 impactForward, net.minecraft.sounds.SoundEvent impactSound) {
        var impactRotation = calculateImpactRotation(impactForward);
        setPos(impactPosition.x, impactPosition.y, impactPosition.z);
        setDeltaMovement(Vec3.ZERO);
        impacted = true;
        impactTick = tickCount;
        entityData.set(DATA_IMPACTED, true);
        entityData.set(DATA_IMPACT_YAW, impactRotation.yaw());
        entityData.set(DATA_IMPACT_PITCH, impactRotation.pitch());
        freezeRotation(impactRotation);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, impactPosition.x, impactPosition.y, impactPosition.z,
                    impactSound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (level() instanceof ServerLevel serverLevel) {
            if (canSummonLightning(serverLevel)) {
                var lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (lightningBolt != null) {
                    lightningBolt.moveTo(impactPosition);
                    if (resolveOwnerPlayer(serverLevel) instanceof ServerPlayer serverPlayer) {
                        lightningBolt.setCause(serverPlayer);
                    }
                    serverLevel.addFreshEntity(lightningBolt);
                    serverLevel.playSound(null, impactPosition.x, impactPosition.y, impactPosition.z,
                            SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 5.0F, 1.0F);
                }
            }

            if (resolveOwnerPlayer(serverLevel) instanceof ServerPlayer serverPlayer) {
                ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                        serverLevel,
                        serverPlayer,
                        weaponStack,
                        spellPayload,
                        impactPosition,
                        impactForward
                );
                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(MagicData.getPlayerMagicData(serverPlayer)));
            }
        }
    }

    private RotationTools.YawPitch calculateImpactRotation(Vec3 impactForward) {
        return RotationTools.calculateYawPitchByDirection(impactForward);
    }

    private void freezeRotation(RotationTools.YawPitch impactRotation) {
        setYRot(impactRotation.yaw());
        setXRot(impactRotation.pitch());
        yRotO = impactRotation.yaw();
        xRotO = impactRotation.pitch();
    }

    private void updateClientImpactPrediction() {
        if (isImpacted()) {
            clientPredictedBlockImpact = false;
            clientPredictedImpactRotation = null;
            freezeRotation(getSyncedImpactRotation());
            return;
        }

        var movement = getDeltaMovement();
        if (movement.lengthSqr() <= 1.0E-6D) {
            clientPredictedBlockImpact = false;
            clientPredictedImpactRotation = null;
            return;
        }

        var hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            clientPredictedBlockImpact = true;
            clientPredictedImpactRotation = calculateImpactRotation(movement.normalize());
            return;
        }

        clientPredictedBlockImpact = false;
        clientPredictedImpactRotation = null;
    }

    private void spawnImpactExpiryParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 18, 0.18D, 0.18D, 0.18D, 0.02D);
    }

    private boolean canSummonLightning(ServerLevel level) {
        return hasChanneling(weaponStack)
                && level.isThundering()
                && level.canSeeSky(blockPosition());
    }

    private Vec3 resolveImpactForward(Vec3 impactPosition) {
        var oldestPosition = recentFlightPositions.peekFirst();
        var historyForward = oldestPosition == null ? Vec3.ZERO : impactPosition.subtract(oldestPosition);
        if (isUsableHistoryForward(historyForward, initialForward)) {
            return historyForward.normalize();
        }

        if (isUsableForward(initialForward)) {
            return initialForward.normalize();
        }

        var movement = getDeltaMovement();
        if (isUsableForward(movement)) {
            return movement.normalize();
        }
        return calculateViewVector(getXRot(), getYRot());
    }

    public static Vec3 resolveImpactForwardForTesting(Vec3 impactPosition, Vec3 oldestHistoryPosition, Vec3 initialForward) {
        var historyForward = impactPosition.subtract(oldestHistoryPosition);
        if (isUsableHistoryForward(historyForward, initialForward)) {
            return historyForward.normalize();
        }
        if (isUsableForward(initialForward)) {
            return initialForward.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    private void recordRecentFlightPosition(Vec3 position) {
        recentFlightPositions.addLast(position);
        while (recentFlightPositions.size() > POSITION_HISTORY_LIMIT) {
            recentFlightPositions.removeFirst();
        }
    }

    private void setInitialForwardFromMovement() {
        var movement = getDeltaMovement();
        if (isUsableForward(movement)) {
            initialForward = movement.normalize();
        }
    }

    private static boolean isUsableHistoryForward(Vec3 historyForward, Vec3 initialForward) {
        if (!isFinite(historyForward)) {
            return false;
        }
        var lengthSqr = historyForward.lengthSqr();
        if (lengthSqr < MIN_HISTORY_DISTANCE_SQR || lengthSqr > MAX_HISTORY_DISTANCE_SQR) {
            return false;
        }
        if (isUsableForward(initialForward) && historyForward.normalize().dot(initialForward.normalize()) < MIN_INITIAL_ALIGNMENT) {
            return false;
        }
        return true;
    }

    private static boolean isUsableForward(Vec3 forward) {
        return isFinite(forward) && forward.lengthSqr() > 1.0E-6D;
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private Player resolveOwnerPlayer(ServerLevel level) {
        if (getOwner() instanceof Player player) {
            return player;
        }
        return ownerUuid == null ? null : level.getPlayerByUUID(ownerUuid);
    }

    public ItemStack getRenderStack() {
        var renderStack = weaponStack.copy();
        renderStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(THROWN_RENDER_CUSTOM_MODEL_DATA));
        return renderStack;
    }

    public boolean isImpacted() {
        return impacted || entityData.get(DATA_IMPACTED);
    }

    public RotationTools.YawPitch resolveRenderYawPitch(float partialTicks) {
        if (clientPredictedBlockImpact && clientPredictedImpactRotation != null) {
            return clientPredictedImpactRotation;
        }

        if (isImpacted()) {
            return getSyncedImpactRotation();
        }

        var motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            return RotationTools.calculateYawPitchByDirection(motion);
        }

        return RotationTools.calculateYawPitchByEntity(this, partialTicks);
    }

    public boolean isClientPredictingBlockImpact() {
        return clientPredictedBlockImpact;
    }

    private RotationTools.YawPitch getSyncedImpactRotation() {
        return new RotationTools.YawPitch(entityData.get(DATA_IMPACT_YAW), entityData.get(DATA_IMPACT_PITCH));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IMPACTED, false);
        builder.define(DATA_IMPACT_YAW, 0.0F);
        builder.define(DATA_IMPACT_PITCH, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains(STACK_TAG)) {
            weaponStack = ItemStack.parseOptional(registryAccess(), tag.getCompound(STACK_TAG));
        }
        spellPayload = ChargedTwinBladeStaffSpellPayload.load(tag.getCompound(SPELL_PAYLOAD_TAG));
        if (tag.hasUUID(OWNER_UUID_TAG)) {
            ownerUuid = tag.getUUID(OWNER_UUID_TAG);
        }
        initialForward = readVec3(tag.getCompound(INITIAL_FORWARD_TAG));
        impacted = tag.getBoolean(IMPACTED_TAG);
        entityData.set(DATA_IMPACTED, impacted);
        if (impacted) {
            entityData.set(DATA_IMPACT_YAW, getYRot());
            entityData.set(DATA_IMPACT_PITCH, getXRot());
        }
        impactTick = tag.getInt(IMPACT_TICK_TAG);
        maxFlightTicks = tag.contains(MAX_FLIGHT_TICKS_TAG) ? tag.getInt(MAX_FLIGHT_TICKS_TAG) : MAX_FLIGHT_TICKS;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.put(STACK_TAG, weaponStack.saveOptional(registryAccess()));
        tag.put(SPELL_PAYLOAD_TAG, spellPayload.save());
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.put(INITIAL_FORWARD_TAG, saveVec3(initialForward));
        tag.putBoolean(IMPACTED_TAG, impacted);
        tag.putInt(IMPACT_TICK_TAG, impactTick);
        tag.putInt(MAX_FLIGHT_TICKS_TAG, maxFlightTicks);
    }

    private static CompoundTag saveVec3(Vec3 vector) {
        var tag = new CompoundTag();
        tag.putDouble(VECTOR_X_TAG, vector.x);
        tag.putDouble(VECTOR_Y_TAG, vector.y);
        tag.putDouble(VECTOR_Z_TAG, vector.z);
        return tag;
    }

    private static Vec3 readVec3(CompoundTag tag) {
        return new Vec3(tag.getDouble(VECTOR_X_TAG), tag.getDouble(VECTOR_Y_TAG), tag.getDouble(VECTOR_Z_TAG));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
    }

    @Override
    public boolean isPickable() {
        // これは意図的に拾えないようにするため必要.
        return false;
    }

    private static boolean hasChanneling(ItemStack stack) {
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            if (entry.getKey().is(net.minecraft.world.item.enchantment.Enchantments.CHANNELING)) {
                return entry.getIntValue() > 0;
            }
        }
        return false;
    }
}
