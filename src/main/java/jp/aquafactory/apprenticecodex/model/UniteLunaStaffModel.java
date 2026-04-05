package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.UniteLunaStaff;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class UniteLunaStaffModel extends GeoModel<UniteLunaStaff> {
    private static final String MOON_BONE = "moon";

    private static final float MOON_ROT_X_SPEED = 0.55f * Mth.DEG_TO_RAD;
    private static final float MOON_ROT_Y_SPEED = 0.85f * Mth.DEG_TO_RAD;
    private static final float MOON_ROT_Z_SPEED = 0.70f * Mth.DEG_TO_RAD;

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/unite_luna_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/unite_luna_staff.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/unite_luna_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(UniteLunaStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(UniteLunaStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(UniteLunaStaff animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(UniteLunaStaff animatable, long instanceId, AnimationState<UniteLunaStaff> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var moon = getBone(MOON_BONE).orElse(null);
        if (moon == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        var level = Minecraft.getInstance().level;
        if (isStaticPerspective(perspective) || level == null) {
            resetToInitialTransform(moon);
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? animationState.getPartialTick() : tickData.floatValue() + animationState.getPartialTick();
        applyMoonRotation(moon, tick);
    }

    private static boolean isStaticPerspective(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.GUI
                || perspective == ItemDisplayContext.GROUND
                || perspective == ItemDisplayContext.FIXED;
    }

    private static void applyMoonRotation(GeoBone moon, float tick) {
        var initial = moon.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float baseRotZ = initial == null ? 0.0f : initial.getRotZ();

        moon.setRotX(baseRotX + tick * MOON_ROT_X_SPEED);
        moon.setRotY(baseRotY + tick * MOON_ROT_Y_SPEED);
        moon.setRotZ(baseRotZ + tick * MOON_ROT_Z_SPEED);
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
