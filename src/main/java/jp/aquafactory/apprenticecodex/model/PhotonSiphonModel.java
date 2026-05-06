package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphon;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphonClientRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class PhotonSiphonModel extends GeoModel<PhotonSiphon> {
    private static final String SIPHON_RING_1_BONE = "siphon_ring_1";
    private static final String SIPHON_RING_2_BONE = "siphon_ring_2";
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/photon_siphon.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/photon_siphon.animation.json");

    @Override
    public ResourceLocation getModelResource(PhotonSiphon animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PhotonSiphon animatable) {
        return animatable.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(PhotonSiphon animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(PhotonSiphon animatable, long instanceId, AnimationState<PhotonSiphon> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var stack = animationState.getData(DataTickets.ITEMSTACK);
        var partialTick = animationState.getPartialTick();

        getBone(SIPHON_RING_1_BONE).ifPresent(ring ->
                applyRingState(ring, PhotonSiphonClientRenderState.resolveCombatRing(stack, perspective, partialTick)));
        getBone(SIPHON_RING_2_BONE).ifPresent(ring ->
                applyRingState(ring, PhotonSiphonClientRenderState.resolveManaRing(stack, perspective, partialTick)));
    }

    private static void applyRingState(GeoBone bone, PhotonSiphonClientRenderState.RingRenderState state) {
        var initial = bone.getInitialSnapshot();
        if (!state.visible()) {
            resetToInitialTransform(bone);
            bone.setHidden(true);
            bone.setChildrenHidden(true);
            return;
        }

        bone.setHidden(false);
        bone.setChildrenHidden(false);

        float baseRotX = initial == null ? 0.0F : initial.getRotX();
        float baseRotY = initial == null ? 0.0F : initial.getRotY();
        float baseRotZ = initial == null ? 0.0F : initial.getRotZ();
        float baseScaleX = initial == null ? 1.0F : initial.getScaleX();
        float baseScaleY = initial == null ? 1.0F : initial.getScaleY();
        float baseScaleZ = initial == null ? 1.0F : initial.getScaleZ();

        bone.setRotX(baseRotX);
        bone.setRotY(baseRotY + state.rotY());
        bone.setRotZ(baseRotZ);
        bone.setScaleX(baseScaleX * state.scale());
        bone.setScaleY(baseScaleY * state.scale());
        bone.setScaleZ(baseScaleZ * state.scale());
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
        bone.setScaleX(initial.getScaleX());
        bone.setScaleY(initial.getScaleY());
        bone.setScaleZ(initial.getScaleZ());
    }
}
