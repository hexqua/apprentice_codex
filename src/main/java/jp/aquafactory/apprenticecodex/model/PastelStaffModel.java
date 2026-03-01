package jp.aquafactory.apprenticecodex.model;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class PastelStaffModel extends GeoModel<PastelStaff> {
    private static final String STONE_TINT_BONE = "stone_tint";

    private static final float STONE_ROT_X_AMPLITUDE = 6.0f * Mth.DEG_TO_RAD;
    private static final float STONE_ROT_Y_AMPLITUDE = 8.0f * Mth.DEG_TO_RAD;
    private static final float STONE_ROT_Z_AMPLITUDE = 6.0f * Mth.DEG_TO_RAD;
    private static final float STONE_FLOAT_Y_AMPLITUDE = 0.60f;
    private static final float STONE_SPIN_Y_SPEED = 2.4f * Mth.DEG_TO_RAD;

    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "geo/pastel_staff.geo.json");
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/pastel_staff.png");
    private static final ResourceLocation ANIM =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "animations/pastel_staff.animation.json");

    @Override
    public ResourceLocation getModelResource(PastelStaff animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PastelStaff animatable) {
        return TEX;
    }

    @Override
    public ResourceLocation getAnimationResource(PastelStaff animatable) {
        return ANIM;
    }

    @Override
    public void setCustomAnimations(PastelStaff animatable, long instanceId, AnimationState<PastelStaff> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var stone = getBone(STONE_TINT_BONE).orElse(null);
        if (stone == null) {
            return;
        }

        var perspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        if (isStaticPerspective(perspective)) {
            resetToInitialTransform(stone);
            return;
        }

        var tickData = animationState.getData(DataTickets.TICK);
        float tick = tickData == null ? 0.0f : tickData.floatValue();

        float waveA = Mth.sin(tick * 0.17f);
        float waveB = Mth.sin(tick * 0.059f + 0.7f);
        float waveC = Mth.sin(tick * 0.13f + 1.4f);
        float waveD = Mth.sin(tick * 0.041f + 2.6f);
        float spinY = (tick * STONE_SPIN_Y_SPEED) % Mth.TWO_PI;

        var initial = stone.getInitialSnapshot();
        float baseRotX = initial == null ? 0.0f : initial.getRotX();
        float baseRotY = initial == null ? 0.0f : initial.getRotY();
        float baseRotZ = initial == null ? 0.0f : initial.getRotZ();
        float basePosY = initial == null ? 0.0f : initial.getOffsetY();

        stone.setRotX(baseRotX + (waveA + waveB) * 0.5f * STONE_ROT_X_AMPLITUDE);
        stone.setRotY(baseRotY + spinY + (waveC + waveD) * 0.5f * STONE_ROT_Y_AMPLITUDE);
        stone.setRotZ(baseRotZ + (waveB + waveD) * 0.5f * STONE_ROT_Z_AMPLITUDE);
        stone.setPosY(basePosY + (waveA + waveD) * 0.5f * STONE_FLOAT_Y_AMPLITUDE);
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
