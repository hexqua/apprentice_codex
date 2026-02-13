package jp.aquafactory.apprenticecodex.spell.tinylumberjack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import jp.aquafactory.apprenticecodex.model.TinyLumberjackSawModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TinyLumberjackSawRenderer extends GeoEntityRenderer<TinyLumberjackSawEntity> {
    public TinyLumberjackSawRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new TinyLumberjackSawModel<>());
    }

        @Override
    public void render(@NotNull TinyLumberjackSawEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
