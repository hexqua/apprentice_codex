package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

public class BroomModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final String DECO_BONE = "deco";

    private static final float DECO_MAX_Z_SWING = 30.0F * Mth.DEG_TO_RAD;
    private static final float DECO_TURN_IMPULSE = 1.85F;
    private static final float DECO_Z_RESTORE_FORCE = 0.19F;
    private static final float DECO_Z_DAMPING = 0.82F;
    private static final float DECO_Z_IDLE_SWING = 3.0F * Mth.DEG_TO_RAD;
    private static final float DECO_Y_IDLE_SWING = 7.5F * Mth.DEG_TO_RAD;
    private static final float DECO_Y_SECONDARY_SWING = 2.5F * Mth.DEG_TO_RAD;
    private static final float DECO_X_MOVE_SWING = 4.5F * Mth.DEG_TO_RAD;
    private static final float DECO_X_ITEM_SWING = 2.0F * Mth.DEG_TO_RAD;
    private static final float DECO_X_REBOUND_SWING = 2.2F * Mth.DEG_TO_RAD;
    private static final float DECO_STATE_RESET_TICKS = 5.0F;
    private static final float DECO_STATE_EXPIRE_TICKS = 80.0F;

    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    private final Map<Long, DecoSwingState> decoSwingStates = new HashMap<>();

    protected BroomModel(String name) {
        model = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/" + name + ".geo.json");
        texture = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/" + name + ".png");
        animation = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/" + name + ".animation.json");
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
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var deco = getBone(DECO_BONE).orElse(null);
        if (deco == null) {
            return;
        }

        resetToInitialTransform(deco);
        var tick = resolveTick(animationState);
        var phaseOffset = (instanceId & 15L) * 0.37F;
        if (animatable instanceof AbstractBroomEntity broom) {
            applyEntityDecoAnimation(deco, broom, instanceId, tick, phaseOffset, animationState.getPartialTick());
        } else {
            applyItemDecoAnimation(deco, tick, phaseOffset);
        }
    }

    private void applyEntityDecoAnimation(GeoBone deco, AbstractBroomEntity broom, long instanceId,
                                          float tick, float phaseOffset, float partialTick) {
        var swingState = decoSwingStates.computeIfAbsent(instanceId, key -> new DecoSwingState());
        var deltaTick = updateDecoSwingState(swingState, broom, tick, partialTick);
        var moveAmount = Mth.clamp((float)broom.getDeltaMovement().horizontalDistance() * 3.0F, 0.0F, 1.0F);
        var idleWave = Mth.sin(tick * 0.085F + phaseOffset);
        var idleWaveSecondary = Mth.sin(tick * 0.043F + phaseOffset + 1.2F);

        var swingZ = Mth.clamp(swingState.zAngle, -DECO_MAX_Z_SWING, DECO_MAX_Z_SWING)
                + DECO_Z_IDLE_SWING * idleWave;
        var twistY = DECO_Y_IDLE_SWING * idleWaveSecondary
                + DECO_Y_SECONDARY_SWING * Mth.sin(tick * 0.117F + phaseOffset + swingState.zAngle * 1.8F);
        var swingX = moveAmount * DECO_X_MOVE_SWING * Mth.sin(tick * 0.19F + phaseOffset * 0.5F)
                + Mth.clamp(-swingState.zVelocity, -1.0F, 1.0F)
                * DECO_X_REBOUND_SWING * Mth.clamp(deltaTick, 0.0F, 1.0F);

        addRotation(deco, swingX, twistY, swingZ);
        pruneDecoSwingStates(tick);
    }

    private static void applyItemDecoAnimation(GeoBone deco, float tick, float phaseOffset) {
        var swingZ = DECO_Z_IDLE_SWING * Mth.sin(tick * 0.085F + phaseOffset);
        var twistY = DECO_Y_IDLE_SWING * Mth.sin(tick * 0.043F + phaseOffset + 1.2F)
                + DECO_Y_SECONDARY_SWING * Mth.sin(tick * 0.117F + phaseOffset);
        var swingX = DECO_X_ITEM_SWING * Mth.sin(tick * 0.071F + phaseOffset + 0.6F);
        addRotation(deco, swingX, twistY, swingZ);
    }

    private static float updateDecoSwingState(DecoSwingState state, AbstractBroomEntity broom,
                                              float tick, float partialTick) {
        var broomYaw = Mth.rotLerp(partialTick, broom.yRotO, broom.getYRot()) * Mth.DEG_TO_RAD;
        if (!state.initialized) {
            state.initialized = true;
            state.lastTick = tick;
            state.lastSeenTick = tick;
            state.lastYaw = broomYaw;
            return 0.0F;
        }

        var deltaTick = Mth.clamp(tick - state.lastTick, 0.0F, 1.5F);
        if (tick - state.lastTick > DECO_STATE_RESET_TICKS) {
            state.zAngle = 0.0F;
            state.zVelocity = 0.0F;
            deltaTick = 0.0F;
        }

        var simulationStep = Math.max(deltaTick, 0.05F);
        var yawDelta = Mth.wrapDegrees((broomYaw - state.lastYaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        state.zVelocity -= yawDelta * DECO_TURN_IMPULSE;
        state.zVelocity -= state.zAngle * DECO_Z_RESTORE_FORCE * simulationStep;
        state.zVelocity *= (float)Math.pow(DECO_Z_DAMPING, simulationStep);
        state.zAngle += state.zVelocity * simulationStep;
        state.zAngle = Mth.clamp(state.zAngle, -DECO_MAX_Z_SWING, DECO_MAX_Z_SWING);

        state.lastTick = tick;
        state.lastSeenTick = tick;
        state.lastYaw = broomYaw;
        return deltaTick;
    }

    private static float resolveTick(AnimationState<?> animationState) {
        var tickData = animationState.getData(DataTickets.TICK);
        if (tickData != null) {
            return tickData.floatValue() + animationState.getPartialTick();
        }

        var level = Minecraft.getInstance().level;
        return (level == null ? 0.0F : level.getGameTime()) + animationState.getPartialTick();
    }

    private static void addRotation(GeoBone bone, float rotX, float rotY, float rotZ) {
        var initial = bone.getInitialSnapshot();
        var baseRotX = initial == null ? 0.0F : initial.getRotX();
        var baseRotY = initial == null ? 0.0F : initial.getRotY();
        var baseRotZ = initial == null ? 0.0F : initial.getRotZ();
        bone.setRotX(baseRotX + rotX);
        bone.setRotY(baseRotY + rotY);
        bone.setRotZ(baseRotZ + rotZ);
    }

    private void pruneDecoSwingStates(float currentTick) {
        if (decoSwingStates.size() < 16) {
            return;
        }

        decoSwingStates.entrySet().removeIf(entry -> currentTick - entry.getValue().lastSeenTick > DECO_STATE_EXPIRE_TICKS);
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

    private static final class DecoSwingState {
        private float lastTick;
        private float lastSeenTick;
        private float lastYaw;
        private float zAngle;
        private float zVelocity;
        private boolean initialized;
    }
}
