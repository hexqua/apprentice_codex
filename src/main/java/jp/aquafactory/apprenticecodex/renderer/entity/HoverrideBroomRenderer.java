package jp.aquafactory.apprenticecodex.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.model.HoverrideBroomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public final class HoverrideBroomRenderer extends BroomEntityRenderer<HoverrideBroomEntity> {
    private static final float MAX_TURN_LEAN_DEGREES = 14.0F;
    private static final float MAX_TURN_DELTA_DEGREES = 10.0F;

    public HoverrideBroomRenderer(EntityRendererProvider.Context context) {
        super(context, new HoverrideBroomModel<>());
    }

    @Override
    protected void applyRotations(HoverrideBroomEntity broom, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        super.applyRotations(broom, poseStack, ageInTicks, rotationYaw, partialTick);

        var intensity = broom.getSpeedEffectIntensity();
        var pitch = switch (broom.getPresentationState()) {
            case ACCELERATING -> -8.0F;
            case GLIDING -> -5.0F;
            case BRAKING -> 9.0F;
            case NORMAL -> -2.0F;
        } * intensity;
        var yawDelta = Mth.wrapDegrees(broom.getYRot() - broom.yRotO);
        var roll = Mth.clamp(yawDelta / MAX_TURN_DELTA_DEGREES, -1.0F, 1.0F)
                * MAX_TURN_LEAN_DEGREES * intensity;

        // 速度そのものではなく姿勢変化で加減速と旋回を見せ、移動性能へ影響させない。
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }
}
