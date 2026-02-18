package jp.aquafactory.apprenticecodex.spell.moonunite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.MoonUniteKatanaModel;
import jp.aquafactory.apprenticecodex.renderer.SwordTrailLayer;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MoonUniteKatanaRenderer extends GeoEntityRenderer<MoonUniteKatanaEntity> {
    public MoonUniteKatanaRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new MoonUniteKatanaModel<>());
        addRenderLayer(new SwordTrailLayer<>(this));
    }

    @Override
    public void render(@NotNull MoonUniteKatanaEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
