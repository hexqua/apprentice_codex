package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.client.event.RenderHandEvent;

public final class PsychicDriveGauntletsHandRenderer {
    private PsychicDriveGauntletsHandRenderer() {}

    public static void render(RenderHandEvent event, AbstractClientPlayer player) {
        if (player.isInvisible() || player.isScoping()) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        // 外部MODがプレイヤーrendererを置換した場合は、腕だけを省略して描画を継続する。
        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;

        var arm = player.getMainArm();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float swing = event.getSwingProgress();
        float rootSwing = Mth.sqrt(swing);
        float swingOffsetX = -0.3F * Mth.sin(rootSwing * Mth.PI);
        float swingOffsetY = 0.4F * Mth.sin(rootSwing * Mth.TWO_PI);
        float swingOffsetZ = -0.4F * Mth.sin(swing * Mth.PI);
        var poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            // バニラのrenderPlayerArmと同じ座標変換を行い、非公開メソッドへのInvoker依存を外す。
            // RenderHandEventには視点の揺れが既に適用されているため、ここでは腕の変換だけを加える。
            poseStack.translate(side * (swingOffsetX + 0.64000005F),
                    swingOffsetY - 0.6F - event.getEquipProgress() * 0.6F, swingOffsetZ - 0.71999997F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
            float squaredSwingSine = Mth.sin(swing * swing * Mth.PI);
            float rootSwingSine = Mth.sin(rootSwing * Mth.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * rootSwingSine * 70.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * squaredSwingSine * -20.0F));
            poseStack.translate(side * -1.0F, 3.6F, 3.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
            poseStack.translate(side * 5.6F, 0.0F, 0.0F);

            // 公開APIを経由し、スキン・袖とNeoForgeの一人称腕描画eventを維持する。
            if (arm == HumanoidArm.RIGHT) {
                playerRenderer.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
            } else {
                playerRenderer.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
        } finally {
            poseStack.popPose();
        }
    }
}
