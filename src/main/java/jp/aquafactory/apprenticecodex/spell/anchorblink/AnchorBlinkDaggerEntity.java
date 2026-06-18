package jp.aquafactory.apprenticecodex.spell.anchorblink;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnchorBlinkDaggerEntity extends ThrowableProjectile implements AntiMagicSusceptible {
    private static final EntityDataAccessor<Boolean> DATA_IMPACTED =
            SynchedEntityData.defineId(AnchorBlinkDaggerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_IMPACT_YAW =
            SynchedEntityData.defineId(AnchorBlinkDaggerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IMPACT_PITCH =
            SynchedEntityData.defineId(AnchorBlinkDaggerEntity.class, EntityDataSerializers.FLOAT);
    private static final String DAMAGE_TAG = "Damage";
    private static final String MAXIMUM_RANGE_TAG = "MaximumRange";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String IMPACTED_TAG = "Impacted";
    private static final String IMPACT_GAME_TIME_TAG = "ImpactGameTime";
    private static final String IMPACT_FORWARD_TAG = "ImpactForward";
    private static final String VECTOR_X_TAG = "X";
    private static final String VECTOR_Y_TAG = "Y";
    private static final String VECTOR_Z_TAG = "Z";
    private static final int IMPACT_LIFETIME_TICKS = 20 * 3;
    private static final int MAX_FLIGHT_TICKS = 20 * 8;
    private static final int POST_BLINK_PROTECTION_TICKS = 20 * 2;
    private static final double ENTITY_IMPACT_OFFSET = 0.01D;
    private static final double BLOCK_IMPACT_OFFSET = 0.05D;
    private static final double TELEPORT_BACK_OFFSET = 1.0D;
    private static final double MIN_VECTOR_LENGTH_SQR = 1.0E-8D;
    private static final Map<UUID, ActiveAnchor> ACTIVE_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, DamageProtection> DAMAGE_PROTECTIONS = new ConcurrentHashMap<>();

    private float damage;
    private float maximumRange = Float.MAX_VALUE;
    private @Nullable UUID ownerUuid;
    private boolean impacted;
    private long impactGameTime = -1L;
    private Vec3 impactForward = new Vec3(0.0D, 0.0D, 1.0D);

    public AnchorBlinkDaggerEntity(EntityType<? extends AnchorBlinkDaggerEntity> entityType, Level level) {
        super(entityType, level);
        setViewScale(8.0F);
    }

    public AnchorBlinkDaggerEntity(EntityType<? extends AnchorBlinkDaggerEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, owner, level);
        ownerUuid = owner.getUUID();
    }

    @Override
    public void tick() {
        if (isImpacted()) {
            super.tick();
            setDeltaMovement(Vec3.ZERO);
            freezeRotation(getSyncedImpactRotation());
            if (!level().isClientSide && impactGameTime >= 0L
                    && level().getGameTime() - impactGameTime >= IMPACT_LIFETIME_TICKS) {
                discard();
            }
            return;
        }

        super.tick();

        if (!level().isClientSide && tickCount > MAX_FLIGHT_TICKS) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity target) {
        return super.canHitEntity(target) && CombatTools.isValidCombatTarget(target, getOwner());
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(level() instanceof ServerLevel)) {
            return;
        }

        var owner = getOwner();
        var target = CombatTools.resolutePartEntity(hitResult.getEntity());
        var source = CombatTools.getDamageSource(level(), this, owner, DamageTypes.ANCHOR_BLINK);
        CombatTools.applyDamage(
                target,
                damage,
                source,
                SpellRegistry.ANCHOR_BLINK.get().getSchoolType(),
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );

        var forward = resolveCurrentForward();
        finishImpact(hitResult.getLocation().subtract(forward.scale(ENTITY_IMPACT_OFFSET)), forward);
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level().isClientSide) {
            return;
        }

        var forward = resolveCurrentForward();
        var impactPosition = hitResult.getLocation().add(
                Vec3.atLowerCornerOf(hitResult.getDirection().getNormal()).scale(BLOCK_IMPACT_OFFSET)
        );
        finishImpact(impactPosition, forward);
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (!level().isClientSide) {
            discard();
        }
    }

    private void finishImpact(Vec3 position, Vec3 forward) {
        if (isImpacted()) {
            return;
        }

        var owner = level() instanceof ServerLevel serverLevel
                ? resolveOwnerPlayer(serverLevel)
                : Optional.<ServerPlayer>empty();
        if (owner.isPresent() && isBeyondMaximumRange(owner.get(), position)) {
            owner.get().connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.anchor_blink.too_far")
                            .withStyle(ChatFormatting.RED)
            ));
            discard();
            return;
        }

        impacted = true;
        impactGameTime = level().getGameTime();
        impactForward = normalizeOrFallback(forward, impactForward);
        setPos(position);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);

        var rotation = RotationTools.calculateYawPitchByDirection(impactForward);
        freezeRotation(rotation);
        entityData.set(DATA_IMPACTED, true);
        entityData.set(DATA_IMPACT_YAW, rotation.yaw());
        entityData.set(DATA_IMPACT_PITCH, rotation.pitch());

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, position.x, position.y, position.z,
                    SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.8F, 1.2F);
            owner.ifPresent(anchorOwner -> {
                ACTIVE_ANCHORS.put(anchorOwner.getUUID(), new ActiveAnchor(serverLevel.dimension(), getId()));
                anchorOwner.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.anchor_blink.ready_for_blink")
                                .withStyle(ChatFormatting.AQUA)
                ));
            });
        }
    }

    public static boolean tryBlink(ServerPlayer player) {
        var activeAnchor = ACTIVE_ANCHORS.get(player.getUUID());
        if (activeAnchor == null || !activeAnchor.dimension().equals(player.serverLevel().dimension())) {
            return false;
        }

        var entity = player.serverLevel().getEntity(activeAnchor.entityId());
        if (!(entity instanceof AnchorBlinkDaggerEntity dagger) || !dagger.isImpacted()) {
            ACTIVE_ANCHORS.remove(player.getUUID(), activeAnchor);
            return false;
        }

        return dagger.tryBlinkOwner(player);
    }

    public static boolean isProtectedFromEnemyDamage(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
        var protection = DAMAGE_PROTECTIONS.get(player.getUUID());
        if (protection == null || protection.isExpired(player)) {
            DAMAGE_PROTECTIONS.remove(player.getUUID(), protection);
            return false;
        }

        var attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getDirectEntity();
        }
        return CombatTools.canBeHostileToMe(attacker, player);
    }

    public static void cleanupExpiredProtection(ServerLevel level) {
        DAMAGE_PROTECTIONS.entrySet().removeIf(entry -> entry.getValue().isExpiredForCleanup(level));
    }

    public static void grantDamageProtectionForTesting(ServerPlayer player, int ticks) {
        DAMAGE_PROTECTIONS.put(player.getUUID(), new DamageProtection(
                player.serverLevel().dimension(),
                player.serverLevel().getGameTime() + ticks
        ));
    }

    private boolean tryBlinkOwner(ServerPlayer player) {
        var destination = resolveTeleportDestination(player);
        if (destination.isEmpty()) {
            return false;
        }

        var target = destination.get();
        Utils.handleSpellTeleport(SpellRegistry.ANCHOR_BLINK.get(), player, target);
        player.fallDistance = 0.0F;
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0F, 1.0F);
        DAMAGE_PROTECTIONS.put(player.getUUID(), new DamageProtection(
                player.serverLevel().dimension(),
                player.serverLevel().getGameTime() + POST_BLINK_PROTECTION_TICKS
        ));
        ACTIVE_ANCHORS.remove(player.getUUID());
        discard();
        return true;
    }

    private Optional<Vec3> resolveTeleportDestination(ServerPlayer player) {
        var backward = impactForward.normalize().scale(-TELEPORT_BACK_OFFSET);
        var center = position().add(backward);
        var right = new Vec3(-impactForward.z, 0.0D, impactForward.x);
        if (right.lengthSqr() <= MIN_VECTOR_LENGTH_SQR) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        var up = new Vec3(0.0D, 1.0D, 0.0D);
        double[] verticalOffsets = {0.0D, -0.5D, 0.5D, -1.0D, 1.0D};
        double[] sideOffsets = {0.0D, 0.35D, -0.35D, 0.7D, -0.7D};
        for (var vertical : verticalOffsets) {
            for (var side : sideOffsets) {
                var candidate = center.add(up.scale(vertical)).add(right.scale(side));
                if (canStandAt(player, candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isBeyondMaximumRange(ServerPlayer owner, Vec3 position) {
        return owner.position().distanceToSqr(position) > (double) maximumRange * maximumRange;
    }

    private static boolean canStandAt(ServerPlayer player, Vec3 candidate) {
        var level = player.serverLevel();
        var dimensions = player.getDimensions(Pose.STANDING);
        var box = dimensions.makeBoundingBox(candidate.x, candidate.y, candidate.z).deflate(1.0E-7D);
        return level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(candidate))
                && level.noCollision(player, box)
                && level.getBlockStates(box).allMatch(state -> state.getFluidState().isEmpty());
    }

    private Optional<ServerPlayer> resolveOwnerPlayer(ServerLevel level) {
        if (getOwner() instanceof ServerPlayer serverPlayer) {
            return Optional.of(serverPlayer);
        }
        if (ownerUuid == null) {
            return Optional.empty();
        }
        var player = level.getPlayerByUUID(ownerUuid);
        return player instanceof ServerPlayer serverPlayer ? Optional.of(serverPlayer) : Optional.empty();
    }

    private Vec3 resolveCurrentForward() {
        return normalizeOrFallback(getDeltaMovement(), calculateViewVector(getXRot(), getYRot()));
    }

    public ItemStack getRenderStack() {
        return new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get());
    }

    public boolean isImpacted() {
        return impacted || entityData.get(DATA_IMPACTED);
    }

    public RotationTools.YawPitch resolveRenderYawPitch(float partialTicks) {
        if (isImpacted()) {
            return getSyncedImpactRotation();
        }

        var movement = getDeltaMovement();
        if (movement.lengthSqr() > MIN_VECTOR_LENGTH_SQR) {
            return RotationTools.calculateYawPitchByDirection(movement);
        }

        return RotationTools.calculateYawPitchByEntity(this, partialTicks);
    }

    public float getDamageForTesting() {
        return damage;
    }

    public void impactForTesting(Vec3 position) {
        finishImpact(position, new Vec3(0.0D, 0.0D, 1.0D));
    }

    private void freezeRotation(RotationTools.YawPitch rotation) {
        setYRot(rotation.yaw());
        setXRot(rotation.pitch());
        yRotO = rotation.yaw();
        xRotO = rotation.pitch();
    }

    private RotationTools.YawPitch getSyncedImpactRotation() {
        return new RotationTools.YawPitch(entityData.get(DATA_IMPACT_YAW), entityData.get(DATA_IMPACT_PITCH));
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        if (vector != null && vector.lengthSqr() > MIN_VECTOR_LENGTH_SQR) {
            return vector.normalize();
        }
        if (fallback != null && fallback.lengthSqr() > MIN_VECTOR_LENGTH_SQR) {
            return fallback.normalize();
        }
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_IMPACTED, false);
        entityData.define(DATA_IMPACT_YAW, 0.0F);
        entityData.define(DATA_IMPACT_PITCH, 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(DAMAGE_TAG, damage);
        tag.putFloat(MAXIMUM_RANGE_TAG, maximumRange);
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.putBoolean(IMPACTED_TAG, impacted);
        tag.putLong(IMPACT_GAME_TIME_TAG, impactGameTime);
        tag.put(IMPACT_FORWARD_TAG, saveVec3(impactForward));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat(DAMAGE_TAG);
        maximumRange = tag.contains(MAXIMUM_RANGE_TAG) ? tag.getFloat(MAXIMUM_RANGE_TAG) : Float.MAX_VALUE;
        if (tag.hasUUID(OWNER_UUID_TAG)) {
            ownerUuid = tag.getUUID(OWNER_UUID_TAG);
        }
        impacted = tag.getBoolean(IMPACTED_TAG);
        impactGameTime = tag.getLong(IMPACT_GAME_TIME_TAG);
        impactForward = readVec3(tag.getCompound(IMPACT_FORWARD_TAG));
        entityData.set(DATA_IMPACTED, impacted);
        if (impacted) {
            var rotation = RotationTools.calculateYawPitchByDirection(impactForward);
            entityData.set(DATA_IMPACT_YAW, rotation.yaw());
            entityData.set(DATA_IMPACT_PITCH, rotation.pitch());
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide && ownerUuid != null) {
            ACTIVE_ANCHORS.computeIfPresent(ownerUuid, (uuid, anchor) -> anchor.entityId() == getId() ? null : anchor);
        }
        super.remove(reason);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0D * 128.0D;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setMaximumRange(float maximumRange) {
        this.maximumRange = maximumRange;
    }

    private static CompoundTag saveVec3(Vec3 vector) {
        var tag = new CompoundTag();
        tag.putDouble(VECTOR_X_TAG, vector.x);
        tag.putDouble(VECTOR_Y_TAG, vector.y);
        tag.putDouble(VECTOR_Z_TAG, vector.z);
        return tag;
    }

    private static Vec3 readVec3(CompoundTag tag) {
        return normalizeOrFallback(
                new Vec3(tag.getDouble(VECTOR_X_TAG), tag.getDouble(VECTOR_Y_TAG), tag.getDouble(VECTOR_Z_TAG)),
                new Vec3(0.0D, 0.0D, 1.0D)
        );
    }

    private record ActiveAnchor(net.minecraft.resources.ResourceKey<Level> dimension, int entityId) {
    }

    private record DamageProtection(net.minecraft.resources.ResourceKey<Level> dimension, long untilGameTime) {
        private boolean isExpired(ServerPlayer player) {
            return isExpired(player.serverLevel());
        }

        private boolean isExpired(ServerLevel level) {
            return !dimension.equals(level.dimension()) || level.getGameTime() >= untilGameTime;
        }

        private boolean isExpiredForCleanup(ServerLevel level) {
            return dimension.equals(level.dimension()) && level.getGameTime() >= untilGameTime;
        }
    }
}
