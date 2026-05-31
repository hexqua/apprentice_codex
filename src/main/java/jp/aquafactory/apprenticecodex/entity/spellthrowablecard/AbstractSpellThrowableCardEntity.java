package jp.aquafactory.apprenticecodex.entity.spellthrowablecard;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffSpellCastManager;
import jp.aquafactory.apprenticecodex.item.spellthrowablecard.AbstractSpellThrowableCardItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractSpellThrowableCardEntity extends Projectile {
    private static final EntityDataAccessor<ItemStack> DATA_CARD_STACK =
            SynchedEntityData.defineId(AbstractSpellThrowableCardEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final String STACK_TAG = "CardStack";
    private static final String OWNER_UUID_TAG = "OwnerUuid";
    private static final String INITIAL_FORWARD_TAG = "InitialForward";
    private static final String VECTOR_X_TAG = "X";
    private static final String VECTOR_Y_TAG = "Y";
    private static final String VECTOR_Z_TAG = "Z";
    private static final int MAX_LIFETIME_TICKS = 20 * 20;

    private ItemStack cardStack = new ItemStack(ItemRegistry.SPELL_INVOKE_CARD.get());
    private UUID ownerUuid;
    protected Vec3 initialForward = Vec3.ZERO;

    protected AbstractSpellThrowableCardEntity(EntityType<? extends AbstractSpellThrowableCardEntity> entityType, Level level) {
        super(entityType, level);
    }

    protected AbstractSpellThrowableCardEntity(
            EntityType<? extends AbstractSpellThrowableCardEntity> entityType,
            Level level,
            LivingEntity owner,
            ItemStack cardStack
    ) {
        super(entityType, level);
        setCardStack(cardStack);
        ownerUuid = owner.getUUID();
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity, inaccuracy);
        setInitialForwardFromMovement();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    protected void castAndDiscard(Vec3 castPosition, Vec3 forward) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        var owner = resolveOwnerPlayer(serverLevel);
        if (!(owner instanceof ServerPlayer serverPlayer)) {
            discard();
            return;
        }

        var stack = getCardStack();
        if (stack.getItem() instanceof AbstractSpellThrowableCardItem cardItem) {
            ChargedTwinBladeStaffSpellCastManager.tryCastAtImpact(
                    serverLevel,
                    serverPlayer,
                    stack,
                    cardItem.createImpactPayload(stack),
                    castPosition,
                    normalizeForward(forward),
                    RemoteOwnerCastOrigin.CHARGED_TWIN_BLADE_STAFF_IMPACT,
                    true
            );
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(MagicData.getPlayerMagicData(serverPlayer)));
        }
        discard();
    }

    public ItemStack getCardStack() {
        var syncedStack = entityData.get(DATA_CARD_STACK);
        return syncedStack.isEmpty() ? cardStack : syncedStack;
    }

    protected void setCardStack(ItemStack stack) {
        cardStack = stack.copy();
        cardStack.setCount(1);
        if (entityData != null) {
            entityData.set(DATA_CARD_STACK, cardStack.copy());
        }
    }

    public RotationTools.YawPitch resolveRenderYawPitch(float partialTicks) {
        var motion = getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            return RotationTools.calculateYawPitchByDirection(motion);
        }
        if (initialForward.lengthSqr() > 1.0E-6D) {
            return RotationTools.calculateYawPitchByDirection(initialForward);
        }
        return RotationTools.calculateYawPitchByEntity(this, partialTicks);
    }

    protected Vec3 resolveForward() {
        var movement = getDeltaMovement();
        if (isUsableForward(movement)) {
            return movement.normalize();
        }
        if (isUsableForward(initialForward)) {
            return initialForward.normalize();
        }
        return calculateViewVector(getXRot(), getYRot());
    }

    protected Player resolveOwnerPlayer(ServerLevel level) {
        if (getOwner() instanceof Player player) {
            return player;
        }
        return ownerUuid == null ? null : level.getPlayerByUUID(ownerUuid);
    }

    protected static Vec3 normalizeForward(Vec3 forward) {
        return isUsableForward(forward) ? forward.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private void setInitialForwardFromMovement() {
        var movement = getDeltaMovement();
        if (isUsableForward(movement)) {
            initialForward = movement.normalize();
        }
    }

    private static boolean isUsableForward(Vec3 forward) {
        return Double.isFinite(forward.x)
                && Double.isFinite(forward.y)
                && Double.isFinite(forward.z)
                && forward.lengthSqr() > 1.0E-6D;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_CARD_STACK, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.contains(STACK_TAG)) {
            setCardStack(ItemStack.of(tag.getCompound(STACK_TAG)));
        }
        if (tag.hasUUID(OWNER_UUID_TAG)) {
            ownerUuid = tag.getUUID(OWNER_UUID_TAG);
        }
        initialForward = readVec3(tag.getCompound(INITIAL_FORWARD_TAG));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.put(STACK_TAG, getCardStack().save(new CompoundTag()));
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.put(INITIAL_FORWARD_TAG, saveVec3(initialForward));
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

    @Override
    public boolean isPickable() {
        return false;
    }
}
