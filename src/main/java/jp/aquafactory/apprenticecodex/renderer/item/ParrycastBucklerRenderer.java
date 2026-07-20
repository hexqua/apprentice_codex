package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ImbuedSpellCoreClientEffectState;
import jp.aquafactory.apprenticecodex.item.shield.ParrycastBuckler;
import jp.aquafactory.apprenticecodex.model.ParrycastBucklerModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class ParrycastBucklerRenderer extends GeoItemRenderer<ParrycastBuckler> {
    private static final String CORE_BONE = "core";
    private static final String SHIELD_BONE = "shield";
    private static final float GLINT_INTENSITY = 0.55F;
    private static final float GLINT_SCROLL_U_PER_TICK = -0.008F;
    private static final float GLINT_SCROLL_V_PER_TICK = 0.016F;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/parrycast_buckler.png"
    );
    private static final ResourceLocation GLINT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/parrycast_buckler_glint.png"
    );
    private static final ResourceLocation COOLDOWN_GLINT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "textures/geo/parrycast_buckler_glint_cooldown.png"
    );
    private static final RenderType DEFAULT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final RenderType CORE_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final RenderType GLINT_RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly(
            "parrycast_buckler_shield_glint", GLINT_TEXTURE
    );
    private static final RenderType COOLDOWN_GLINT_RENDER_TYPE = ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly(
            "parrycast_buckler_shield_glint_cooldown", COOLDOWN_GLINT_TEXTURE
    );

    private SpecialPass specialPass = SpecialPass.NONE;
    private float glintUOffset;
    private float glintVOffset;

    public ParrycastBucklerRenderer() {
        super(new ParrycastBucklerModel());
    }

    @Override
    public long getInstanceId(ParrycastBuckler animatable) {
        return ParrycastBuckler.resolveClientAnimationInstanceId(this.currentItemStack);
    }

    @Override
    public RenderType getRenderType(ParrycastBuckler animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return DEFAULT_RENDER_TYPE;
    }

    @Override
    public void postRender(PoseStack poseStack, ParrycastBuckler animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);

        if (isReRender) {
            return;
        }

        renderCorePass(model, poseStack, bufferSource, animatable, partialTick);
        renderShieldPass(model, poseStack, bufferSource, animatable, partialTick,
                packedLight, packedOverlay, colour);
        renderShieldGlintPass(model, poseStack, bufferSource, animatable, partialTick, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, ParrycastBuckler animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  int colour) {
        boolean coreBone = isBoneOrChildOf(bone, CORE_BONE);
        boolean shieldBone = isBoneOrChildOf(bone, SHIELD_BONE);
        if (this.specialPass == SpecialPass.NONE) {
            if (coreBone || shieldBone) {
                return;
            }

            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        boolean targetBone = switch (this.specialPass) {
            case CORE -> coreBone;
            case SHIELD, SHIELD_GLINT -> shieldBone;
            case NONE -> false;
        };
        if (targetBone) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
                                     int packedLight, int packedOverlay, int colour) {
        if (this.specialPass != SpecialPass.SHIELD_GLINT) {
            super.createVerticesOfQuad(
                    quad, poseState, normal, buffer, packedLight, packedOverlay, colour
            );
            return;
        }

        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            buffer.addVertex(poseState, position.x(), position.y(), position.z())
                    .setColor(colour)
                    .setUv(vertex.texU() + this.glintUOffset, vertex.texV() + this.glintVOffset)
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(normal.x(), normal.y(), normal.z());
        }
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.specialPass = SpecialPass.NONE;
        this.glintUOffset = 0.0F;
        this.glintVOffset = 0.0F;
    }

    private void renderCorePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                ParrycastBuckler animatable, float partialTick) {
        var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
        // WisdomShard の選択魔法ではなく、スタック内の Imbue 魔法のクールダウンだけを参照する。
        var coreState = ImbuedSpellCoreClientEffectState.resolve(currentStack, partialTick);
        this.specialPass = SpecialPass.CORE;
        try {
            this.reRender(
                    model, poseStack, bufferSource, animatable, CORE_RENDER_TYPE,
                    bufferSource.getBuffer(CORE_RENDER_TYPE), partialTick,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    packColour(coreState.red(), coreState.green(), coreState.blue(), coreState.alpha())
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
        }
    }

    private void renderShieldPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                  ParrycastBuckler animatable, float partialTick, int packedLight,
                                  int packedOverlay, int colour) {
        this.specialPass = SpecialPass.SHIELD;
        try {
            this.reRender(
                    model, poseStack, bufferSource, animatable, DEFAULT_RENDER_TYPE,
                    bufferSource.getBuffer(DEFAULT_RENDER_TYPE), partialTick,
                    packedLight, packedOverlay, colour
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
        }
    }

    private void renderShieldGlintPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                       ParrycastBuckler animatable, float partialTick, int colour) {
        float renderTime = resolveRenderTime(partialTick);
        var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
        var glintRenderType = ImbuedSpellCoreClientEffectState.isCooldownActive(currentStack, partialTick)
                ? COOLDOWN_GLINT_RENDER_TYPE
                : GLINT_RENDER_TYPE;
        this.specialPass = SpecialPass.SHIELD_GLINT;
        this.glintUOffset = wrapUnit(renderTime * GLINT_SCROLL_U_PER_TICK);
        this.glintVOffset = wrapUnit(renderTime * GLINT_SCROLL_V_PER_TICK);
        try {
            this.reRender(
                    model, poseStack, bufferSource, animatable, glintRenderType,
                    bufferSource.getBuffer(glintRenderType), partialTick,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    scaleColour(colour, GLINT_INTENSITY)
            );
        } finally {
            this.specialPass = SpecialPass.NONE;
            this.glintUOffset = 0.0F;
            this.glintVOffset = 0.0F;
        }
    }

    private void renderChildBonesOnly(PoseStack poseStack, ParrycastBuckler animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      int colour) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        RenderUtil.prepMatrixForBone(poseStack, bone);
        renderChildBones(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
        poseStack.popPose();
    }

    private static float resolveRenderTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
    }

    private static float wrapUnit(float value) {
        return value - Mth.floor(value);
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static int packColour(float red, float green, float blue, float alpha) {
        var a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        var r = Mth.clamp(Math.round(red * 255.0F), 0, 255);
        var g = Mth.clamp(Math.round(green * 255.0F), 0, 255);
        var b = Mth.clamp(Math.round(blue * 255.0F), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int scaleColour(int colour, float intensity) {
        var alpha = colour >>> 24 & 0xFF;
        var red = Math.round((colour >>> 16 & 0xFF) * intensity);
        var green = Math.round((colour >>> 8 & 0xFF) * intensity);
        var blue = Math.round((colour & 0xFF) * intensity);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private enum SpecialPass {
        NONE,
        CORE,
        SHIELD,
        SHIELD_GLINT
    }
}
