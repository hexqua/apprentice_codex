package jp.aquafactory.apprenticecodex.item.manaforceblade;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class ManaForceBladeProjectileRenderer extends EntityRenderer<ManaForceBladeProjectileEntity> {
    private static final RenderType RENDER_TYPE =
            ApprenticeRenderTypes.additiveColorNoCull("mana_force_blade_projectile_additive");
    private static final float HALF_WIDTH = 1.0F / 32.0F;
    private static final float HALF_LENGTH = 4.0F / 16.0F;
    private static final int ALPHA = 210;

    public ManaForceBladeProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(ManaForceBladeProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var motion = entity.getDeltaMovement();
        var yawPitch = RotationTools.calculateYawPitchByDirection(motion.normalize());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        var color = entity.getColor();
        var red = (color >> 16) & 0xFF;
        var green = (color >> 8) & 0xFF;
        var blue = color & 0xFF;
        var vertexConsumer = buffer.getBuffer(RENDER_TYPE);
        renderBox(poseStack.last().pose(), vertexConsumer, red, green, blue);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void renderBox(Matrix4f matrix, VertexConsumer buffer, int red, int green, int blue) {
        addQuad(buffer, matrix, -HALF_WIDTH, -HALF_WIDTH, -HALF_LENGTH, HALF_WIDTH, -HALF_WIDTH, -HALF_LENGTH,
                HALF_WIDTH, HALF_WIDTH, -HALF_LENGTH, -HALF_WIDTH, HALF_WIDTH, -HALF_LENGTH, red, green, blue);
        addQuad(buffer, matrix, -HALF_WIDTH, -HALF_WIDTH, HALF_LENGTH, -HALF_WIDTH, HALF_WIDTH, HALF_LENGTH,
                HALF_WIDTH, HALF_WIDTH, HALF_LENGTH, HALF_WIDTH, -HALF_WIDTH, HALF_LENGTH, red, green, blue);
        addQuad(buffer, matrix, -HALF_WIDTH, -HALF_WIDTH, -HALF_LENGTH, -HALF_WIDTH, HALF_WIDTH, -HALF_LENGTH,
                -HALF_WIDTH, HALF_WIDTH, HALF_LENGTH, -HALF_WIDTH, -HALF_WIDTH, HALF_LENGTH, red, green, blue);
        addQuad(buffer, matrix, HALF_WIDTH, -HALF_WIDTH, -HALF_LENGTH, HALF_WIDTH, -HALF_WIDTH, HALF_LENGTH,
                HALF_WIDTH, HALF_WIDTH, HALF_LENGTH, HALF_WIDTH, HALF_WIDTH, -HALF_LENGTH, red, green, blue);
        addQuad(buffer, matrix, -HALF_WIDTH, -HALF_WIDTH, -HALF_LENGTH, -HALF_WIDTH, -HALF_WIDTH, HALF_LENGTH,
                HALF_WIDTH, -HALF_WIDTH, HALF_LENGTH, HALF_WIDTH, -HALF_WIDTH, -HALF_LENGTH, red, green, blue);
        addQuad(buffer, matrix, -HALF_WIDTH, HALF_WIDTH, -HALF_LENGTH, HALF_WIDTH, HALF_WIDTH, -HALF_LENGTH,
                HALF_WIDTH, HALF_WIDTH, HALF_LENGTH, -HALF_WIDTH, HALF_WIDTH, HALF_LENGTH, red, green, blue);
    }

    private static void addQuad(VertexConsumer buffer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                int red, int green, int blue) {
        addVertex(buffer, matrix, x1, y1, z1, red, green, blue);
        addVertex(buffer, matrix, x2, y2, z2, red, green, blue);
        addVertex(buffer, matrix, x3, y3, z3, red, green, blue);
        addVertex(buffer, matrix, x4, y4, z4, red, green, blue);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f matrix,
                                  float x, float y, float z,
                                  int red, int green, int blue) {
        buffer.vertex(matrix, x, y, z)
                .color(red, green, blue, ALPHA)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ManaForceBladeProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
