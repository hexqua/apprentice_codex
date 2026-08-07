package jp.aquafactory.apprenticecodex.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.model.FloatmountBroomModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class FloatmountBroomRenderer extends GeoEntityRenderer<FloatmountBroomEntity> {
    public FloatmountBroomRenderer(EntityRendererProvider.Context context) {
        super(context, new FloatmountBroomModel<>());
        shadowRadius = 0.35F;
    }

    @Override
    protected void applyRotations(FloatmountBroomEntity broom, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        // GeoEntityRendererは非LivingEntityのbody yawを0として扱うため、車体自身の補間yawを明示する。
        var broomYaw = Mth.rotLerp(partialTick, broom.yRotO, broom.getYRot());
        super.applyRotations(broom, poseStack, ageInTicks, broomYaw, partialTick, nativeScale);
    }

    @Override
    public Color getRenderColor(FloatmountBroomEntity broom, float partialTick, int packedLight) {
        var base = super.getRenderColor(broom, partialTick, packedLight);
        var multipliers = switch (broom.getDamageStage()) {
            case 1 -> new float[]{0.90F, 0.78F, 0.78F};
            case 2 -> new float[]{0.78F, 0.55F, 0.55F};
            case 3 -> new float[]{0.66F, 0.34F, 0.34F};
            case 4 -> new float[]{0.52F, 0.18F, 0.18F};
            default -> new float[]{1.0F, 1.0F, 1.0F};
        };
        return Color.ofARGB(
                base.getAlpha(),
                Mth.clamp(Math.round(base.getRed() * multipliers[0]), 0, 255),
                Mth.clamp(Math.round(base.getGreen() * multipliers[1]), 0, 255),
                Mth.clamp(Math.round(base.getBlue() * multipliers[2]), 0, 255)
        );
    }
}