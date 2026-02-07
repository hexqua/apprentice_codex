package jp.aquafactory.apprenticecodex.common.spells.arcanebeam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// todo:ビームエンティティは絶対汎用性があるので実装が完了したら抽象化して切り出す.
public class ArcaneBeamRenderer extends EntityRenderer<ArcaneBeamEntity> {

    // バニラのビーコンを使う.
    private static final ResourceLocation BEAM_TEX = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public ArcaneBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(@NotNull ArcaneBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {

        // ビーム描画は真上になっているので、これをビームの方向変換をかける.
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        var dir = Vec3.directionFromRotation(yawPitch.pitch(), yawPitch.yaw()).normalize();
        var from = new Vector3f(0, 1, 0);
        var to = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z);
        var q = new Quaternionf().rotationTo(from, to);


        var length = entity.getLength();
        var radius = entity.getRadius();
        var outArgb = entity.getColorARGBOuter();
        var inArgb = entity.getColorARGBInner();

        // UVスクロール.
        var time = (entity.tickCount + partialTicks) * 0.25f;

        // ビーコンのように回転しつつ流れるようにする.
        var vc = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEX, true));
        poseStack.pushPose();
        poseStack.mulPose(q);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 7.1f));
        drawBeam(poseStack, vc, length, radius, outArgb, packedLight, time);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(q);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTicks) * 3.3f));
        drawBeam(poseStack, vc, length, radius * 0.7f, inArgb, packedLight, time);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }


    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ArcaneBeamEntity entity) {
        return BEAM_TEX;
    }

    private void drawBeam(PoseStack poseStack, VertexConsumer consumer, float length, float radius, int argb, int packedLight, float uvParameter){
        // 色抽出.
        var a = ((argb >> 24) & 0xFF) / 255.0f;
        var r = ((argb >> 16) & 0xFF) / 255.0f;
        var g = ((argb >> 8) & 0xFF) / 255.0f;
        var b = (argb & 0xFF) / 255.0f;

        // UVスクロール対応.
        var v0 = -uvParameter;
        var v1 = v0 + length;

        // ビーコンのビームを直接Quadで描画.
        var x0 = -radius;
        var x1 = radius;
        var z0 = -radius;
        var z1 = radius;
        var y0 = 0.0f;
        var y1 = length;

        // 法線類も渡さないと落ちる.
        var last = poseStack.last();
        var poseMat = last.pose();
        var normalMat = last.normal();

        // Quadを「外向き」で4枚
        //noinspection DuplicatedCode
        addQuad(poseMat, normalMat, consumer,
                x1, y0, z0,  0f, v0,
                x1, y1, z0,  0f, v1,
                x1, y1, z1,  1f, v1,
                x1, y0, z1,  1f, v0,
                r, g, b, a, packedLight,
                1f,0f,0f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z1,  0f, v0,
                x0, y1, z1,  0f, v1,
                x0, y1, z0,  1f, v1,
                x0, y0, z0,  1f, v0,
                r, g, b, a, packedLight,
                -1f,0f,0f);

        //noinspection DuplicatedCode
        addQuad(poseMat, normalMat, consumer,
                x1, y0, z1,  0f, v0,
                x1, y1, z1,  0f, v1,
                x0, y1, z1,  1f, v1,
                x0, y0, z1,  1f, v0,
                r, g, b, a, packedLight,
                0f, 0f, 1f);

        addQuad(poseMat, normalMat, consumer,
                x0, y0, z0,  0f, v0,
                x0, y1, z0,  0f, v1,
                x1, y1, z0,  1f, v1,
                x1, y0, z0,  1f, v0,
                r, g, b, a, packedLight,
                0f, 0f, -1f);
    }

    private static void addQuad(Matrix4f poseMat, Matrix3f normalMat, VertexConsumer vc,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float r, float g, float b, float a, int light,
                                float nx, float ny, float nz) {

        //noinspection DuplicatedCode
        vc.vertex(poseMat, x0, y0, z0).color(r, g, b, a).uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();

        vc.vertex(poseMat, x1, y1, z1).color(r, g, b, a).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();

        //noinspection DuplicatedCode
        vc.vertex(poseMat, x2, y2, z2).color(r, g, b, a).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();

        vc.vertex(poseMat, x3, y3, z3).color(r, g, b, a).uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normalMat, nx, ny, nz).endVertex();
    }
}
