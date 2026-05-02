package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaffClientRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class CircuitHeatStaffModel extends GeoModel<CircuitHeatStaff> {
    private static final String CORE_BONE = "core";
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/circuit_heat_staff.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/circuit_heat_staff.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/circuit_heat_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(CircuitHeatStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CircuitHeatStaff animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CircuitHeatStaff animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(CircuitHeatStaff animatable, long instanceId, AnimationState<CircuitHeatStaff> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var core = getBone(CORE_BONE).orElse(null);
        if (core == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var itemStack = animationState.getData(DataTickets.ITEMSTACK);
        if (itemStack == null || itemStack.isEmpty()) {
            resetRotation(core);
            return;
        }

        var initial = core.getInitialSnapshot();
        var baseRotX = initial == null ? 0.0F : initial.getRotX();
        var baseRotY = initial == null ? 0.0F : initial.getRotY();
        var baseRotZ = initial == null ? 0.0F : initial.getRotZ();
        var rotation = CircuitHeatStaffClientRenderState.resolveCoreRotation(itemStack, perspective, animationState.getPartialTick());
        core.setRotX(baseRotX + rotation * 0.7F);
        core.setRotY(baseRotY + rotation);
        core.setRotZ(baseRotZ + rotation * 0.45F);
    }

    private static void resetRotation(GeoBone bone) {
        var initial = bone.getInitialSnapshot();
        if (initial == null) {
            return;
        }

        bone.setRotX(initial.getRotX());
        bone.setRotY(initial.getRotY());
        bone.setRotZ(initial.getRotZ());
    }
}
