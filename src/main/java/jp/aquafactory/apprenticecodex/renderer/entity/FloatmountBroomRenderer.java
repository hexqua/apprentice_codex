package jp.aquafactory.apprenticecodex.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.model.FloatmountBroomModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

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

    // todo: internal以外を探す.
    @Override
    public void render(@NotNull FloatmountBroomEntity broom, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (!broom.isVehicle()) {
            var hurtTime = broom.getHurtTime() - partialTick;
            var damage = Math.max(0.0F, broom.getDamage() - partialTick);
            if (hurtTime > 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(
                        Mth.sin(hurtTime) * hurtTime * damage / 10.0F * broom.getHurtDirection()
                ));
            }
        }
        // todo: internal以外を探す.
        super.render(broom, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
