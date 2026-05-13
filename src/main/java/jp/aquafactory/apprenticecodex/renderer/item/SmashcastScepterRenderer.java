package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.model.SmashcastScepterModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public final class SmashcastScepterRenderer extends GeoItemRenderer<SmashcastScepter> {
    private static final String HIP_CORE_BONE = "hip_core";
    private static final String HEAD_EMISSIVE_BONE = "head_emissive";
    private static final String RUNE_BONE = "rune";
    private static final String SHELL_BONE = "shell";
    private static final String IMBUE_CORE_BONE = "imbue_core";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/smashcast_scepter.png");
    private static final RenderType EMISSIVE_RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);
    private static final RenderType CUTOUT_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    private RenderPass renderPass = RenderPass.NONE;

    public SmashcastScepterRenderer() {
        super(new SmashcastScepterModel());
    }

    @Override
    public void postRender(PoseStack poseStack, SmashcastScepter animatable, BakedGeoModel model,
                            MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                            int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);

        if (isReRender) {
            return;
        }

        float hipCoreBrightness = resolvePulseBrightness(partialTick, 0.5F, 0.1F, 60.0F);
        renderEmissivePass(model, poseStack, bufferSource, animatable, RenderPass.HIP_CORE, partialTick,
                rgba(hipCoreBrightness, hipCoreBrightness, hipCoreBrightness, alpha(colour)));

        float headBrightness = resolvePulseBrightness(partialTick, 0.9F, 0.1F, 50.0F);
        renderEmissivePass(model, poseStack, bufferSource, animatable, RenderPass.HEAD_EMISSIVE, partialTick,
                rgba(headBrightness, headBrightness, headBrightness, alpha(colour)));

        var imbueCoreColor = resolveImbueCoreColor(animatable, partialTick);
        renderEmissivePass(model, poseStack, bufferSource, animatable, RenderPass.IMBUE_CORE, partialTick,
                rgba(imbueCoreColor.red(), imbueCoreColor.green(), imbueCoreColor.blue(), alpha(colour)));

        renderCutoutPass(model, poseStack, bufferSource, animatable, RenderPass.RUNE, partialTick, packedLight,
                packedOverlay, colour);
        renderCutoutPass(model, poseStack, bufferSource, animatable, RenderPass.SHELL, partialTick, packedLight,
                packedOverlay, colour);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, SmashcastScepter animatable, GeoBone bone, RenderType renderType,
                                   MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                   float partialTick, int packedLight, int packedOverlay, int colour) {
        var hipCoreBone = isBoneOrChildOf(bone, HIP_CORE_BONE);
        var headEmissiveBone = isBoneOrChildOf(bone, HEAD_EMISSIVE_BONE);
        var runeBone = isBoneOrChildOf(bone, RUNE_BONE);
        var shellBone = isBoneOrChildOf(bone, SHELL_BONE);
        var imbueCoreBone = isBoneOrChildOf(bone, IMBUE_CORE_BONE);
        var specialBone = hipCoreBone || headEmissiveBone || runeBone || shellBone || imbueCoreBone;

        if (this.renderPass == RenderPass.NONE && specialBone) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.renderPass == RenderPass.HIP_CORE) {
            renderPassBone(
                    poseStack, animatable, bone, hipCoreBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.renderPass == RenderPass.HEAD_EMISSIVE) {
            renderPassBone(
                    poseStack, animatable, bone, headEmissiveBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.renderPass == RenderPass.RUNE) {
            renderPassBone(
                    poseStack, animatable, bone, runeBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.renderPass == RenderPass.SHELL) {
            renderPassBone(
                    poseStack, animatable, bone, shellBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.renderPass == RenderPass.IMBUE_CORE) {
            renderPassBone(
                    poseStack, animatable, bone, imbueCoreBone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.renderPass = RenderPass.NONE;
    }

    private void renderEmissivePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                    SmashcastScepter animatable, RenderPass pass, float partialTick,
                                    int colour) {
        this.renderPass = pass;
        try {
            // emissive は通常パスと glint から切り離し、FULL_BRIGHT の専用パスだけで描画する。
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    EMISSIVE_RENDER_TYPE,
                    bufferSource.getBuffer(EMISSIVE_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    colour
            );
        } finally {
            this.renderPass = RenderPass.NONE;
        }
    }

    private void renderCutoutPass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                  SmashcastScepter animatable, RenderPass pass, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        this.renderPass = pass;
        try {
            var currentStack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
            var cutoutBuffer = ItemRenderer.getFoilBufferDirect(
                    bufferSource,
                    CUTOUT_RENDER_TYPE,
                    this.renderPerspective == ItemDisplayContext.GUI,
                    currentStack.hasFoil()
            );
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    CUTOUT_RENDER_TYPE,
                    cutoutBuffer,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    colour
            );
        } finally {
            this.renderPass = RenderPass.NONE;
        }
    }

    private void renderPassBone(PoseStack poseStack, SmashcastScepter animatable, GeoBone bone, boolean targetBone,
                                RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                int colour) {
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

    private void renderChildBonesOnly(PoseStack poseStack, SmashcastScepter animatable, GeoBone bone,
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
                poseStack,
                animatable,
                bone,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                colour
        );
        poseStack.popPose();
    }

    private GlowColor resolveImbueCoreColor(SmashcastScepter animatable, float partialTick) {
        var stack = this.currentItemStack != null ? this.currentItemStack : ItemStack.EMPTY;
        float brightness;
        if (isImbuedSpellOnCooldown(animatable, stack)) {
            brightness = resolvePulseBrightness(partialTick, 0.7F, 0.15F, 20.0F);
            return new GlowColor(0.75F * brightness, 0.1F * brightness, 0.08F * brightness);
        }

        brightness = resolvePulseBrightness(partialTick, 0.95F, 0.05F, 20.0F);
        return new GlowColor(brightness, brightness, brightness);
    }

    private static boolean isImbuedSpellOnCooldown(SmashcastScepter animatable, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SmashcastScepter)) {
            return false;
        }

        var spellData = animatable.getImbuedSpellData(stack);
        if (spellData == null || spellData == SpellData.EMPTY) {
            return false;
        }

        return ClientMagicData.getCooldowns().isOnCooldown(spellData.getSpell());
    }

    private static float resolvePulseBrightness(float partialTick, float center, float amplitude, float periodTicks) {
        float time = resolveRenderTime(partialTick);
        return center + amplitude * Mth.sin(time * Mth.TWO_PI / periodTicks);
    }

    private static float resolveRenderTime(float partialTick) {
        var level = Minecraft.getInstance().level;
        return level == null ? partialTick : level.getGameTime() + partialTick;
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private static float alpha(int colour) {
        return ((colour >>> 24) & 0xFF) / 255.0F;
    }

    private static int rgba(float red, float green, float blue, float alpha) {
        return (Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24)
                | (Mth.clamp(Math.round(red * 255.0F), 0, 255) << 16)
                | (Mth.clamp(Math.round(green * 255.0F), 0, 255) << 8)
                | Mth.clamp(Math.round(blue * 255.0F), 0, 255);
    }

    private record GlowColor(float red, float green, float blue) {
    }

    private enum RenderPass {
        NONE,
        HIP_CORE,
        HEAD_EMISSIVE,
        RUNE,
        SHELL,
        IMBUE_CORE
    }
}
