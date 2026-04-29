package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeEffectState;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class FocusStaffbowChargeEffectRenderer {
    private static final ResourceLocation RHOMBUS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/particle/glow_rhombus.png");
    private static final ResourceLocation SPARK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/particle/glow_circle.png");
    private static final RenderType RHOMBUS_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("focus_staffbow_charge_rhombus_additive_color_only", RHOMBUS_TEXTURE);
    private static final RenderType SPARK_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("focus_staffbow_charge_spark_additive_color_only", SPARK_TEXTURE);

    private static final float RHOMBUS_SIZE_AT_1X = 0.15F;
    private static final float RHOMBUS_SIZE_AT_3X = 0.3F;
    private static final float RHOMBUS_ALPHA = 0.85F;
    private static final int RHOMBUS_WHITEN_TICKS = 4;
    private static final int RHOMBUS_LIFETIME_TICKS = 12;
    private static final float RHOMBUS_SPAWNS_PER_TICK = 0.9F;
    private static final float SPARK_SPAWN_RADIUS = 1.5F;
    private static final int SPARK_CONVERGE_TICKS = 15;
    private static final int SPARKS_PER_TICK = 8;
    private static final float SPARK_SIZE = 0.05F;
    private static final float SPARK_ALPHA = 0.75F;

    private FocusStaffbowChargeEffectRenderer() {
    }

    static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       FocusStaffbowChargeEffectState state, float partialTick) {
        if (!state.visible()) {
            return;
        }

        var color = resolveSchoolColor(state.spellId());
        var rhombusSize = resolveRhombusSize(state) * state.longRampProgress();
        if (rhombusSize > 0.001F) {
            renderRhombuses(poseStack, bufferSource.getBuffer(RHOMBUS_RENDER_TYPE), state, partialTick, rhombusSize, color);
        }

        renderSparks(poseStack, bufferSource.getBuffer(SPARK_RENDER_TYPE), state, partialTick);
    }

    private static void renderRhombuses(PoseStack poseStack, VertexConsumer buffer,
                                        FocusStaffbowChargeEffectState state, float partialTick,
                                        float baseSize, EffectColor color) {
        var age = state.elapsedTicks() + partialTick;
        var currentTick = Mth.floor(age);
        var firstBirthTick = Math.max(0, currentTick - RHOMBUS_LIFETIME_TICKS + 1);
        for (int birthTick = firstBirthTick; birthTick <= currentTick; birthTick++) {
            var progress = Mth.clamp((age - birthTick) / RHOMBUS_LIFETIME_TICKS, 0.0F, 1.0F);
            if (progress >= 1.0F) {
                continue;
            }

            var spawnCount = getRhombusSpawnCount(birthTick);
            for (int index = 0; index < spawnCount; index++) {
                renderRhombus(poseStack, buffer, state, birthTick, index, progress, baseSize, color);
            }
        }
    }

    private static void renderRhombus(PoseStack poseStack, VertexConsumer buffer, FocusStaffbowChargeEffectState state,
                                      int birthTick, int index, float progress, float baseSize, EffectColor schoolColor) {
        var tint = mixRhombusColorFromWhite(schoolColor, progress);
        var fade = 1.0F - easeInCubic(progress);
        var alphaJitter = Mth.lerp(noise(state.startedGameTime(), birthTick, index, 29), 0.78F, 1.08F);
        var alpha = RHOMBUS_ALPHA * fade * alphaJitter * state.longRampProgress();
        if (alpha <= 0.01F) {
            return;
        }

        var sizeJitter = Mth.lerp(noise(state.startedGameTime(), birthTick, index, 31), 0.82F, 1.16F);
        var size = baseSize * sizeJitter;
        var center = createRhombusOffset(state, birthTick, index, baseSize);
        var roll = noise(state.startedGameTime(), birthTick, index, 37) * Mth.TWO_PI
                + progress * Mth.TWO_PI * Mth.lerp(noise(state.startedGameTime(), birthTick, index, 41), -0.28F, 0.28F);
        drawCrossPlanes(poseStack, buffer, center, size, size, roll, tint.red(), tint.green(), tint.blue(), alpha);
    }

    private static void renderSparks(PoseStack poseStack, VertexConsumer buffer,
                                     FocusStaffbowChargeEffectState state, float partialTick) {
        var age = state.elapsedTicks() + partialTick;
        var currentTick = Mth.floor(age);
        var firstBirthTick = Math.max(0, currentTick - SPARK_CONVERGE_TICKS + 1);
        for (int birthTick = firstBirthTick; birthTick <= currentTick; birthTick++) {
            var progress = Mth.clamp((age - birthTick) / SPARK_CONVERGE_TICKS, 0.0F, 1.0F);
            if (progress >= 1.0F) {
                continue;
            }

            for (int index = 0; index < SPARKS_PER_TICK; index++) {
                renderSpark(poseStack, buffer, state, birthTick, index, progress);
            }
        }
    }

    private static void renderSpark(PoseStack poseStack, VertexConsumer buffer, FocusStaffbowChargeEffectState state,
                                    int birthTick, int index, float progress) {
        var initialOffset = createSparkOffset(state, birthTick, index);
        var easedProgress = easeOutCubic(progress);
        var position = initialOffset.scale(1.0F - easedProgress);
        var color = Mth.clamp(progress, 0.0F, 1.0F);
        var alpha = (float) Math.sin(progress * Math.PI) * SPARK_ALPHA;
        if (alpha <= 0.01F) {
            return;
        }

        var size = SPARK_SIZE * (1.0F - progress * 0.35F);
        var roll = noise(state.startedGameTime(), birthTick, index, 17) * Mth.TWO_PI;
        drawCameraBillboard(poseStack, buffer, position, size, size * 0.72F, roll, color, color, color, alpha);
    }

    private static float resolveRhombusSize(FocusStaffbowChargeEffectState state) {
        var progress = (float) ((Mth.clamp(state.chargeMultiplier(), 1.0D, 3.0D) - 1.0D) / 2.0D);
        return Mth.lerp(progress, RHOMBUS_SIZE_AT_1X, RHOMBUS_SIZE_AT_3X);
    }

    private static int getRhombusSpawnCount(int tick) {
        if (tick < 0) {
            return 0;
        }

        return Mth.floor((tick + 1) * RHOMBUS_SPAWNS_PER_TICK) - Mth.floor(tick * RHOMBUS_SPAWNS_PER_TICK);
    }

    private static Vec3 createRhombusOffset(FocusStaffbowChargeEffectState state, int birthTick, int index, float baseSize) {
        var x = noise(state.startedGameTime(), birthTick, index, 43) * 2.0F - 1.0F;
        var y = noise(state.startedGameTime(), birthTick, index, 47) * 2.0F - 1.0F;
        var z = noise(state.startedGameTime(), birthTick, index, 53) * 2.0F - 1.0F;
        var direction = new Vec3(x, y, z);
        if (direction.lengthSqr() < 1.0e-4D) {
            direction = new Vec3(0.0D, 1.0D, 0.0D);
        }

        var radius = baseSize * Mth.lerp(noise(state.startedGameTime(), birthTick, index, 59), 0.02F, 0.08F);
        return direction.normalize().scale(radius);
    }

    private static Vec3 createSparkOffset(FocusStaffbowChargeEffectState state, int birthTick, int index) {
        var azimuth = hashUnit(state.startedGameTime(), birthTick, index, 3) * Mth.TWO_PI;
        var z = hashUnit(state.startedGameTime(), birthTick, index, 7) * 2.0F - 1.0F;
        var xy = Mth.sqrt(Math.max(0.0F, 1.0F - z * z));
        var direction = new Vec3(Mth.cos(azimuth) * xy, z, Mth.sin(azimuth) * xy);
        var radius = SPARK_SPAWN_RADIUS * Mth.lerp(noise(state.startedGameTime(), birthTick, index, 13), 0.45F, 1.0F);
        return direction.scale(radius);
    }

    private static EffectColor resolveSchoolColor(String spellId) {
        var spell = SpellRegistry.getSpell(spellId);
        var color = MagicTools.resolveSchoolTintColor(spell != null ? spell.getSchoolType() : null);
        return new EffectColor(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        );
    }

    private static EffectColor mixRhombusColorFromWhite(EffectColor schoolColor, float lifeProgress) {
        var colorProgress = RHOMBUS_WHITEN_TICKS <= 0
                ? 1.0F
                : Mth.clamp(lifeProgress * RHOMBUS_LIFETIME_TICKS / RHOMBUS_WHITEN_TICKS, 0.0F, 1.0F);
        return new EffectColor(
                Mth.lerp(colorProgress, 1.0F, schoolColor.red()),
                Mth.lerp(colorProgress, 1.0F, schoolColor.green()),
                Mth.lerp(colorProgress, 1.0F, schoolColor.blue())
        );
    }

    private static void drawCrossPlanes(PoseStack poseStack, VertexConsumer buffer, Vec3 center,
                                        float width, float height, float roll,
                                        float red, float green, float blue, float alpha) {
        var cos = Mth.cos(roll);
        var sin = Mth.sin(roll);
        var rightX = cos * width;
        var rightY = sin * width;
        var upX = -sin * height;
        var upY = cos * height;

        drawDoubleSidedQuad(
                poseStack, buffer,
                center.add(-rightX - upX, -rightY - upY, 0.0D),
                center.add(-rightX + upX, -rightY + upY, 0.0D),
                center.add(rightX + upX, rightY + upY, 0.0D),
                center.add(rightX - upX, rightY - upY, 0.0D),
                red, green, blue, alpha, 0.0F, 0.0F, 1.0F
        );
        drawDoubleSidedQuad(
                poseStack, buffer,
                center.add(-rightX - upX, 0.0D, -rightY - upY),
                center.add(-rightX + upX, 0.0D, -rightY + upY),
                center.add(rightX + upX, 0.0D, rightY + upY),
                center.add(rightX - upX, 0.0D, rightY - upY),
                red, green, blue, alpha, 0.0F, 1.0F, 0.0F
        );
        drawDoubleSidedQuad(
                poseStack, buffer,
                center.add(0.0D, -rightX - upX, -rightY - upY),
                center.add(0.0D, -rightX + upX, -rightY + upY),
                center.add(0.0D, rightX + upX, rightY + upY),
                center.add(0.0D, rightX - upX, rightY - upY),
                red, green, blue, alpha, 1.0F, 0.0F, 0.0F
        );
    }

    private static void drawCameraBillboard(PoseStack poseStack, VertexConsumer buffer, Vec3 center,
                                            float width, float height, float roll,
                                            float red, float green, float blue, float alpha) {
        var cameraRotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        var cameraRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRotation);
        var cameraUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(cameraRotation);
        var cameraFacing = new Vector3f(0.0F, 0.0F, 1.0F).rotate(cameraRotation);

        var inversePoseRotation = new Matrix3f(poseStack.last().pose()).invert();
        cameraRight.mul(inversePoseRotation).normalize();
        cameraUp.mul(inversePoseRotation).normalize();
        cameraFacing.mul(inversePoseRotation).normalize();

        if (Math.abs(roll) > 1.0e-4F) {
            cameraRight.rotateAxis(roll, cameraFacing.x(), cameraFacing.y(), cameraFacing.z());
            cameraUp.rotateAxis(roll, cameraFacing.x(), cameraFacing.y(), cameraFacing.z());
        }

        cameraRight.mul(width);
        cameraUp.mul(height);
        var p0 = center.subtract(cameraRight.x() + cameraUp.x(), cameraRight.y() + cameraUp.y(), cameraRight.z() + cameraUp.z());
        var p1 = center.add(-cameraRight.x() + cameraUp.x(), -cameraRight.y() + cameraUp.y(), -cameraRight.z() + cameraUp.z());
        var p2 = center.add(cameraRight.x() + cameraUp.x(), cameraRight.y() + cameraUp.y(), cameraRight.z() + cameraUp.z());
        var p3 = center.add(cameraRight.x() - cameraUp.x(), cameraRight.y() - cameraUp.y(), cameraRight.z() - cameraUp.z());
        drawDoubleSidedQuad(
                poseStack,
                buffer,
                p0,
                p1,
                p2,
                p3,
                red,
                green,
                blue,
                alpha,
                cameraFacing.x(),
                cameraFacing.y(),
                cameraFacing.z()
        );
    }

    private static void drawDoubleSidedQuad(PoseStack poseStack, VertexConsumer buffer,
                                            Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                            float red, float green, float blue, float alpha,
                                            float normalX, float normalY, float normalZ) {
        drawQuad(poseStack, buffer, p0, p1, p2, p3, red, green, blue, alpha, normalX, normalY, normalZ);
        drawQuad(poseStack, buffer, p3, p2, p1, p0, red, green, blue, alpha, -normalX, -normalY, -normalZ);
    }

    private static void drawQuad(PoseStack poseStack, VertexConsumer buffer,
                                 Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                 float red, float green, float blue, float alpha,
                                 float normalX, float normalY, float normalZ) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        vertex(buffer, poseMatrix, normalMatrix, p0, 0.0F, 1.0F, red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(buffer, poseMatrix, normalMatrix, p1, 0.0F, 0.0F, red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(buffer, poseMatrix, normalMatrix, p2, 1.0F, 0.0F, red, green, blue, alpha, normalX, normalY, normalZ);
        vertex(buffer, poseMatrix, normalMatrix, p3, 1.0F, 1.0F, red, green, blue, alpha, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position,
                               float u, float v, float red, float green, float blue, float alpha,
                               float normalX, float normalY, float normalZ) {
        // 加算合成では alpha だけでなく RGB も落とすと、白飛びを抑えながら密度を調整できる。
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(red * alpha, green * alpha, blue * alpha, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }

    private static float easeOutCubic(float value) {
        var clamped = Mth.clamp(value, 0.0F, 1.0F);
        var inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeInCubic(float value) {
        var clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * clamped;
    }

    private static float noise(long startedGameTime, int birthTick, int index, int salt) {
        var value = Math.sin(startedGameTime * 0.173D + birthTick * 12.9898D + index * 78.233D + salt * 37.719D);
        return (float) (value - Math.floor(value));
    }

    private static float hashUnit(long startedGameTime, int birthTick, int index, int salt) {
        var value = startedGameTime * 0x9E3779B97F4A7C15L
                ^ (long) birthTick * 0xBF58476D1CE4E5B9L
                ^ (long) index * 0x94D049BB133111EBL
                ^ (long) salt * 0xD2B74407B1CE6E93L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (float) ((value >>> 40) & 0xFFFFFFL) / (float) 0x1000000;
    }

    private record EffectColor(float red, float green, float blue) {
    }
}
