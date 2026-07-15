package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ElementalBowModel extends GeoModel<ElementalBow> {
    private static final String ORB_FOCUS_BONE = "orb_focus";
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/elemental_bow.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/elemental_bow.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/elemental_bow.animation.json");

    @Override
    public ResourceLocation getModelResource(ElementalBow animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ElementalBow animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ElementalBow animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ElementalBow animatable, long instanceId, AnimationState<ElementalBow> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var orbFocus = getBone(ORB_FOCUS_BONE).orElse(null);
        if (orbFocus == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var itemStack = animationState.getData(DataTickets.ITEMSTACK);
        if (isStaticPerspective(perspective) || itemStack == null || itemStack.isEmpty()) {
            resetToInitialTransform(orbFocus);
            return;
        }

        var rotation = ElementalBowClientRenderState.resolveOrbRotation(itemStack, perspective, animationState.getPartialTick());
        var initial = orbFocus.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0F : initial.getRotX();
        float baseRotY = initial == null ? 0.0F : initial.getRotY();
        float baseRotZ = initial == null ? 0.0F : initial.getRotZ();

        orbFocus.setRotX(baseRotX + rotation.rotX());
        orbFocus.setRotY(baseRotY + rotation.rotY());
        orbFocus.setRotZ(baseRotZ + rotation.rotZ());
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
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
        bone.setHidden(false);
        bone.setChildrenHidden(false);
    }
}
