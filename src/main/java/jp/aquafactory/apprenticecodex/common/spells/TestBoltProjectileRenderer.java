package jp.aquafactory.apprenticecodex.common.spells;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

public class TestBoltProjectileRenderer extends EntityRenderer<TestBoltProjectileEntity> {
    public TestBoltProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(TestBoltProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var stack = entity.getItem();
        if (stack.isEmpty()) return;

        var yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        var pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        // 右上に切っ先が向いているアイテムが先端を向くように調整.
        var scale = 1.0f;
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0f));
        poseStack.scale(scale, scale, scale);

        // ItemRendererで描画.
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.NONE,
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull TestBoltProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
