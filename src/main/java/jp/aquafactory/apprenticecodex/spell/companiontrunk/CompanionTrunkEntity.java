package jp.aquafactory.apprenticecodex.spell.companiontrunk;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CompanionTrunkEntity extends PathfinderMob implements GeoEntity, Container {
    public static final float WIDTH = 0.95f;
    public static final float HEIGHT = 1.1f;

    private static final double FOLLOW_START_DISTANCE = 3.0;
    private static final double FOLLOW_STOP_DISTANCE = 1.75;
    private static final double TELEPORT_DISTANCE = 24.0;
    private static final double HORIZONTAL_JUMP_SPEED = 0.32;
    private static final double JUMP_VERTICAL_SPEED = 0.5;
    private static final int JUMP_COOLDOWN_TICK = 10;
    private static final int HEAL_INTERVAL_TICK = 80;
    private static final double GROUND_HORIZONTAL_DAMPING = 0.08;
    private static final int JUMP_MAIN_DURATION_TICK = 15;
    private static final int OPEN_DURATION_TICK = 10;
    private static final int CLOSE_DURATION_TICK = 5;
    private static final int LAND_DURATION_TICK = 10;

    private static final EntityDataAccessor<Integer> LID_ANIMATION_STATE =
            SynchedEntityData.defineId(CompanionTrunkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LID_ANIMATION_SERIAL =
            SynchedEntityData.defineId(CompanionTrunkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BODY_ANIMATION_STATE =
            SynchedEntityData.defineId(CompanionTrunkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BODY_ANIMATION_SERIAL =
            SynchedEntityData.defineId(CompanionTrunkEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_JUMP_MAIN = RawAnimation.begin().thenPlay("jump_main");
    private static final RawAnimation ANIM_OPEN = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation ANIM_CLOSE = RawAnimation.begin().thenPlay("close");
    private static final RawAnimation ANIM_JUMP_BODY = RawAnimation.begin().thenPlay("jump_body");
    private static final RawAnimation ANIM_LAND_BODY = RawAnimation.begin().thenPlay("land_body");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> openers = new HashSet<>();

    private @Nullable UUID ownerUuid;
    private @Nullable Player cachedOwner;
    private boolean wasOnGround;
    private int jumpCooldownTick;
    private int lidAnimationTick;
    private int bodyAnimationTick;
    private boolean removalHandled;
    private int clientLastLidAnimationSerial;
    private int clientLastBodyAnimationSerial;

    public CompanionTrunkEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        wasOnGround = true;
    }

    public CompanionTrunkEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player owner) {
        this(entityType, level);
        setOwner(owner);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
                .add(Attributes.FOLLOW_RANGE, TELEPORT_DISTANCE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LID_ANIMATION_STATE, LidAnimationState.IDLE.id);
        entityData.define(LID_ANIMATION_SERIAL, 0);
        entityData.define(BODY_ANIMATION_STATE, BodyAnimationState.IDLE.id);
        entityData.define(BODY_ANIMATION_SERIAL, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            tickOnServer(serverLevel);
        }
    }

    private void tickOnServer(ServerLevel level) {
        var owner = getOwner();
        if (owner == null || owner.isRemoved()) {
            discardWithoutInventory();
            return;
        }

        if (!owner.isAlive()) {
            dropAllContentsAndDiscard();
            return;
        }

        if (owner.level() != level) {
            discardWithoutInventory();
            return;
        }

        if (jumpCooldownTick > 0) {
            --jumpCooldownTick;
        }

        if (tickCount % HEAL_INTERVAL_TICK == 0 && isAlive() && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }

        tickAnimationPhases();
        updateBuoyancy();
        updateLandingAnimation();
        tickFollowOwner(owner);
    }

    private void tickAnimationPhases() {
        if (lidAnimationTick > 0 && --lidAnimationTick <= 0) {
            finishLidAnimationPhase();
        }

        if (bodyAnimationTick > 0 && --bodyAnimationTick <= 0) {
            finishBodyAnimationPhase();
        }
    }

    private void tickFollowOwner(Player owner) {
        var distanceSqr = distanceToSqr(owner);
        if (distanceSqr >= TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            teleportNearOwner(owner);
            return;
        }

        if (distanceSqr <= FOLLOW_STOP_DISTANCE * FOLLOW_STOP_DISTANCE) {
            getNavigation().stop();
            dampGroundMotion();
            setXRot(0.0f);
            return;
        }

        getNavigation().stop();
        if (onGround()) {
            dampGroundMotion();
        }

        if (onGround() && jumpCooldownTick <= 0 && distanceSqr >= FOLLOW_START_DISTANCE * FOLLOW_START_DISTANCE) {
            performFollowJump(owner.position().subtract(position()));
        }

        var movement = getDeltaMovement();
        if (movement.horizontalDistanceSqr() > 1.0E-4) {
            setYRot((float) (Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0f);
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
        setXRot(0.0f);
    }

    private void performFollowJump(Vec3 toOwner) {
        var horizontal = new Vec3(toOwner.x, 0.0, toOwner.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return;
        }

        var direction = horizontal.normalize();
        setDeltaMovement(
                direction.x * HORIZONTAL_JUMP_SPEED,
                JUMP_VERTICAL_SPEED,
                direction.z * HORIZONTAL_JUMP_SPEED
        );
        hasImpulse = true;
        jumpCooldownTick = JUMP_COOLDOWN_TICK;

        // ラッチは open と排他なので、誰かが開いている間は open 姿勢を優先する.
        if (openers.isEmpty()) {
            playLidJumpAnimation();
        }
        startBodyJumpAnimation();
    }

    private void dampGroundMotion() {
        var movement = getDeltaMovement();
        if (Math.abs(movement.x) <= 1.0E-4 && Math.abs(movement.z) <= 1.0E-4) {
            return;
        }

        setDeltaMovement(movement.x * GROUND_HORIZONTAL_DAMPING, movement.y, movement.z * GROUND_HORIZONTAL_DAMPING);
        hasImpulse = true;
    }

    private void updateLandingAnimation() {
        if (!onGround()) {
            wasOnGround = false;
            return;
        }

        if (!wasOnGround) {
            startBodyLandAnimation();
        }
        wasOnGround = true;
    }

    private void updateBuoyancy() {
        if (!isInWaterOrBubble() && !isInLava()) {
            return;
        }

        var movement = getDeltaMovement();
        setDeltaMovement(movement.x * 0.92, Math.max(movement.y, 0.08), movement.z * 0.92);
        hasImpulse = true;
        fallDistance = 0.0f;
    }

    private void teleportNearOwner(Player owner) {
        var destination = findTeleportDestination(owner);
        moveTo(destination.x, destination.y, destination.z, owner.getYRot(), 0.0f);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0f;
        level().playSound(null, blockPosition(), SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private Vec3 findTeleportDestination(Player owner) {
        var basePos = owner.blockPosition();
        int[][] offsets = new int[][]{
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2}
        };

        for (var yOffset = 0; yOffset <= 2; ++yOffset) {
            for (var offset : offsets) {
                var candidate = basePos.offset(offset[0], yOffset, offset[1]);
                if (canTeleportTo(candidate)) {
                    return Vec3.atBottomCenterOf(candidate);
                }
            }
        }

        return owner.position();
    }

    private boolean canTeleportTo(BlockPos pos) {
        var belowState = level().getBlockState(pos.below());
        if (belowState.isAir() && level().getFluidState(pos.below()).getType() == Fluids.EMPTY) {
            return false;
        }

        var bounds = getDimensions(getPose()).makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return level().noCollision(this, bounds);
    }

    public void setOwner(Player owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
    }

    public @Nullable Player getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }

        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
            if (player != null) {
                cachedOwner = player;
                return player;
            }
        }
        return null;
    }

    public @Nullable UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setCompanionMaxHealth(float maxHealth) {
        var attribute = getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(maxHealth);
        }
        if (getHealth() > maxHealth) {
            setHealth(maxHealth);
        } else if (getHealth() <= 0.0f) {
            setHealth(maxHealth);
        }
    }

    public void applyStoredCustomName() {
        var storage = getStorage();
        super.setCustomName(storage != null ? storage.getCustomName() : null);
    }

    public void discardWithoutInventory() {
        handleRemoval(false, false);
        discard();
    }

    public void dropAllContentsAndDiscard() {
        handleRemoval(true, true);
        discard();
    }

    private void handleRemoval(boolean dropInventory, boolean notifyDestroyed) {
        if (removalHandled) {
            return;
        }

        removalHandled = true;
        closeTrackedMenus();
        if (dropInventory) {
            dropStoredInventory();
        }
        if (notifyDestroyed) {
            notifyOwnerDestroyed();
        }
        openers.clear();
    }

    private void notifyOwnerDestroyed() {
        if (!(level() instanceof ServerLevel serverLevel) || ownerUuid == null) {
            return;
        }

        var owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) {
            CompanionTrunkManager.onTrunkDestroyed(owner, this);
        }
    }

    private void closeTrackedMenus() {
        if (!(level() instanceof ServerLevel serverLevel) || openers.isEmpty()) {
            return;
        }

        for (var openerId : Set.copyOf(openers)) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(openerId);
            if (player != null) {
                player.closeContainer();
            }
        }
    }

    private void dropStoredInventory() {
        var storage = getStorage();
        if (storage == null) {
            return;
        }

        var handler = storage.getHandler();
        for (var i = 0; i < handler.getSlots(); ++i) {
            var stack = handler.extractItem(i, handler.getStackInSlot(i).getCount(), false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level(), getX(), getY(), getZ(), stack);
            }
        }
    }

    private @Nullable ItemStackHandler getHandler() {
        var storage = getStorage();
        return storage == null ? null : storage.getHandler();
    }

    private @Nullable jp.aquafactory.apprenticecodex.capability.companiontrunkinventory.CompanionTrunkInventory getStorage() {
        var owner = getOwner();
        return owner == null ? null : Capabilities.getCompanionTrunkInventoryOrNull(owner);
    }

    private Component getContainerDisplayName() {
        var storage = getStorage();
        if (storage != null) {
            var customName = storage.getCustomName();
            if (customName != null) {
                return customName;
            }
        }
        return Component.translatable("container.apprenticecodex.companion_trunk.default");
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, menuPlayer) -> ChestMenu.threeRows(containerId, playerInventory, this),
                getContainerDisplayName()
        ));
        return InteractionResult.CONSUME;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (!level().isClientSide) {
            var storage = getStorage();
            if (storage != null) {
                storage.setCustomName(name);
            }
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isOwnerDamageSource(source)
                || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                || source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    private boolean isOwnerDamageSource(DamageSource source) {
        var owner = getOwner();
        if (owner == null) {
            return false;
        }

        if (source.getEntity() == owner || source.getDirectEntity() == owner) {
            return true;
        }

        var direct = source.getDirectEntity();
        return direct instanceof Projectile projectile && projectile.getOwner() == owner;
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        handleRemoval(true, true);
        super.die(damageSource);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity entity) {
        var owner = getOwner();
        if (entity == this || entity == owner) {
            return true;
        }
        return owner != null && owner.isAlliedTo(entity);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public int getContainerSize() {
        return jp.aquafactory.apprenticecodex.capability.companiontrunkinventory.CompanionTrunkInventory.SIZE;
    }

    @Override
    public boolean isEmpty() {
        var storage = getStorage();
        return storage == null || storage.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        var handler = getHandler();
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return handler.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        var handler = getHandler();
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        return handler.extractItem(slot, amount, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        var handler = getHandler();
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }

        var stack = handler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        handler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        var handler = getHandler();
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return;
        }
        handler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        var storage = getStorage();
        if (storage != null) {
            storage.clearItems();
        }
    }

    @Override
    public void startOpen(@NotNull Player player) {
        if (level().isClientSide) {
            return;
        }

        var wasEmpty = openers.isEmpty();
        openers.add(player.getUUID());
        if (wasEmpty && !openers.isEmpty()) {
            startLidOpenAnimation();
            level().playSound(null, blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.75f, 1.0f);
        }
    }

    @Override
    public void stopOpen(@NotNull Player player) {
        if (level().isClientSide) {
            return;
        }

        var wasOpen = !openers.isEmpty();
        openers.remove(player.getUUID());
        if (wasOpen && openers.isEmpty()) {
            startLidCloseAnimation();
            level().playSound(null, blockPosition(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.75f, 1.0f);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        ownerUuid = compoundTag.hasUUID("OwnerUUID") ? compoundTag.getUUID("OwnerUUID") : null;
        wasOnGround = compoundTag.getBoolean("WasOnGround");
        jumpCooldownTick = compoundTag.getInt("JumpCooldownTick");
        lidAnimationTick = compoundTag.getInt("LidAnimationTick");
        bodyAnimationTick = compoundTag.getInt("BodyAnimationTick");
        setLidAnimationState(LidAnimationState.of(compoundTag.getInt("LidAnimationState")), false);
        setBodyAnimationState(BodyAnimationState.of(compoundTag.getInt("BodyAnimationState")), false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (ownerUuid != null) {
            compoundTag.putUUID("OwnerUUID", ownerUuid);
        }
        compoundTag.putBoolean("WasOnGround", wasOnGround);
        compoundTag.putInt("JumpCooldownTick", jumpCooldownTick);
        compoundTag.putInt("LidAnimationTick", lidAnimationTick);
        compoundTag.putInt("BodyAnimationTick", bodyAnimationTick);
        compoundTag.putInt("LidAnimationState", entityData.get(LID_ANIMATION_STATE));
        compoundTag.putInt("BodyAnimationState", entityData.get(BODY_ANIMATION_STATE));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
                new AnimationController<>(this, "lid", state -> {
                    syncControllerReplay(state.getController(), true);

                    switch (LidAnimationState.of(entityData.get(LID_ANIMATION_STATE))) {
                        case JUMP_MAIN -> state.getController().setAnimation(ANIM_JUMP_MAIN);
                        case OPENING, OPEN_HELD -> state.getController().setAnimation(ANIM_OPEN);
                        case CLOSING -> state.getController().setAnimation(ANIM_CLOSE);
                        case IDLE -> state.getController().setAnimation(ANIM_IDLE);
                    }
                    return PlayState.CONTINUE;
                })
        );
        controllerRegistrar.add(
                new AnimationController<>(this, "body", state -> {
                    syncControllerReplay(state.getController(), false);

                    switch (BodyAnimationState.of(entityData.get(BODY_ANIMATION_STATE))) {
                        case JUMP_HELD -> state.getController().setAnimation(ANIM_JUMP_BODY);
                        case LANDING -> state.getController().setAnimation(ANIM_LAND_BODY);
                        case IDLE -> state.getController().setAnimation(ANIM_IDLE);
                    }
                    return PlayState.CONTINUE;
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void syncControllerReplay(AnimationController<CompanionTrunkEntity> controller, boolean lidController) {
        // GeckoLib は同じ RawAnimation を再指定しても再読込しないため、再生要求ごとに serial を進めて明示的にリセットする.
        if (!level().isClientSide) {
            return;
        }

        if (lidController) {
            var serial = entityData.get(LID_ANIMATION_SERIAL);
            if (serial != clientLastLidAnimationSerial) {
                clientLastLidAnimationSerial = serial;
                controller.forceAnimationReset();
            }
            return;
        }

        var serial = entityData.get(BODY_ANIMATION_SERIAL);
        if (serial != clientLastBodyAnimationSerial) {
            clientLastBodyAnimationSerial = serial;
            controller.forceAnimationReset();
        }
    }

    private void playLidJumpAnimation() {
        lidAnimationTick = JUMP_MAIN_DURATION_TICK;
        setLidAnimationState(LidAnimationState.JUMP_MAIN, true);
    }

    private void startLidOpenAnimation() {
        lidAnimationTick = OPEN_DURATION_TICK;
        setLidAnimationState(LidAnimationState.OPENING, true);
    }

    private void startLidCloseAnimation() {
        lidAnimationTick = CLOSE_DURATION_TICK;
        setLidAnimationState(LidAnimationState.CLOSING, true);
    }

    private void finishLidAnimationPhase() {
        switch (LidAnimationState.of(entityData.get(LID_ANIMATION_STATE))) {
            case JUMP_MAIN -> setLidAnimationState(openers.isEmpty() ? LidAnimationState.IDLE : LidAnimationState.OPEN_HELD, false);
            case OPENING -> setLidAnimationState(openers.isEmpty() ? LidAnimationState.IDLE : LidAnimationState.OPEN_HELD, false);
            case CLOSING -> setLidAnimationState(openers.isEmpty() ? LidAnimationState.IDLE : LidAnimationState.OPEN_HELD, false);
            case OPEN_HELD, IDLE -> {
            }
        }
    }

    private void startBodyJumpAnimation() {
        bodyAnimationTick = 0;
        setBodyAnimationState(BodyAnimationState.JUMP_HELD, true);
    }

    private void startBodyLandAnimation() {
        bodyAnimationTick = LAND_DURATION_TICK;
        setBodyAnimationState(BodyAnimationState.LANDING, true);
    }

    private void finishBodyAnimationPhase() {
        if (BodyAnimationState.of(entityData.get(BODY_ANIMATION_STATE)) == BodyAnimationState.LANDING) {
            setBodyAnimationState(BodyAnimationState.IDLE, false);
        }
    }

    private void setLidAnimationState(LidAnimationState state, boolean replay) {
        entityData.set(LID_ANIMATION_STATE, state.id);
        if (replay) {
            entityData.set(LID_ANIMATION_SERIAL, entityData.get(LID_ANIMATION_SERIAL) + 1);
        }
    }

    private void setBodyAnimationState(BodyAnimationState state, boolean replay) {
        entityData.set(BODY_ANIMATION_STATE, state.id);
        if (replay) {
            entityData.set(BODY_ANIMATION_SERIAL, entityData.get(BODY_ANIMATION_SERIAL) + 1);
        }
    }

    private enum LidAnimationState {
        IDLE(0),
        JUMP_MAIN(1),
        OPENING(2),
        OPEN_HELD(3),
        CLOSING(4);

        private final int id;

        LidAnimationState(int id) {
            this.id = id;
        }

        private static LidAnimationState of(int rawId) {
            return switch (rawId) {
                case 1 -> JUMP_MAIN;
                case 2 -> OPENING;
                case 3 -> OPEN_HELD;
                case 4 -> CLOSING;
                default -> IDLE;
            };
        }
    }

    private enum BodyAnimationState {
        IDLE(0),
        JUMP_HELD(1),
        LANDING(2);

        private final int id;

        BodyAnimationState(int id) {
            this.id = id;
        }

        private static BodyAnimationState of(int rawId) {
            return switch (rawId) {
                case 1 -> JUMP_HELD;
                case 2 -> LANDING;
                default -> IDLE;
            };
        }
    }
}
