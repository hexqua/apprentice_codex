package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.model.MagiAgentSuitModel;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class MagiAgentSuitRenderer extends GeoArmorRenderer<MagiAgentSuitItem> {
    private static final String RIGHT_RUNE_BONE = "arm_r_rune";
    private static final String LEFT_RUNE_BONE = "arm_l_rune";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/geo/magi_agent_suit.png");
    private static final RenderType RUNE_RENDER_TYPE =
            ApprenticeRenderTypes.entityAdditiveGlowNoCullColorOnly("magi_agent_suit_rune_additive", TEXTURE);

    private RuneRenderPass runeRenderPass = RuneRenderPass.NONE;

    public MagiAgentSuitRenderer() {
        super(new MagiAgentSuitModel());
    }

    @Override
    public void postRender(PoseStack poseStack, MagiAgentSuitItem animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (isReRender || !isCurrentChestplate(animatable)) {
            return;
        }

        renderRunePass(model, poseStack, bufferSource, animatable, RuneRenderPass.RIGHT,
                resolveSchoolColor(MagiAgentSuitItem.getResolvedCalibrationSchool(getCurrentStack())), partialTick);
        renderRunePass(model, poseStack, bufferSource, animatable, RuneRenderPass.LEFT,
                resolveSchoolColor(MagicTools.getImbuedSpellSchool(getCurrentStack())), partialTick);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, MagiAgentSuitItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        boolean rightRuneBone = isBoneOrChildOf(bone, RIGHT_RUNE_BONE);
        boolean leftRuneBone = isBoneOrChildOf(bone, LEFT_RUNE_BONE);

        if (this.runeRenderPass == RuneRenderPass.NONE && (rightRuneBone || leftRuneBone)) {
            renderChildBonesOnly(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (this.runeRenderPass == RuneRenderPass.RIGHT) {
            renderRunePassBone(
                    poseStack, animatable, bone, rightRuneBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        if (this.runeRenderPass == RuneRenderPass.LEFT) {
            renderRunePassBone(
                    poseStack, animatable, bone, leftRuneBone, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        super.renderRecursively(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.runeRenderPass = RuneRenderPass.NONE;
    }

    private void renderRunePass(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                MagiAgentSuitItem animatable, RuneRenderPass pass, @Nullable RuneColor color,
                                float partialTick) {
        if (color == null) {
            return;
        }

        this.runeRenderPass = pass;
        try {
            this.reRender(
                    model,
                    poseStack,
                    bufferSource,
                    animatable,
                    RUNE_RENDER_TYPE,
                    bufferSource.getBuffer(RUNE_RENDER_TYPE),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    color.red(),
                    color.green(),
                    color.blue(),
                    1.0F
            );
        } finally {
            this.runeRenderPass = RuneRenderPass.NONE;
        }
    }

    private void renderRunePassBone(PoseStack poseStack, MagiAgentSuitItem animatable, GeoBone bone,
                                    boolean targetBone, RenderType renderType, MultiBufferSource bufferSource,
                                    VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                                    int packedOverlay, float red, float green, float blue, float alpha) {
        if (targetBone) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        renderChildBonesOnly(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );
    }

    private void renderChildBonesOnly(PoseStack poseStack, MagiAgentSuitItem animatable, GeoBone bone,
                                      RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                      boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations));
        }

        RenderUtils.prepMatrixForBone(poseStack, bone);
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
                red,
                green,
                blue,
                alpha
        );
        poseStack.popPose();
    }

    private boolean isCurrentChestplate(MagiAgentSuitItem animatable) {
        ItemStack stack = getCurrentStack();
        return !stack.isEmpty()
                && stack.getItem() == animatable
                && animatable.getArmorType() == ArmorItem.Type.CHESTPLATE;
    }

    private static @Nullable RuneColor resolveSchoolColor(@Nullable SchoolType school) {
        TextColor color = school == null ? null : school.getDisplayName().getStyle().getColor();
        if (color == null) {
            return null;
        }

        int rgb = color.getValue();
        return new RuneColor(
                ((rgb >> 16) & 0xFF) / 255.0F,
                ((rgb >> 8) & 0xFF) / 255.0F,
                (rgb & 0xFF) / 255.0F
        );
    }

    private static boolean isBoneOrChildOf(GeoBone bone, String rootBoneName) {
        for (GeoBone current = bone; current != null; current = current.getParent()) {
            if (rootBoneName.equals(current.getName())) {
                return true;
            }
        }

        return false;
    }

    private enum RuneRenderPass {
        NONE,
        RIGHT,
        LEFT
    }

    private record RuneColor(float red, float green, float blue) {
    }
}
