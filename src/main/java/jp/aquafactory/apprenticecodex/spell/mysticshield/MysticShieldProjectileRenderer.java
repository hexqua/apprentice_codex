package jp.aquafactory.apprenticecodex.spell.mysticshield;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class MysticShieldProjectileRenderer extends EntityRenderer<MysticShieldProjectileEntity> {
    private static final ResourceLocation UNUSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/spell/mystic_shield_shield.png");
    private static final RenderType RENDER_TYPE =
            ApprenticeRenderTypes.colorNoCull("mystic_shield_projectile_cube");
    private static final float OUTER_HALF = 0.16f;
    private static final float INNER_HALF = 0.085f;
    private static final float INSET_EPSILON = 0.0015f;

    public MysticShieldProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull MysticShieldProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        var yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        var pitch = Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot());
        var age = entity.tickCount + partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 11.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 17.0f));

        drawCube(poseStack.last().pose(), buffer.getBuffer(RENDER_TYPE));
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MysticShieldProjectileEntity entity) {
        return UNUSED_TEXTURE;
    }

    private static void drawCube(Matrix4f pose, VertexConsumer consumer) {
        drawFaceWithInset(consumer, pose, Face.POSITIVE_Z);
        drawFaceWithInset(consumer, pose, Face.NEGATIVE_Z);
        drawFaceWithInset(consumer, pose, Face.POSITIVE_X);
        drawFaceWithInset(consumer, pose, Face.NEGATIVE_X);
        drawFaceWithInset(consumer, pose, Face.POSITIVE_Y);
        drawFaceWithInset(consumer, pose, Face.NEGATIVE_Y);
    }

    private static void drawFaceWithInset(VertexConsumer consumer, Matrix4f pose, Face face) {
        face.quad(consumer, pose, OUTER_HALF, OUTER_HALF, 255, 124, 24, 255, 0.0f);
        face.quad(consumer, pose, INNER_HALF, INNER_HALF, 255, 255, 255, 255, INSET_EPSILON);
    }

    private enum Face {
        POSITIVE_Z {
            @Override
            void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green, int blue,
                      int alpha, float offset) {
                var z = OUTER_HALF + offset;
                vertex(consumer, pose, -halfA, -halfB, z, red, green, blue, alpha);
                vertex(consumer, pose, halfA, -halfB, z, red, green, blue, alpha);
                vertex(consumer, pose, halfA, halfB, z, red, green, blue, alpha);
                vertex(consumer, pose, -halfA, halfB, z, red, green, blue, alpha);
            }
        },
        NEGATIVE_Z {
            @Override
            void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green, int blue,
                      int alpha, float offset) {
                var z = -OUTER_HALF - offset;
                vertex(consumer, pose, halfA, -halfB, z, red, green, blue, alpha);
                vertex(consumer, pose, -halfA, -halfB, z, red, green, blue, alpha);
                vertex(consumer, pose, -halfA, halfB, z, red, green, blue, alpha);
                vertex(consumer, pose, halfA, halfB, z, red, green, blue, alpha);
            }
        },
        POSITIVE_X {
            @Override
            void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green, int blue,
                      int alpha, float offset) {
                var x = OUTER_HALF + offset;
                vertex(consumer, pose, x, -halfA, halfB, red, green, blue, alpha);
                vertex(consumer, pose, x, -halfA, -halfB, red, green, blue, alpha);
                vertex(consumer, pose, x, halfA, -halfB, red, green, blue, alpha);
                vertex(consumer, pose, x, halfA, halfB, red, green, blue, alpha);
            }
        },
        NEGATIVE_X {
            @Override
            void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green, int blue,
                      int alpha, float offset) {
                var x = -OUTER_HALF - offset;
                vertex(consumer, pose, x, -halfA, -halfB, red, green, blue, alpha);
                vertex(consumer, pose, x, -halfA, halfB, red, green, blue, alpha);
                vertex(consumer, pose, x, halfA, halfB, red, green, blue, alpha);
                vertex(consumer, pose, x, halfA, -halfB, red, green, blue, alpha);
            }
        },
        POSITIVE_Y {
            @Override
            void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green, int blue,
                      int alpha, float offset) {
                var y = OUTER_HALF + offset;
                vertex(consumer, pose, -halfA, y, halfB, red, green, blue, alpha);
                vertex(consumer, pose, halfA, y, halfB, red, green, blue, alpha);
                vertex(consumer, pose, halfA, y, -halfB, red, green, blue, alpha);
                vertex(consumer, pose, -halfA, y, -halfB, red, green, blue, alpha);
            }
        },
        NEGATIVE_Y {
            @Override
            void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green, int blue,
                      int alpha, float offset) {
                var y = -OUTER_HALF - offset;
                vertex(consumer, pose, -halfA, y, -halfB, red, green, blue, alpha);
                vertex(consumer, pose, halfA, y, -halfB, red, green, blue, alpha);
                vertex(consumer, pose, halfA, y, halfB, red, green, blue, alpha);
                vertex(consumer, pose, -halfA, y, halfB, red, green, blue, alpha);
            }
        };

        abstract void quad(VertexConsumer consumer, Matrix4f pose, float halfA, float halfB, int red, int green,
                           int blue, int alpha, float offset);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        consumer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
