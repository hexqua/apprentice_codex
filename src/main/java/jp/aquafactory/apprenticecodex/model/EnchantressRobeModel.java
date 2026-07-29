package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

public class EnchantressRobeModel<T extends GeoItem> extends GeoModel<T> {
    private static final String HAT_ACCESSORY_BONE = "hat_acc";
    private static final String CAPE_ROOT_BONE = "cape_root";
    private static final String CAPE_MID_BONE = "came_mid";
    private static final String CAPE_TIP_BONE = "came_tip";

    private static final float HAT_ACCESSORY_MAX_Z_SWING = 30.0f * Mth.DEG_TO_RAD;
    private static final float HAT_ACCESSORY_HEAD_TURN_IMPULSE = 1.85f;
    private static final float HAT_ACCESSORY_Z_RESTORE_FORCE = 0.19f;
    private static final float HAT_ACCESSORY_Z_DAMPING = 0.82f;
    private static final float HAT_ACCESSORY_Z_IDLE_SWING = 3.0f * Mth.DEG_TO_RAD;
    private static final float HAT_ACCESSORY_Y_IDLE_SWING = 7.5f * Mth.DEG_TO_RAD;
    private static final float HAT_ACCESSORY_Y_SECONDARY_SWING = 2.5f * Mth.DEG_TO_RAD;
    private static final float HAT_ACCESSORY_X_MOVE_SWING = 4.5f * Mth.DEG_TO_RAD;
    private static final float HAT_ACCESSORY_X_REBOUND_SWING = 2.2f * Mth.DEG_TO_RAD;
    private static final float HAT_ACCESSORY_STATE_RESET_TICKS = 5.0f;
    private static final float HAT_ACCESSORY_STATE_EXPIRE_TICKS = 80.0f;

    private static final float CAPE_BASE_SWING_X = 3.5f * Mth.DEG_TO_RAD;
    private static final float CAPE_MOVE_SWING_X = 22.0f * Mth.DEG_TO_RAD;
    private static final float CAPE_FALL_SWING_X = 12.0f * Mth.DEG_TO_RAD;
    private static final float CAPE_RISE_SWING_X = 7.0f * Mth.DEG_TO_RAD;
    private static final float CAPE_CROUCH_SWING_X = 8.0f * Mth.DEG_TO_RAD;
    private static final float CAPE_GLIDE_SWING_X = 10.0f * Mth.DEG_TO_RAD;
    private static final float CAPE_IDLE_SWING_X = 1.2f * Mth.DEG_TO_RAD;
    private static final float CAPE_IDLE_SWING_Z = 0.8f * Mth.DEG_TO_RAD;
    private static final float CAPE_MOVE_SWING_Z = 6.0f * Mth.DEG_TO_RAD;
    private static final float CAPE_MOVE_TWIST_Y = 3.0f * Mth.DEG_TO_RAD;

    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;
    private final Map<Long, HatAccessorySwingState> hatAccessorySwingStates = new HashMap<>();

