package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.item.armor.ChromaticMagiaDressItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ChromaticMagiaDressModel extends GeoModel<ChromaticMagiaDressItem> {
    private static final String HIP_WIND_BONE = "hip_wind";

    private static final float HIP_WIND_BASE_SWING_X = 4.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_MOVE_SWING_X = 38.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_FALL_SWING_X = 18.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_RISE_SWING_X = 14.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_CROUCH_SWING_X = 12.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_GLIDE_SWING_X = 14.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_IDLE_SWING_X = 3.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_MOVE_FLUTTER_X = 12.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_IDLE_SWING_Z = 2.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_MOVE_SWING_Z = 16.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_IDLE_TWIST_Y = 2.0F * Mth.DEG_TO_RAD;
    private static final float HIP_WIND_MOVE_TWIST_Y = 10.0F * Mth.DEG_TO_RAD;

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/chromatic_magia_dress.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/chromatic_magia_dress.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/chromatic_magia_dress.animation.json");

    @Override
    public ResourceLocation getModelResource(ChromaticMagiaDressItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ChromaticMagiaDressItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ChromaticMagiaDressItem animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ChromaticMagiaDressItem animatable, long instanceId,
                                    AnimationState<ChromaticMagiaDressItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        applyHipWindAnimation(instanceId, animationState);
    }

    private void applyHipWindAnimation(long instanceId, AnimationState<ChromaticMagiaDressItem> animationState) {
        var hipWind = getBone(HIP_WIND_BONE).orElse(null);
        if (hipWind == null) {
            return;
        }

        resetToInitialTransform(hipWind);
        if (!ApprenticeCodexClientConfig.enableApprenticeMageRobeCapeAnimation()) {
            return;
        }

        var slot = animationState.getData(DataTickets.EQUIPMENT_SLOT);
        var entity = animationState.getData(DataTickets.ENTITY);
        if (slot != EquipmentSlot.CHEST || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = (tickData == null ? 0.0F : tickData.floatValue()) + animationState.getPartialTick();
        float phaseOffset = (instanceId & 15L) * 0.41F;

        float horizontalSpeed = (float) Mth.clamp(livingEntity.getDeltaMovement().horizontalDistance(), 0.0, 0.40);
        float verticalSpeed = (float) Mth.clamp(livingEntity.getDeltaMovement().y, -0.50, 0.40);
        float moveAmount = Mth.clamp(Math.max(horizontalSpeed * 4.0F, animationState.getLimbSwingAmount() * 1.2F),
                0.0F, 1.0F);
        float fallingAmount = Mth.clamp(-verticalSpeed * 2.1F, 0.0F, 1.0F);
        float risingAmount = Mth.clamp(verticalSpeed * 2.4F, 0.0F, 1.0F);

        float idleWave = Mth.sin(tick * 0.12F + phaseOffset);
        float idleWaveSecondary = Mth.sin(tick * 0.073F + phaseOffset + 1.35F);
        float strideWave = Mth.sin(animationState.getLimbSwing() * 0.74F + phaseOffset * 0.5F);

        float crouchSwing = livingEntity.isCrouching() ? HIP_WIND_CROUCH_SWING_X : 0.0F;
        float glideSwing = livingEntity.isFallFlying() ? HIP_WIND_GLIDE_SWING_X : 0.0F;
        float swingX = HIP_WIND_BASE_SWING_X
                + moveAmount * HIP_WIND_MOVE_SWING_X
                + fallingAmount * HIP_WIND_FALL_SWING_X
                - risingAmount * HIP_WIND_RISE_SWING_X
                + crouchSwing
                + glideSwing;
        float flutterX = (HIP_WIND_IDLE_SWING_X + moveAmount * HIP_WIND_MOVE_FLUTTER_X)
                * (idleWave * 0.45F + strideWave * 0.55F);
        float swingZ = (HIP_WIND_IDLE_SWING_Z + moveAmount * HIP_WIND_MOVE_SWING_Z)
                * (strideWave * 0.75F + idleWaveSecondary * 0.25F);
        float twistY = (HIP_WIND_IDLE_TWIST_Y + moveAmount * HIP_WIND_MOVE_TWIST_Y) * idleWaveSecondary;

        var initial = hipWind.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0F : initial.getRotX();
        float baseRotY = initial == null ? 0.0F : initial.getRotY();
        float baseRotZ = initial == null ? 0.0F : initial.getRotZ();

        // 小さな腰布でも移動感が出るよう、通常のケープより振れ幅を強めにする。
        hipWind.setRotX(baseRotX - swingX - flutterX);
        hipWind.setRotY(baseRotY + twistY);
        hipWind.setRotZ(baseRotZ + swingZ);
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
}
