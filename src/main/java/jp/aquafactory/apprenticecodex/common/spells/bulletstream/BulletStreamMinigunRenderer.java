package jp.aquafactory.apprenticecodex.common.spells.bulletstream;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import jp.aquafactory.apprenticecodex.gecko.BulletStreamMinigunModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BulletStreamMinigunRenderer extends GeoEntityRenderer<BulletStreamMinigunEntity> {
    private static final RandomSource RNG = RandomSource.create();

    public BulletStreamMinigunRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new BulletStreamMinigunModel<>());
    }

    @Override
    public void render(@NotNull BulletStreamMinigunEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        // リコイル表現はシンプルに.
        if (entity.getIsRecoilTick()) {
            // 同一tickであればランダムにブレないように.
            RNG.setSeed(entity.tickCount + entity.getId());
            var randomPitch = (RNG.nextFloat() * 6f - 3f) * (1 - partialTicks);
            var randomYaw = (RNG.nextFloat() * 2f - 1f) * (1 - partialTicks);
            poseStack.mulPose(Axis.XP.rotationDegrees(randomPitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomYaw));
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}