    public EnchantressRobeModel() {
        this(
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/enchantress_robe.geo.json"),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/enchantress_robe.png"),
                ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/enchantress_robe.animation.json")
        );
    }

    protected EnchantressRobeModel(ResourceLocation model, ResourceLocation texture, ResourceLocation animation) {
        this.model = model;
        this.texture = texture;
        this.animation = animation;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animation;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId,
                                    AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        applyHatAccessoryAnimation(instanceId, animationState);
        applyCapeAnimation(animatable, instanceId, animationState);
    }

    private void applyHatAccessoryAnimation(long instanceId, AnimationState<T> animationState) {
        var hatAccessory = getBone(HAT_ACCESSORY_BONE).orElse(null);
        if (hatAccessory == null) {
            return;
        }

        resetToInitialTransform(hatAccessory);

        var slot = animationState.getData(DataTickets.EQUIPMENT_SLOT);
        var entity = animationState.getData(DataTickets.ENTITY);
        if (slot != EquipmentSlot.HEAD || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = (tickData == null ? 0.0f : tickData.floatValue()) + animationState.getPartialTick();
        float phaseOffset = (instanceId & 15L) * 0.37f;
        float moveAmount = Mth.clamp((float) livingEntity.getDeltaMovement().horizontalDistance() * 3.0f, 0.0f, 1.0f);
        var swingState = hatAccessorySwingStates.computeIfAbsent(instanceId, key -> new HatAccessorySwingState());
        float deltaTick = updateHatAccessorySwingState(swingState, livingEntity, tick, animationState.getPartialTick());

        float idleWave = Mth.sin(tick * 0.085f + phaseOffset);
        float idleWaveSecondary = Mth.sin(tick * 0.043f + phaseOffset + 1.2f);

        var initial = hatAccessory.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float baseRotZ = initial == null ? 0.0f : initial.getRotZ();

        // 紐で吊られた飾りとして、頭の向き変化に遅れて Z へ大きく揺れ、Y には常時ねじれを入れる。
        float swingZ = Mth.clamp(swingState.zAngle, -HAT_ACCESSORY_MAX_Z_SWING, HAT_ACCESSORY_MAX_Z_SWING)
                + HAT_ACCESSORY_Z_IDLE_SWING * idleWave;
        float twistY = HAT_ACCESSORY_Y_IDLE_SWING * idleWaveSecondary
                + HAT_ACCESSORY_Y_SECONDARY_SWING * Mth.sin(tick * 0.117f + phaseOffset + swingState.zAngle * 1.8f);
        float swingX = moveAmount * HAT_ACCESSORY_X_MOVE_SWING * Mth.sin(tick * 0.19f + phaseOffset * 0.5f)
                + Mth.clamp(-swingState.zVelocity, -1.0f, 1.0f) * HAT_ACCESSORY_X_REBOUND_SWING * Mth.clamp(deltaTick, 0.0f, 1.0f);

        hatAccessory.setRotX(baseRotX + swingX);
        hatAccessory.setRotY(baseRotY + twistY);
        hatAccessory.setRotZ(baseRotZ + swingZ);

        pruneHatAccessorySwingStates(tick);
    }

    private static float updateHatAccessorySwingState(HatAccessorySwingState state, LivingEntity livingEntity, float tick, float partialTick) {
        float headYaw = Mth.rotLerp(partialTick, livingEntity.yHeadRotO, livingEntity.yHeadRot) * Mth.DEG_TO_RAD;

        if (!state.initialized) {
            state.initialized = true;
            state.lastTick = tick;
            state.lastHeadYaw = headYaw;
            state.lastSeenTick = tick;
            state.zAngle = 0.0f;
            state.zVelocity = 0.0f;
            return 0.0f;
        }

        float deltaTick = Mth.clamp(tick - state.lastTick, 0.0f, 1.5f);
        if (tick - state.lastTick > HAT_ACCESSORY_STATE_RESET_TICKS) {
            state.zAngle = 0.0f;
            state.zVelocity = 0.0f;
            deltaTick = 0.0f;
        }

        float simulationStep = Math.max(deltaTick, 0.05f);
        float headYawDelta = Mth.wrapDegrees((headYaw - state.lastHeadYaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        state.zVelocity -= headYawDelta * HAT_ACCESSORY_HEAD_TURN_IMPULSE;
        state.zVelocity -= state.zAngle * HAT_ACCESSORY_Z_RESTORE_FORCE * simulationStep;
        state.zVelocity *= (float) Math.pow(HAT_ACCESSORY_Z_DAMPING, simulationStep);
        state.zAngle += state.zVelocity * simulationStep;
        state.zAngle = Mth.clamp(state.zAngle, -HAT_ACCESSORY_MAX_Z_SWING, HAT_ACCESSORY_MAX_Z_SWING);

        state.lastTick = tick;
        state.lastHeadYaw = headYaw;
        state.lastSeenTick = tick;
        return deltaTick;
    }

    private void pruneHatAccessorySwingStates(float currentTick) {
        if (hatAccessorySwingStates.size() < 16) {
            return;
        }

        hatAccessorySwingStates.entrySet().removeIf(entry -> currentTick - entry.getValue().lastSeenTick > HAT_ACCESSORY_STATE_EXPIRE_TICKS);
    }

    private void applyCapeAnimation(T animatable, long instanceId,
                                    AnimationState<T> animationState) {
        var capeRoot = getBone(CAPE_ROOT_BONE).orElse(null);
        var capeMid = getBone(CAPE_MID_BONE).orElse(null);
        var capeTip = getBone(CAPE_TIP_BONE).orElse(null);
        if (capeRoot == null || capeMid == null || capeTip == null) {
            return;
        }

        resetToInitialTransform(capeRoot);
        resetToInitialTransform(capeMid);
        resetToInitialTransform(capeTip);
        if (!ApprenticeCodexClientConfig.enableApprenticeMageRobeCapeAnimation()) {
            return;
        }

        var slot = animationState.getData(DataTickets.EQUIPMENT_SLOT);
        var entity = animationState.getData(DataTickets.ENTITY);
        if (slot != EquipmentSlot.CHEST || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = (tickData == null ? 0.0f : tickData.floatValue()) + animationState.getPartialTick();
        float phaseOffset = (instanceId & 15L) * 0.31f;

        float horizontalSpeed = (float) Mth.clamp(livingEntity.getDeltaMovement().horizontalDistance(), 0.0, 0.35);
        float verticalSpeed = (float) Mth.clamp(livingEntity.getDeltaMovement().y, -0.45, 0.35);
        float moveAmount = Mth.clamp(Math.max(horizontalSpeed * 3.25f, animationState.getLimbSwingAmount()), 0.0f, 1.0f);
        float fallingAmount = Mth.clamp(-verticalSpeed * 2.2f, 0.0f, 1.0f);
        float risingAmount = Mth.clamp(verticalSpeed * 2.8f, 0.0f, 1.0f);

        float idleWave = Mth.sin(tick * 0.10f + phaseOffset);
        float idleWaveSecondary = Mth.sin(tick * 0.065f + phaseOffset + 1.3f);
        float strideWave = Mth.sin(animationState.getLimbSwing() * 0.60f + phaseOffset * 0.5f);

        float crouchSwing = livingEntity.isCrouching() ? CAPE_CROUCH_SWING_X : 0.0f;
        float glideSwing = livingEntity.isFallFlying() ? CAPE_GLIDE_SWING_X : 0.0f;
        float swingX = CAPE_BASE_SWING_X
                + moveAmount * CAPE_MOVE_SWING_X
                + fallingAmount * CAPE_FALL_SWING_X
                - risingAmount * CAPE_RISE_SWING_X
                + crouchSwing
                + glideSwing;
        float flutterX = (CAPE_IDLE_SWING_X + moveAmount * 4.0f * Mth.DEG_TO_RAD)
                * (idleWave * 0.65f + strideWave * 0.35f);
        float swingZ = (CAPE_IDLE_SWING_Z + moveAmount * CAPE_MOVE_SWING_Z)
                * (strideWave * 0.75f + idleWaveSecondary * 0.25f);
        float twistY = (0.7f * Mth.DEG_TO_RAD + moveAmount * CAPE_MOVE_TWIST_Y) * idleWaveSecondary;

        // 根元より先端の方が遅れて大きく揺れるようにして、布っぽい追従感を作る。
        applyCapeSegment(capeRoot, swingX, flutterX, twistY, swingZ, 0.42f, 0.35f, 0.20f, 0.25f);
        applyCapeSegment(capeMid, swingX, flutterX, twistY, swingZ, 0.78f, 0.80f, 0.45f, 0.60f);
        applyCapeSegment(capeTip, swingX, flutterX, twistY, swingZ, 1.10f, 1.25f, 0.70f, 1.00f);
    }

    private static void applyCapeSegment(GeoBone bone, float swingX, float flutterX, float twistY, float swingZ,
                                         float swingXScale, float flutterXScale, float twistYScale, float swingZScale) {
        var initial = bone.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float baseRotZ = initial == null ? 0.0f : initial.getRotZ();

        bone.setRotX(baseRotX - swingX * swingXScale - flutterX * flutterXScale);
        bone.setRotY(baseRotY + twistY * twistYScale);
        bone.setRotZ(baseRotZ + swingZ * swingZScale);
    }

    private static void resetToInitialTransform(GeoBone bone) {
        var initial = bone.getInitialSnapshot();
        if (initial == null) {
            return;
        }

        bone.setRotX(initial.getRotX());
        bone.setRotY(initial.getRotY());
        bone.setRotZ(initial.getRotZ());

        bone.setPosX(initial.getOffsetX());
        bone.setPosY(initial.getOffsetY());
        bone.setPosZ(initial.getOffsetZ());
    }

    private static final class HatAccessorySwingState {
        private float lastTick;
        private float lastSeenTick;
        private float lastHeadYaw;
        private float zAngle;
        private float zVelocity;
        private boolean initialized;
    }
}
