package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class EnchantressRobeModel extends GeoModel<EnchantressRobeItem> {
    private static final String CAPE_ROOT_BONE = "cape_root";
    private static final String CAPE_MID_BONE = "came_mid";
    private static final String CAPE_TIP_BONE = "came_tip";

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

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/enchantress_robe.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/enchantress_robe.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/enchantress_robe.animation.json");

    @Override
    public ResourceLocation getModelResource(EnchantressRobeItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EnchantressRobeItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(EnchantressRobeItem animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(EnchantressRobeItem animatable, long instanceId,
                                    AnimationState<EnchantressRobeItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

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
}
