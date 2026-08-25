package jp.aquafactory.apprenticecodex.spell.automagnet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.compat.botania.BotaniaSolegnoliaCompatBridge;
import jp.aquafactory.apprenticecodex.block.magneticstabilityanchor.MagneticStabilityAnchorProtection;
import jp.aquafactory.apprenticecodex.entity.PersistentSummonWeaponEntity;
import jp.aquafactory.apprenticecodex.mixin.ItemEntityAccessor;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class AutoMagnetFamiliarEntity extends PersistentSummonWeaponEntity implements GeoEntity, AntiMagicSusceptible {
    private static final EntityDataAccessor<Integer> DATA_COLLECTION_MODE =
            SynchedEntityData.defineId(AutoMagnetFamiliarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_COLLECTION_BLOCKED =
            SynchedEntityData.defineId(AutoMagnetFamiliarEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double ORBIT_RADIUS = 1.4;
    private static final double ORBIT_HEIGHT = 1.2;
    private static final double ORBIT_SPEED = Math.PI / 45.0;
    private static final double FLOAT_SPEED = Math.PI / 20.0;
    private static final double FLOAT_RANGE = 0.15;
    private static final int COLLECT_INTERVAL_TICK = 2;
    private static final double MIN_PICKUP_RANGE = 0.5;
    // 既存ワールドとの互換のため tag 名は据え置きつつ、転送済み判定にも使う。
    private static final String ITEM_TRANSFER_MARKED_TAG = "apprenticecodex_auto_magnet_transfer_sound_played";

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private double orbitOffset;
    private double pickupRange;
    private double collectMana;

    public AutoMagnetFamiliarEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
        noPhysics = true;
    }

    public AutoMagnetFamiliarEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner, double pickupRange, double collectMana) {
        this(pEntityType, pLevel, owner, pickupRange, collectMana, AutoMagnetCollectionMode.NORMAL);
    }

    public AutoMagnetFamiliarEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner,
                                    double pickupRange, double collectMana, AutoMagnetCollectionMode collectionMode) {
        super(pEntityType, pLevel, owner);
        setNoGravity(true);
        noPhysics = true;
        orbitOffset = pLevel.random.nextDouble() * (Math.PI * 2.0);
        this.pickupRange = Math.max(MIN_PICKUP_RANGE, pickupRange);
        this.collectMana = Math.max(0.0, collectMana);
        setCollectionMode(collectionMode);
        setStandbyPosition(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLLECTION_MODE, AutoMagnetCollectionMode.NORMAL.id());
        builder.define(DATA_COLLECTION_BLOCKED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        orbitOffset = pCompound.contains("OrbitOffset") ? pCompound.getDouble("OrbitOffset") : 0.0;
        pickupRange = pCompound.contains("PickupRange") ? pCompound.getDouble("PickupRange") : MIN_PICKUP_RANGE;
        collectMana = pCompound.contains("CollectMana") ? pCompound.getDouble("CollectMana") : 0.0;
        setCollectionMode(pCompound.contains("CollectionMode")
                ? AutoMagnetCollectionMode.byName(pCompound.getString("CollectionMode"))
                : AutoMagnetCollectionMode.NORMAL);
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putDouble("OrbitOffset", orbitOffset);
        pCompound.putDouble("PickupRange", pickupRange);
        pCompound.putDouble("CollectMana", collectMana);
        pCompound.putString("CollectionMode", getCollectionMode().name());
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive()) {
            discard();
            return;
        }

        if (owner.level() != level) {
            // 次元移動直後は再召喚側で復元する.
            discard();
            return;
        }

        noPhysics = true;
        var target = calculateOrbitPosition(owner);
        followTargetPosition(target);
        setXRot(0.0f);
        setYRot((float) (-Math.toDegrees(getOrbitAngle()) + 90.0));
        setRot(getYRot(), getXRot());
        hasImpulse = true;

        var canCollect = getCollectionMode().canCollect(owner.isCrouching());
        setCollectionBlocked(!canCollect);
        if (canCollect && tickCount % COLLECT_INTERVAL_TICK == 0) {
            collectNearbyDrops(level, owner);
        }
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            var height = owner.getBbHeight() * 0.6 + ORBIT_HEIGHT;
            return owner.position().add(
                    Math.cos(orbitOffset) * ORBIT_RADIUS,
                    height,
                    Math.sin(orbitOffset) * ORBIT_RADIUS
            );
        }
        return Vec3.ZERO;
    }

    @Override
    public boolean canBeCollidedWith() {
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(@NotNull Entity pEntity) {
        // 当たり判定を持たせないため押し返し処理は無効化.
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    public boolean isAlwaysTicking() {
        // 所有者の長距離 teleport で元チャンクが非表示になっても、管理対象から外れて再召喚されないようにする。
        // 常にプレイヤーの周囲を動くタイプのため、これが負荷要因になることは無い(増殖する方が負荷になりうる)
        return true;
    }

    @Override
    public boolean shouldBeSaved() {
        // 実体は capability から再構築するため、ワールド保存に残して次回ログイン時の重複源にしない。
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved()) {
            return;
        }

        if (getOwner() instanceof ServerPlayer owner) {
            AutoMagnetFamiliarManager.deactivate(owner);
            return;
        }

        discard();
    }

    public double getPickupRange() {
        return pickupRange;
    }

    public void configureCollection(double pickupRange, double collectMana, AutoMagnetCollectionMode collectionMode) {
        this.pickupRange = Math.max(MIN_PICKUP_RANGE, pickupRange);
        this.collectMana = Math.max(0.0, collectMana);
        setCollectionMode(collectionMode);
    }

    public AutoMagnetCollectionMode getCollectionMode() {
        return AutoMagnetCollectionMode.byId(entityData.get(DATA_COLLECTION_MODE));
    }

    private void setCollectionMode(AutoMagnetCollectionMode collectionMode) {
        entityData.set(DATA_COLLECTION_MODE, collectionMode.id());
    }

    public boolean isCollectionBlocked() {
        return entityData.get(DATA_COLLECTION_BLOCKED);
    }

    private void setCollectionBlocked(boolean blocked) {
        if (entityData.get(DATA_COLLECTION_BLOCKED) != blocked) {
            entityData.set(DATA_COLLECTION_BLOCKED, blocked);
        }
    }

    private Vec3 calculateOrbitPosition(LivingEntity owner) {
        var angle = getOrbitAngle();
        var floatOffset = Math.sin(tickCount * FLOAT_SPEED + orbitOffset) * FLOAT_RANGE;
        var height = owner.getBbHeight() * 0.6 + ORBIT_HEIGHT + floatOffset;
        return owner.position().add(
                Math.cos(angle) * ORBIT_RADIUS,
                height,
                Math.sin(angle) * ORBIT_RADIUS
        );
    }

    private double getOrbitAngle() {
        return orbitOffset + tickCount * ORBIT_SPEED;
    }

    private void collectNearbyDrops(ServerLevel level, LivingEntity owner) {
        if (pickupRange <= 0.0) {
            return;
        }

        var ownerPos = owner.position();
        var ownerFeet = new Vec3(owner.getX(), owner.getY(), owner.getZ());
        var rangeSq = pickupRange * pickupRange;
        var area = owner.getBoundingBox().inflate(pickupRange);

        var consumedCollectMana = false;
        for (var item : level.getEntitiesOfClass(ItemEntity.class, area, e -> canCollectItem(e, owner, ownerPos, rangeSq))) {
            if (!consumedCollectMana && !isAlreadyTransferredByAutoMagnet(item)) {
                // 同一 tick にまとめて回収できる item 群は 1 回分だけ課金する。
                if (!tryConsumeCollectMana(owner)) {
                    break;
                }
                consumedCollectMana = true;
            }
            moveEntityToOwnerFeet(item, ownerFeet);
            item.setNoPickUpDelay();
            playItemTransferSoundOnce(level, item);
        }

        for (var orb : level.getEntitiesOfClass(ExperienceOrb.class, area, e -> canCollectOrb(e, ownerPos, rangeSq))) {
            moveEntityToOwnerFeet(orb, ownerFeet);
        }
    }

    private static boolean canCollectItem(ItemEntity item, LivingEntity owner, Vec3 ownerPos, double rangeSq) {
        if (item.isRemoved()) {
            return false;
        }
        if (item.position().distanceToSqr(ownerPos) > rangeSq) {
            return false;
        }
        if (MagneticStabilityAnchorProtection.preventsItemCollection(item)) {
            return false;
        }
        if (BotaniaSolegnoliaCompatBridge.preventsAutoMagnetItemCollection(owner, item)) {
            return false;
        }
        if (isRecentOwnerDrop(item, owner)) {
            return false;
        }
        if (owner instanceof Player player) {
            return canFitInNormalInventory(player, item.getItem());
        }
        return true;
    }

    private static boolean canCollectOrb(ExperienceOrb orb, Vec3 ownerPos, double rangeSq) {
        return !orb.isRemoved() && orb.position().distanceToSqr(ownerPos) <= rangeSq;
    }

    private static boolean isRecentOwnerDrop(ItemEntity item, LivingEntity owner) {
        if (!item.hasPickUpDelay()) {
            return false;
        }

        var thrower = getThrowerUuid(item);
        return thrower != null && thrower.equals(owner.getUUID());
    }

    private static boolean canFitInNormalInventory(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 拡張インベントリは考慮しない(際限がないため)
        for (var inventoryStack : player.getInventory().items) {
            if (inventoryStack.isEmpty()) {
                return true;
            }
            if (!ItemStack.isSameItemSameComponents(inventoryStack, stack)) {
                continue;
            }
            if (inventoryStack.getCount() < inventoryStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean tryConsumeCollectMana(LivingEntity owner) {
        if (collectMana <= 0.0) {
            return true;
        }

        var magicData = MagicData.getPlayerMagicData(owner);
        if (magicData == null || magicData.getMana() < collectMana) {
            return false;
        }

        magicData.setMana(Math.max(0f, magicData.getMana() - (float) collectMana));
        return true;
    }

    private static void moveEntityToOwnerFeet(Entity target, Vec3 ownerFeet) {
        target.setPos(ownerFeet.x, ownerFeet.y, ownerFeet.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
    }

    private static UUID getThrowerUuid(ItemEntity item) {
        return ((ItemEntityAccessor) item).apprenticecodex$getThrower();
    }

    private static boolean isAlreadyTransferredByAutoMagnet(ItemEntity item) {
        return item.getPersistentData().getBoolean(ITEM_TRANSFER_MARKED_TAG);
    }

    private void playItemTransferSoundOnce(ServerLevel level, ItemEntity item) {
        var persistentData = item.getPersistentData();
        if (persistentData.getBoolean(ITEM_TRANSFER_MARKED_TAG)) {
            return;
        }
        persistentData.putBoolean(ITEM_TRANSFER_MARKED_TAG, true);
        AudioTools.playSoundFromEntity(level, this, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.4f);
    }
}
