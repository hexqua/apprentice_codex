package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaff;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ZenithStaffModel extends GeoModel<ZenithStaff> {
    private static final String TAIL_BONE = "tail";
    private static final float TAIL_ROT_X_SPEED = 0.55F * Mth.DEG_TO_RAD;
    private static final float TAIL_ROT_Y_SPEED = 0.85F * Mth.DEG_TO_RAD;
    private static final float TAIL_ROT_Z_SPEED = 0.70F * Mth.DEG_TO_RAD;
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/zenith_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/zenith_staff.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/zenith_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(ZenithStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ZenithStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(ZenithStaff animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(ZenithStaff animatable, long instanceId, AnimationState<ZenithStaff> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var tail = getBone(TAIL_BONE).orElse(null);
        if (tail == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (isStaticPerspective(perspective)) {
            resetToInitialTransform(tail);
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? 0.0F : tickData.floatValue();

        var initial = tail.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0F : initial.getRotX();
        float baseRotY = initial == null ? 0.0F : initial.getRotY();
        float baseRotZ = initial == null ? 0.0F : initial.getRotZ();

        tail.setRotX(baseRotX + tick * TAIL_ROT_X_SPEED);
        tail.setRotY(baseRotY + tick * TAIL_ROT_Y_SPEED);
        tail.setRotZ(baseRotZ + tick * TAIL_ROT_Z_SPEED);
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.FIXED
                || perspective == ItemDisplayContext.GROUND;
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
