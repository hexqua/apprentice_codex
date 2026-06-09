package jp.aquafactory.apprenticecodex.spell.grindrunner;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.recipe.grindrunner.GrindRunnerRecipe;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.ProcessingRecipeDenylist;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class GrindRunnerWheelEntity extends SummonWeaponEntity implements GeoEntity {

    private enum WheelState {
        DROPPING,
        STANDBY,
        LAUNCHED
    }

    private static final double LOOK_TARGET_RANGE = 64.0;
    private static final double LOOK_TARGET_HITBOX_WIDTH = 0.5;
    private static final double LOOK_UPDATE_SUPPRESS_DISTANCE_SQR = 1.0;
    private static final double LAUNCH_GRAVITY = 0.08;
    private static final int LAUNCH_SLOWDOWN_TICKS = 10;
    private static final int STOP_RESIDUAL_TICKS = 10;
    private static final double STOP_SPEED_THRESHOLD_SQR = 0.01;
    private static final double STOP_VERTICAL_SPEED_THRESHOLD = 0.1;
    private static final int DAMAGE_INTERVAL_TICK = 2;
    private static final int ITEM_PROCESS_INTERVAL_TICKS = 10;
    private static final double ITEM_PROCESS_RADIUS = 1.0;
    private static final double DAMAGE_AXIS_RANGE = 1.1;
    private static final double DAMAGE_SIDE_RADIUS = 0.3;
    private static final double DAMAGE_SAMPLE_STEP = 0.2;
    private static final float LAUNCH_MAX_DAMAGE_MULTIPLIER = 3.0f;
    private static final double LAUNCH_START_GROUND_EPSILON = 1.0E-3;
    private static final float MAX_YAW_TURN_PER_TICK_DEG = 30.0f;
    private static final float NORMAL_ANIMATION_SPEED = 1.0f;
    private static final String CREATE_MOD_ID = "create";
    private static final ResourceLocation CREATE_CRUSHING_RECIPE_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "crushing");
    private static final ResourceLocation CREATE_MILLING_RECIPE_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "milling");
    private static boolean hasLoggedCreateReflectionFailure = false;
    private static final EntityDataAccessor<Float> ANIMATION_SPEED =
            SynchedEntityData.defineId(GrindRunnerWheelEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation GRIND = RawAnimation.begin().thenLoop("grind");

    private @Nullable Vec3 summonGroundPosition;
    private @Nullable Vec3 dropStartPosition;
    private int dropDurationTick = 1;
    private int dropProgressTick = 0;
    private WheelState state = WheelState.STANDBY;

    private boolean hasAimInitialized = false;
    private boolean wasAimUpdateSuppressed = false;

    private double launchSpeed = 1.2;
    private int launchSustainTicks = 0;
    private int launchedTick = 0;
    private int stoppedTick = 0;
    private float damage;
    private float grindItemPerSecond;
    private float pendingItemProcessBudget;
    private final Set<UUID> skipProcessingItemIds = new HashSet<>();
    private Vec3 lastLaunchDirection = new Vec3(0, 0, 1);

    public GrindRunnerWheelEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public GrindRunnerWheelEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANIMATION_SPEED, NORMAL_ANIMATION_SPEED);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        launchSpeed = pCompound.getDouble("LaunchSpeed");
        launchSustainTicks = pCompound.getInt("LaunchSustainTicks");
        grindItemPerSecond = pCompound.getFloat("GrindItemPerSecond");
        pendingItemProcessBudget = pCompound.getFloat("PendingItemProcessBudget");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putDouble("LaunchSpeed", launchSpeed);
        pCompound.putInt("LaunchSustainTicks", launchSustainTicks);
        pCompound.putFloat("GrindItemPerSecond", grindItemPerSecond);
        pCompound.putFloat("PendingItemProcessBudget", pendingItemProcessBudget);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                1.5,
                12,
                0.1f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル(再ログインで消えるので制御不要)
        if (level.isClientSide && firstTick) {
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

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (state == WheelState.LAUNCHED) {
            tickLaunched(owner);
            return;
        }

        setAnimationSpeed(NORMAL_ANIMATION_SPEED);

        if (summonGroundPosition == null) {
            summonGroundPosition = owner.position();
        }

        if (state == WheelState.DROPPING) {
            tickDropping();
            // 出現演出中は向き更新を止める.
            return;
        }

        holdSummonPosition();
        updateAimByOwnerLook(owner);
        performEmbeddedDamage(owner);
        processNearbyItems(level);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (summonGroundPosition != null) {
            return summonGroundPosition;
        }

        if (getOwner() instanceof LivingEntity owner) {
            return owner.position();
        }

        return position();
    }

    @Override
    public void releaseWeapon() {
        if (state == WheelState.LAUNCHED) {
            return;
        }

        ensureLaunchStartAboveGround();

        var launchDir = flattenDirection(getLookAngle());
        if (launchDir.lengthSqr() < 1.0E-6 && getOwner() instanceof LivingEntity owner) {
            launchDir = flattenDirection(owner.getViewVector(1.0F));
        }
        if (launchDir.lengthSqr() < 1.0E-6) {
            launchDir = new Vec3(0, 0, 1);
        }

        state = WheelState.LAUNCHED;
        launchedTick = 0;
        stoppedTick = 0;
        lastLaunchDirection = launchDir.normalize();
        setNoGravity(false);
        setDeltaMovement(lastLaunchDirection.scale(launchSpeed));
        setAnimationSpeed(NORMAL_ANIMATION_SPEED);
        AudioTools.playSoundFromEntity(level(), this, SoundRegistry.WHEEL_LAUNCH.get(), SoundSource.PLAYERS);
        hasImpulse = true;
    }

    private void ensureLaunchStartAboveGround() {
        var level = level();
        var probePos = BlockPos.containing(getX(), getBoundingBox().minY - 0.01, getZ());
        var collisionShape = level.getBlockState(probePos).getCollisionShape(level, probePos);
        if (collisionShape.isEmpty()) {
            return;
        }

        var collisionTopY = probePos.getY() + collisionShape.max(Direction.Axis.Y);
        var minLaunchY = collisionTopY + LAUNCH_START_GROUND_EPSILON;
        if (getY() >= minLaunchY) {
            return;
        }

        // モブ重なり時などで地表面より低くなっていたら、射出前に補正する.
        setPos(getX(), minLaunchY, getZ());
    }

    public void setSummonSettings(Vec3 summonGroundPosition, double dropHeight, int dropDurationTick) {
        this.summonGroundPosition = summonGroundPosition;
        this.dropStartPosition = summonGroundPosition.add(0, Math.max(0, dropHeight), 0);
        this.dropDurationTick = Math.max(1, dropDurationTick);
        this.dropProgressTick = 0;
        this.state = WheelState.DROPPING;
        this.hasAimInitialized = false;
        this.wasAimUpdateSuppressed = false;
        this.launchedTick = 0;
        this.stoppedTick = 0;
        this.setAnimationSpeed(NORMAL_ANIMATION_SPEED);

        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPos(dropStartPosition.x, dropStartPosition.y, dropStartPosition.z);

        // 初期向きは発動者の yaw を採用する.
        if (getOwner() instanceof LivingEntity owner) {
            var yaw = resolveInitialYaw(owner);
            setYRot(yaw);
            setXRot(0);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
            hasAimInitialized = true;
        }
    }

    public void setLaunchSettings(double launchSpeed) {
        this.launchSpeed = Math.max(0.05, launchSpeed);
    }

    public void setLaunchSustainTicks(int launchSustainTicks) {
        this.launchSustainTicks = Math.max(0, launchSustainTicks);
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0.0f, damage);
    }

    public void setGrindItemPerSecond(float grindItemPerSecond) {
        this.grindItemPerSecond = Math.max(0.0f, grindItemPerSecond);
        pendingItemProcessBudget = 0.0f;
    }

    private void tickDropping() {
        if (dropStartPosition == null || summonGroundPosition == null) {
            state = WheelState.STANDBY;
            return;
        }

        dropProgressTick = Math.min(dropProgressTick + 1, dropDurationTick);
        var t = dropProgressTick / (double) dropDurationTick;
        var nextPos = dropStartPosition.lerp(summonGroundPosition, t);
        setPos(nextPos.x, nextPos.y, nextPos.z);

        if (dropProgressTick >= dropDurationTick) {
            state = WheelState.STANDBY;
            holdSummonPosition();
        }
    }

    private void holdSummonPosition() {
        var standby = getStandbyPosition();
        setPos(standby.x, standby.y, standby.z);
        setDeltaMovement(Vec3.ZERO);
    }

    private void updateAimByOwnerLook(LivingEntity owner) {
        var aimResult = RaycastTools.raycastFromEye(owner, LOOK_TARGET_RANGE, LOOK_TARGET_HITBOX_WIDTH, e -> e != owner);
        var targetPos = aimResult.hitPosition();
        var shouldSuppress = targetPos.distanceToSqr(position()) <= LOOK_UPDATE_SUPPRESS_DISTANCE_SQR;

        if (shouldSuppress) {
            if (!hasAimInitialized) {
                // 初回だけ発動者の水平向きにそろえて、近距離抑制中の姿勢崩れを防ぐ.
                var ownerLook = owner.getViewVector(1.0F);
                var flatLook = new Vec3(ownerLook.x, 0, ownerLook.z);
                if (flatLook.lengthSqr() < 1.0E-6) {
                    flatLook = new Vec3(0, 0, 1);
                }
                applyFacing(flatLook);
                hasAimInitialized = true;
            } else if (!wasAimUpdateSuppressed) {
                // 抑制に入る瞬間にだけピッチを戻して、地面めり込み見えを防ぐ.
                setXRot(0);
                setRot(getYRot(), getXRot());
                hasImpulse = true;
            }

            wasAimUpdateSuppressed = true;
            return;
        }

        applyFacing(targetPos.subtract(position()));
        hasAimInitialized = true;
        wasAimUpdateSuppressed = false;
    }

    private void tickLaunched(LivingEntity owner) {
        launchedTick++;
        var targetHorizontalSpeed = resolveHorizontalSpeedForTick(launchedTick);
        var launchDirection = resolveLaunchDirection();
        var velocityY = getDeltaMovement().y - LAUNCH_GRAVITY;
        setDeltaMovement(launchDirection.scale(targetHorizontalSpeed).add(0, velocityY, 0));

        move(MoverType.SELF, getDeltaMovement());

        // 1ブロック段差は自前で登坂し、2ブロック相当の壁に当たった時のみ消す.
        if (horizontalCollision) {
            var stepped = tryStepUpOneBlock(getDeltaMovement());
            if (stepped) {
                var moved = getDeltaMovement();
                setDeltaMovement(moved.x, Math.max(0.0, moved.y), moved.z);
            } else if (isLargeObstacleAhead(getDeltaMovement())) {
                discard();
                return;
            }
        }

        if (onGround() && getDeltaMovement().y < 0.0) {
            var moved = getDeltaMovement();
            setDeltaMovement(moved.x, 0.0, moved.z);
        }

        if (targetHorizontalSpeed <= 0.0
                && onGround()
                && getDeltaMovement().horizontalDistanceSqr() <= STOP_SPEED_THRESHOLD_SQR
                && Math.abs(getDeltaMovement().y) <= STOP_VERTICAL_SPEED_THRESHOLD) {
            stoppedTick++;
            setDeltaMovement(Vec3.ZERO);
            if (stoppedTick >= STOP_RESIDUAL_TICKS) {
                discard();
                return;
            }
        } else {
            stoppedTick = 0;
        }

        updateAnimationSpeedByLaunchSpeed(targetHorizontalSpeed);
        updateRotationByMovement();
        performLaunchDamage(owner, targetHorizontalSpeed);
    }

    private double resolveHorizontalSpeedForTick(int tick) {
        if (tick <= launchSustainTicks) {
            return launchSpeed;
        }

        var slowdownTick = tick - launchSustainTicks;
        if (slowdownTick >= LAUNCH_SLOWDOWN_TICKS) {
            return 0.0;
        }

        var t = Mth.clamp(slowdownTick / (double) LAUNCH_SLOWDOWN_TICKS, 0.0, 1.0);
        return Mth.lerp(t, launchSpeed, 0.0);
    }

    private Vec3 resolveLaunchDirection() {
        var horizontal = flattenDirection(getDeltaMovement());
        if (horizontal.lengthSqr() > 1.0E-6) {
            lastLaunchDirection = horizontal;
            return horizontal;
        }

        if (lastLaunchDirection.lengthSqr() > 1.0E-6) {
            return lastLaunchDirection;
        }

        var fallback = flattenDirection(getLookAngle());
        if (fallback.lengthSqr() > 1.0E-6) {
            lastLaunchDirection = fallback;
            return fallback;
        }

        return new Vec3(0, 0, 1);
    }

    private void updateAnimationSpeedByLaunchSpeed(double horizontalSpeed) {
        if (launchSpeed <= 1.0E-6) {
            setAnimationSpeed(0.0f);
            return;
        }

        var speedRatio = Mth.clamp(horizontalSpeed / launchSpeed, 0.0, 1.0);
        setAnimationSpeed((float) (NORMAL_ANIMATION_SPEED * speedRatio));
    }

    private void setAnimationSpeed(float speed) {
        if (Math.abs(entityData.get(ANIMATION_SPEED) - speed) <= 1.0E-4f) {
            return;
        }
        entityData.set(ANIMATION_SPEED, speed);
    }

    private void applyFacing(Vec3 direction) {
        var flat = flattenDirection(direction);
        if (flat.lengthSqr() < 1.0E-6) {
            return;
        }

        var yawPitch = RotationTools.calculateYawPitchByDirection(flat);
        rotateYawWithLimit(yawPitch.yaw());
    }

    private boolean tryStepUpOneBlock(Vec3 velocity) {
        var horizontal = flattenDirection(velocity);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return false;
        }

        var level = level();
        var ahead = position().add(horizontal.scale(Math.max(getBbWidth(), 0.5)));
        var feetY = getBoundingBox().minY + 0.01;
        var frontPos = BlockPos.containing(ahead.x, feetY, ahead.z);
        if (!isSolidCollision(level, frontPos)) {
            return false;
        }

        var upperPos = frontPos.above();
        if (isSolidCollision(level, upperPos)) {
            return false;
        }

        var forward = horizontal.scale(0.2);
        var stepY = frontPos.getY() + 1.0 + 0.001;
        setPos(getX() + forward.x, Math.max(getY(), stepY), getZ() + forward.z);
        return true;
    }

    private boolean isLargeObstacleAhead(Vec3 velocity) {
        var horizontal = flattenDirection(velocity);
        if (horizontal.lengthSqr() < 1.0E-6) {
            return false;
        }

        var level = level();
        var ahead = position().add(horizontal.scale(Math.max(getBbWidth(), 0.5)));
        var feetY = getBoundingBox().minY + 0.01;
        var frontPos = BlockPos.containing(ahead.x, feetY, ahead.z);
        return isSolidCollision(level, frontPos) && isSolidCollision(level, frontPos.above());
    }

    private static boolean isSolidCollision(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private void updateRotationByMovement() {
        var horizontal = flattenDirection(getDeltaMovement());
        if (horizontal.lengthSqr() < 1.0E-6) {
            return;
        }

        var yawPitch = RotationTools.calculateYawPitchByDirection(horizontal);
        rotateYawWithLimit(yawPitch.yaw());
    }

    private void rotateYawWithLimit(float targetYaw) {
        setYRot(Mth.approachDegrees(getYRot(), targetYaw, MAX_YAW_TURN_PER_TICK_DEG));
        setXRot(0);
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private static Vec3 flattenDirection(Vec3 direction) {
        var flat = new Vec3(direction.x, 0, direction.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        return flat.normalize();
    }

    private static float resolveInitialYaw(LivingEntity owner) {
        var yaw = owner.getYRot();
        if (Float.isFinite(yaw)) {
            return yaw;
        }

        // 極端な姿勢時のフォールバック.
        if (Float.isFinite(owner.yBodyRot)) {
            return owner.yBodyRot;
        }
        return 0.0f;
    }

    private void performEmbeddedDamage(LivingEntity owner) {
        performDamage(owner, damage, CombatTools.KnockbackTypes.NO_KNOCKBACK);
    }

    private void processNearbyItems(ServerLevel level) {
        if (tickCount % ITEM_PROCESS_INTERVAL_TICKS != 0) {
            return;
        }

        pendingItemProcessBudget += Math.max(0.0f, grindItemPerSecond / 2f);
        var maxProcessCount = Mth.floor(pendingItemProcessBudget);
        if (maxProcessCount <= 0) {
            return;
        }

        var hits = sampleNearbyProcessTargets(level);
        if (hits.size() > 1) {
            hits.sort(Comparator.comparingDouble(item -> item.position().distanceToSqr(position())));
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

        pendingItemProcessBudget = Math.max(0.0f, pendingItemProcessBudget - processedCount);
    }

    private List<ItemEntity> sampleNearbyProcessTargets(ServerLevel level) {
        var area = new AABB(position(), position()).inflate(ITEM_PROCESS_RADIUS);
        return new ArrayList<>(level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                item -> item.isAlive() && !item.getItem().isEmpty()
        ));
    }

    private int tryProcessItem(ServerLevel level, ItemEntity itemEntity, int maxProcessCount) {
        if (maxProcessCount <= 0 || !itemEntity.isAlive()) {
            return 0;
        }

        var inputStack = itemEntity.getItem();
        if (inputStack.isEmpty() || inputStack.getCount() <= 0) {
            return 0;
        }

        var processCount = Math.min(maxProcessCount, inputStack.getCount());
        if (processCount <= 0) {
            return 0;
        }

        var createProcessResult = tryProcessCreateRecipe(level, itemEntity, inputStack, processCount);
        if (createProcessResult > 0) {
            return createProcessResult;
        }

        var recipe = findProcessingRecipe(level, inputStack);
        if (recipe.isEmpty()) {
            return 0;
        }

        var outputsPerInput = recipe.get().getResultTemplates();
        if (outputsPerInput.isEmpty()) {
            return 0;
        }

        var outputStacks = buildOutputStacks(outputsPerInput, processCount);
        applyProcessingResult(level, itemEntity, outputStacks, processCount);
        return processCount;
    }

    private int tryProcessCreateRecipe(ServerLevel level, ItemEntity itemEntity, ItemStack inputStack, int processCount) {
        for (var recipeTypeId : List.of(CREATE_CRUSHING_RECIPE_TYPE_ID, CREATE_MILLING_RECIPE_TYPE_ID)) {
            var createRecipe = findCreateProcessingRecipe(level, inputStack, recipeTypeId);
            if (createRecipe.isEmpty()) {
                continue;
            }

            // Create 側もレシピ一致だけで加工可否を決め、出力抽選だけ借りる.
            var createOutputs = rollCreateProcessingOutputs(level, createRecipe.get().value(), processCount);
            if (createOutputs.isPresent()) {
                applyProcessingResult(level, itemEntity, createOutputs.get(), processCount);
                return processCount;
            }

            logCreateReflectionFailureOnce(createRecipe.get().id());
        }

        return 0;
    }

    private Optional<GrindRunnerRecipe> findProcessingRecipe(ServerLevel level, ItemStack inputStack) {
        if (inputStack.isEmpty() || inputStack.getCount() <= 0) {
            return Optional.empty();
        }

        var recipeManager = level.getRecipeManager();
        var input = new SingleRecipeInput(inputStack.copyWithCount(1));
        var directMatch = recipeManager.getRecipeFor(RecipeRegistry.GRIND_RUNNER_RECIPE_TYPE.get(), input, level);
        if (directMatch.isPresent() && ProcessingRecipeDenylist.isAllowed(directMatch.get())) {
            return Optional.of(directMatch.get().value());
        }

        return recipeManager.getRecipes().stream()
                .filter(ProcessingRecipeDenylist::isAllowed)
                .map(RecipeHolder::value)
                .filter(GrindRunnerRecipe.class::isInstance)
                .map(GrindRunnerRecipe.class::cast)
                .filter(recipe -> recipe.matches(input, level))
                .findFirst();
    }

    private Optional<RecipeHolder<?>> findCreateProcessingRecipe(ServerLevel level, ItemStack inputStack, ResourceLocation recipeTypeId) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            return Optional.empty();
        }

        RecipeType<?> createRecipeType = BuiltInRegistries.RECIPE_TYPE.getOptional(recipeTypeId).orElse(null);
        if (createRecipeType == null) {
            return Optional.empty();
        }

        return level.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.value().getType() == createRecipeType)
                .filter(recipe -> matchesCreateProcessingInput(recipe.value(), inputStack))
                .findFirst();
    }

    private static boolean matchesCreateProcessingInput(Recipe<?> recipe, ItemStack inputStack) {
        if (inputStack.isEmpty()) {
            return false;
        }

        var ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return false;
        }

        return ingredients.get(0).test(inputStack);
    }

    private Optional<List<ItemStack>> rollCreateProcessingOutputs(ServerLevel level, Recipe<?> createRecipe, int processCount) {
        var outputs = new ArrayList<ItemStack>();
        for (var i = 0; i < processCount; i++) {
            var rolledPerInput = rollCreateProcessingOutputsPerInput(level, createRecipe);
            if (rolledPerInput.isEmpty()) {
                return Optional.empty();
            }
            outputs.addAll(rolledPerInput.get());
        }
        return Optional.of(outputs);
    }

    private Optional<List<ItemStack>> rollCreateProcessingOutputsPerInput(ServerLevel level, Recipe<?> createRecipe) {
        var rolledByRecipe = invokeCreateRecipeRollResults(level, createRecipe);
        if (rolledByRecipe.isPresent()) {
            return rolledByRecipe;
        }

        Object rawRollableResults;
        try {
            rawRollableResults = createRecipe.getClass().getMethod("getRollableResults").invoke(createRecipe);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }

        if (!(rawRollableResults instanceof List<?> rollableResults)) {
            return Optional.empty();
        }

        var rolled = new ArrayList<ItemStack>();
        for (var output : rollableResults) {
            var rolledStack = rollCreateProcessingOutput(level, output);
            if (rolledStack == null) {
                return Optional.empty();
            }
            if (!rolledStack.isEmpty() && rolledStack.getCount() > 0) {
                rolled.add(rolledStack);
            }
        }
        return Optional.of(rolled);
    }

    private Optional<List<ItemStack>> invokeCreateRecipeRollResults(ServerLevel level, Recipe<?> createRecipe) {
        try {
            var method = createRecipe.getClass().getMethod("rollResults");
            var rolled = copyItemStacks(method.invoke(createRecipe));
            return rolled == null ? Optional.empty() : Optional.of(rolled);
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }

        try {
            var method = createRecipe.getClass().getMethod("rollResults", net.minecraft.util.RandomSource.class);
            var rolled = copyItemStacks(method.invoke(createRecipe, level.random));
            return rolled == null ? Optional.empty() : Optional.of(rolled);
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private @Nullable ItemStack rollCreateProcessingOutput(ServerLevel level, Object processingOutput) {
        try {
            var method = processingOutput.getClass().getMethod("rollOutput");
            return copyItemStack(method.invoke(processingOutput));
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        try {
            var method = processingOutput.getClass().getMethod("rollOutput", net.minecraft.util.RandomSource.class);
            return copyItemStack(method.invoke(processingOutput, level.random));
        } catch (NoSuchMethodException ignored) {
            // no-op
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        try {
            var getStackMethod = processingOutput.getClass().getMethod("getStack");
            var getChanceMethod = processingOutput.getClass().getMethod("getChance");
            var stackValue = getStackMethod.invoke(processingOutput);
            var chanceValue = getChanceMethod.invoke(processingOutput);
            if (!(stackValue instanceof ItemStack stack) || stack.isEmpty() || stack.getCount() <= 0) {
                return ItemStack.EMPTY;
            }

            var chance = chanceValue instanceof Number number ? number.floatValue() : 0.0f;
            if (chance >= 1.0f || level.random.nextFloat() < chance) {
                return stack.copy();
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }

        return ItemStack.EMPTY;
    }

    private static @Nullable List<ItemStack> copyItemStacks(Object rawValue) {
        if (!(rawValue instanceof List<?> rawList)) {
            return null;
        }

        var copied = new ArrayList<ItemStack>();
        for (var element : rawList) {
            var copiedStack = copyItemStack(element);
            if (!copiedStack.isEmpty() && copiedStack.getCount() > 0) {
                copied.add(copiedStack);
            }
        }
        return copied;
    }

    private static ItemStack copyItemStack(Object rawValue) {
        if (!(rawValue instanceof ItemStack stack) || stack.isEmpty() || stack.getCount() <= 0) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }

    private static void logCreateReflectionFailureOnce(ResourceLocation recipeId) {
        if (hasLoggedCreateReflectionFailure) {
            return;
        }

        hasLoggedCreateReflectionFailure = true;
        ApprenticeCodex.LOGGER.warn(
                "Create processing integration fallback enabled: reflection failed for recipe {}. " +
                        "GrindRunner will use apprenticecodex recipes for compatibility.",
                recipeId
        );
    }

    private void applyProcessingResult(ServerLevel level, ItemEntity sourceItem, List<ItemStack> outputStacks, int processCount) {
        if (!sourceItem.isAlive() || processCount <= 0) {
            return;
        }

        var sourceStack = sourceItem.getItem();
        if (sourceStack.isEmpty() || sourceStack.getCount() <= 0) {
            return;
        }

        var actualProcessCount = Math.min(processCount, sourceStack.getCount());
        if (actualProcessCount <= 0) {
            return;
        }

        var normalizedOutputStacks = normalizeOutputStacks(outputStacks);

        var sourcePosition = sourceItem.position();
        var sourceVelocity = sourceItem.getDeltaMovement();
        var remainingInputCount = sourceStack.getCount() - actualProcessCount;

        if (remainingInputCount <= 0) {
            if (normalizedOutputStacks.isEmpty()) {
                sourceItem.discard();
                return;
            }

            var firstOutput = normalizedOutputStacks.remove(0);
            sourceItem.setItem(firstOutput);
            skipProcessingItemIds.add(sourceItem.getUUID());
        } else {
            var remain = sourceStack.copy();
            remain.setCount(remainingInputCount);
            sourceItem.setItem(remain);
        }

        for (var outputStack : normalizedOutputStacks) {
            spawnProcessedOutput(level, sourcePosition, sourceVelocity, outputStack);
        }

        playItemProcessedEffects(level, sourcePosition, actualProcessCount);
    }

    private List<ItemStack> buildOutputStacks(List<ItemStack> outputPrototypes, int processCount) {
        var outputStacks = new ArrayList<ItemStack>();
        for (var outputPrototype : outputPrototypes) {
            if (outputPrototype.isEmpty() || outputPrototype.getCount() <= 0) {
                continue;
            }

            var outputCountLong = (long) outputPrototype.getCount() * processCount;
            if (outputCountLong <= 0) {
                continue;
            }

            var outputCount = (int) Math.min(Integer.MAX_VALUE, outputCountLong);
            outputStacks.addAll(splitOutputStacks(outputPrototype, outputCount));
        }
        return outputStacks;
    }

    private List<ItemStack> normalizeOutputStacks(List<ItemStack> rawOutputStacks) {
        var normalized = new ArrayList<ItemStack>();
        for (var outputStack : rawOutputStacks) {
            if (outputStack.isEmpty() || outputStack.getCount() <= 0) {
                continue;
            }
            normalized.addAll(splitOutputStacks(outputStack, outputStack.getCount()));
        }
        return normalized;
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

    private void spawnProcessedOutput(ServerLevel level, Vec3 sourcePosition, Vec3 sourceVelocity, ItemStack outputStack) {
        if (outputStack.isEmpty()) {
            return;
        }

        var spawned = new ItemEntity(level, sourcePosition.x, sourcePosition.y, sourcePosition.z, outputStack);
        spawned.setDeltaMovement(sourceVelocity);
        level.addFreshEntity(spawned);
        skipProcessingItemIds.add(spawned.getUUID());
    }

    private void playItemProcessedEffects(ServerLevel level, Vec3 sourcePosition, int processCount) {
        var particleCount = Mth.clamp(4 + processCount * 2, 6, 24);
        AudioTools.playSoundFromPosition(level, sourcePosition, SoundRegistry.WHEEL_PROCESS.get(), SoundSource.NEUTRAL, 0.6f, 1.0f, 0.15f);
        level.sendParticles(
                ParticleTypes.CRIT,
                sourcePosition.x,
                sourcePosition.y + 0.1,
                sourcePosition.z,
                particleCount,
                0.1,
                0.05,
                0.1,
                0.01
        );
    }

    private void performLaunchDamage(LivingEntity owner, double horizontalSpeed) {
        if (launchSpeed <= 1.0E-6) {
            return;
        }

        var speedRatio = Mth.clamp(horizontalSpeed / launchSpeed, 0.0, 1.0);
        var multiplier = (float) (speedRatio * LAUNCH_MAX_DAMAGE_MULTIPLIER);
        performDamage(owner, damage * multiplier, CombatTools.KnockbackTypes.DEFAULT);
    }

    private void performDamage(LivingEntity owner, float damageAmount, CombatTools.KnockbackTypes knockbackType) {
        if (damageAmount <= 0.0f || tickCount % DAMAGE_INTERVAL_TICK != 0) {
            return;
        }

        var level = level();
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.GRIND_RUNNER);
        var includeOwner = state == WheelState.LAUNCHED;
        for (var target : resolveDamageTargets(owner, includeOwner)) {
            CombatTools.applyDamage(target, damageAmount, source, SpellRegistry.GRIND_RUNNER.get().getSchoolType(), knockbackType);
        }
    }

    private LinkedHashSet<LivingEntity> resolveDamageTargets(LivingEntity owner, boolean includeOwner) {
        var axis = resolveDamageAxis(owner);
        var start = position().subtract(axis.scale(DAMAGE_AXIS_RANGE));
        var end = position().add(axis.scale(DAMAGE_AXIS_RANGE));
        var hits = RaycastTools.sampleBeamHits(
                level(),
                start,
                end,
                DAMAGE_SIDE_RADIUS,
                DAMAGE_SAMPLE_STEP,
                e -> (includeOwner && e == owner) || CombatTools.isValidCombatTarget(e, owner)
        );

        var targets = new LinkedHashSet<LivingEntity>();
        for (var hit : hits) {
            if (!(hit instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
                continue;
            }

            if (livingTarget == owner && !includeOwner) {
                continue;
            }

            if (livingTarget == owner || CombatTools.isValidCombatTarget(livingTarget, owner)) {
                targets.add(livingTarget);
            }
        }
        return targets;
    }

    private Vec3 resolveDamageAxis(LivingEntity owner) {
        var movementAxis = flattenDirection(getDeltaMovement());
        if (movementAxis.lengthSqr() > 1.0E-6) {
            return movementAxis;
        }

        var facingAxis = flattenDirection(getLookAngle());
        if (facingAxis.lengthSqr() > 1.0E-6) {
            return facingAxis;
        }

        var ownerAxis = flattenDirection(owner.getViewVector(1.0F));
        if (ownerAxis.lengthSqr() > 1.0E-6) {
            return ownerAxis;
        }

        return new Vec3(0, 0, 1);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(GRIND);
                    state.getController().setAnimationSpeed(entityData.get(ANIMATION_SPEED));
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
