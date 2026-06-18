package jp.aquafactory.apprenticecodex.spell.anchorblink;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

public class AnchorBlinkDaggerRenderer extends EntityRenderer<AnchorBlinkDaggerEntity> {
    public AnchorBlinkDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(@NotNull AnchorBlinkDaggerEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = entity.resolveRenderYawPitch(partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        // SpellSideEdge のモデルは Y+ が先端なので、既存の進行方向(+Z)基準へ倒して描画する.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getRenderStack(),
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AnchorBlinkDaggerEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
