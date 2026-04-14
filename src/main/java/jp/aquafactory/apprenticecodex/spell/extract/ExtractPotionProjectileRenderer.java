package jp.aquafactory.apprenticecodex.spell.extract;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationFluidEffectTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;

public class ExtractPotionProjectileRenderer extends EntityRenderer<ExtractPotionProjectileEntity> {
    private static final float AMP_DEG = 2.0f;
    private static final float FREQ = 0.35f;
    private static final float OUTER_CUBE_SIZE = 0.26F;
    private static final float INNER_CUBE_SCALE = 0.62F;
    private static final RenderType WATER_CUBE_RENDER_TYPE = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);

    public ExtractPotionProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull ExtractPotionProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var potionStack = entity.getItem();
        if (potionStack.isEmpty()) {
            renderFallbackItem(entity, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        var waterSprite = resolveWaterSprite();
        if (waterSprite == null) {
            renderFallbackItem(entity, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        var motion = entity.getDeltaMovement();
        var horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        var tick = entity.tickCount + partialTicks;
        var seed = (entity.getId() & 1023) * 0.17f;

        float yawDeg;
        float pitchDeg;
        if (horizontal < 1.0e-5 && Math.abs(motion.y) < 1.0e-5) {
            yawDeg = entityYaw;
            pitchDeg = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        } else {
            // x/zなので.
            //noinspection SuspiciousNameCombination
            yawDeg = (float) (Mth.atan2(motion.x, motion.z) * (180 / (float) Math.PI));
            pitchDeg = (float) (Mth.atan2(motion.y, horizontal) * (180 / (float) Math.PI));
        }

        var speed = Math.round(motion.length());
        var damping = Mth.clamp(speed * 3.0f, 0.0f, 1.0f);
        var swayYaw = Mth.sin(tick * FREQ + seed) * AMP_DEG * damping;
        var swayPitch = Mth.cos(tick * (FREQ * 0.9f) + seed) * (AMP_DEG * 0.6f) * damping;
        var color = potionStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor();
        var red = (color >> 16) & 0xFF;
        var green = (color >> 8) & 0xFF;
        var blue = color & 0xFF;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg + swayYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg + swayPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees((tick * 10.0F) % 360.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees((tick * 7.0F) % 360.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees((tick * 13.0F) % 360.0F));

        var cubeBuffer = buffer.getBuffer(WATER_CUBE_RENDER_TYPE);
        drawTexturedCube(poseStack, cubeBuffer, waterSprite, OUTER_CUBE_SIZE * INNER_CUBE_SCALE * 0.5F,
                255, 255, 255, 220);
        drawTexturedCube(poseStack, cubeBuffer, waterSprite, OUTER_CUBE_SIZE * 0.5F,
                red, green, blue, (int) (AtelierStationFluidEffectTuning.WATER_ALPHA * 255.0f));
        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderFallbackItem(ExtractPotionProjectileEntity entity, float partialTicks, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getItem(),
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ExtractPotionProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static TextureAtlasSprite resolveWaterSprite() {
        var stillTexture = IClientFluidTypeExtensions.of(Fluids.WATER)
                .getStillTexture(new FluidStack(Fluids.WATER, 1000));
        if (stillTexture == null) {
            return null;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    private static void drawTexturedCube(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, float half,
                                         int red, int green, int blue, int alpha) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var u0 = sprite.getU0();
        var u1 = sprite.getU1();
        var v0 = sprite.getV0();
        var v1 = sprite.getV1();

        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, -half, half), new Vec3(half, -half, half),
                new Vec3(half, half, half), new Vec3(-half, half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 0.0d, 1.0d), red, green, blue, alpha);
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(half, -half, -half), new Vec3(-half, -half, -half),
                new Vec3(-half, half, -half), new Vec3(half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 0.0d, -1.0d), red, green, blue, alpha);
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, -half, -half), new Vec3(-half, -half, half),
                new Vec3(-half, half, half), new Vec3(-half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(-1.0d, 0.0d, 0.0d), red, green, blue, alpha);
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(half, -half, half), new Vec3(half, -half, -half),
                new Vec3(half, half, -half), new Vec3(half, half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(1.0d, 0.0d, 0.0d), red, green, blue, alpha);
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, half, half), new Vec3(half, half, half),
                new Vec3(half, half, -half), new Vec3(-half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 1.0d, 0.0d), red, green, blue, alpha);
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, -half, -half), new Vec3(half, -half, -half),
                new Vec3(half, -half, half), new Vec3(-half, -half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, -1.0d, 0.0d), red, green, blue, alpha);
    }

    private static void face(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                             Vec3 normal, int red, int green, int blue, int alpha) {
        vertex(buffer, poseMatrix, normalMatrix, p0, u0, v0, normal, red, green, blue, alpha);
        vertex(buffer, poseMatrix, normalMatrix, p1, u1, v1, normal, red, green, blue, alpha);
        vertex(buffer, poseMatrix, normalMatrix, p2, u2, v2, normal, red, green, blue, alpha);
        vertex(buffer, poseMatrix, normalMatrix, p3, u3, v3, normal, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position,
                               float u, float v, Vec3 normal, int red, int green, int blue, int alpha) {
        buffer.addVertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
