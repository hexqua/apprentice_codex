package jp.aquafactory.apprenticecodex.common.spells.personalshelf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class PersonalShelfChestBlockRenderer implements BlockEntityRenderer<PersonalShelfChestBlockEntity> {
    public PersonalShelfChestBlockRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull PersonalShelfChestBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {

        var level = blockEntity.getLevel();
        var state = blockEntity.getBlockState();
        if (level == null) {
            return;
        }

        // ブロック用.
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var model = blockRenderer.getBlockModel(state);
        @SuppressWarnings("deprecation") var rt = ItemBlockRenderTypes.getRenderType(state, false);
        var vc = buffer.getBuffer(rt);

        // 浮く演出用.
        var t = (level.getGameTime() + partialTick) / 10.0;
        var h = blockEntity.getBlockPos().hashCode();
        var phase = (h & 1023) / 1023.0 * (Math.PI * 2.0);
        var y = (float)(Math.sin(t + phase) * 0.04f);

        poseStack.pushPose();
        poseStack.translate(0.0, 0.1 + y, 0.0);

        //noinspection deprecation
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                vc,
                state,
                model,
                1.0f, 1.0f, 1.0f,
                packedLight,
                packedOverlay
        );

        // エンドポータル流用.
        vc = buffer.getBuffer(RenderType.endPortal());
        drawQuadUp(poseStack, vc, 0.25f, 0.75f, 0.25f, 0.75f, 0.7001f);

        poseStack.popPose();
    }

    @SuppressWarnings("SameParameterValue")
    private static void drawQuadUp(PoseStack poseStack, VertexConsumer vc,
                                   float x0, float x1, float z0, float z1, float y) {
        var pose = poseStack.last();
        var mat = pose.pose();

        vc.vertex(mat, x0, y, z0).color(255, 255, 255, 255).endVertex();
        vc.vertex(mat, x0, y, z1).color(255, 255, 255, 255).endVertex();
        vc.vertex(mat, x1, y, z1).color(255, 255, 255, 255).endVertex();
        vc.vertex(mat, x1, y, z0).color(255, 255, 255, 255).endVertex();
    }
}
