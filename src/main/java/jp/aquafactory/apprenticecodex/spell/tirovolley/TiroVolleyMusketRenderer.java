package jp.aquafactory.apprenticecodex.spell.tirovolley;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.model.TiroVolleyMusketModel;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TiroVolleyMusketRenderer extends GeoEntityRenderer<TiroVolleyMusketEntity> {
    public TiroVolleyMusketRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TiroVolleyMusketModel<>());
    }

    @Override
    public void render(@NotNull TiroVolleyMusketEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var recoilTick = entity.getRecoilTick();
        var yawPitch = calculateYawPitchForRecoil(entity, recoilTick, partialTick);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        if (entity.isFired() && recoilTick > 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(calculateRecoilUpAngle(8.0f, recoilTick, partialTick)));
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static RotationTools.YawPitch calculateYawPitchForRecoil(TiroVolleyMusketEntity entity, int recoilTick, float partialTick) {
        var rawYawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTick);
        if (recoilTick <= 0) {
            return rawYawPitch;
        }

        var recoilAnimationTick = TiroVolleyMusketEntity.MAX_RECOIL_TICK - recoilTick + partialTick;
        if (recoilAnimationTick < 5) {
            return new RotationTools.YawPitch(entity.getFireYaw(), entity.getFirePitch());
        }

        var v = recoilTick / (float) (TiroVolleyMusketEntity.MAX_RECOIL_TICK - 5);
        return new RotationTools.YawPitch(
                Mth.lerp(v, rawYawPitch.yaw(), entity.getFireYaw()),
                Mth.lerp(v, rawYawPitch.pitch(), entity.getFirePitch())
        );
    }

    private static float calculateRecoilUpAngle(float angle, float recoilTick, float partialTick) {
        var recoilAnimationTick = TiroVolleyMusketEntity.MAX_RECOIL_TICK - recoilTick + partialTick;
        if (recoilAnimationTick < 4) {
            return angle * (1.0f - recoilAnimationTick / 4.0f);
        }
        return 0.0f;
    }
}
