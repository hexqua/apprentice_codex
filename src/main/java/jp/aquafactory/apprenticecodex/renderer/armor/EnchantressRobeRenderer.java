package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.model.EnchantressRobeModel;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class EnchantressRobeRenderer extends GeoArmorRenderer<EnchantressRobeItem> {
    static final String RUNE_TINT_RIGHT_BONE = "rune_tint_right";
    static final String RUNE_TINT_LEFT_BONE = "rune_tint_left";

    private float runeRed = 1.0f;
    private float runeGreen = 1.0f;
    private float runeBlue = 1.0f;
    private boolean renderRunes;

    public EnchantressRobeRenderer() {
        super(new EnchantressRobeModel<>());
        addRenderLayer(new EnchantressRobeGlowLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, EnchantressRobeItem animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);

        updateRuneRenderState();
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.runeRed = 1.0f;
        this.runeGreen = 1.0f;
        this.runeBlue = 1.0f;
        this.renderRunes = false;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EnchantressRobeItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations));
        }

        if (!isRuneBoneName(bone.getName())) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, red, green, blue, alpha
            );
            return;
        }

        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);

        if (this.renderRunes) {
            renderCubesOfBone(
                    poseStack,
                    bone,
                    buffer,
                    packedLight,
                    packedOverlay,
                    this.runeRed,
                    this.runeGreen,
                    this.runeBlue,
                    alpha
            );
        }

        renderChildBones(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha
        );

        poseStack.popPose();
    }

    private void updateRuneRenderState() {
        this.renderRunes = false;
        this.runeRed = 1.0f;
        this.runeGreen = 1.0f;
        this.runeBlue = 1.0f;

        var schoolType = MagicTools.getImbuedSpellSchool(getCurrentStack());
        if (schoolType == null) {
            return;
        }

        int runeColor = resolveSchoolTintColor(schoolType);
        this.runeRed = ((runeColor >> 16) & 0xFF) / 255.0f;
        this.runeGreen = ((runeColor >> 8) & 0xFF) / 255.0f;
        this.runeBlue = (runeColor & 0xFF) / 255.0f;
        this.renderRunes = true;
    }

    static int resolveSchoolTintColor(SchoolType schoolType) {
        return MagicTools.resolveSchoolTintColor(schoolType);
    }

    private static boolean isRuneBoneName(String boneName) {
        return RUNE_TINT_RIGHT_BONE.equals(boneName) || RUNE_TINT_LEFT_BONE.equals(boneName);
    }
}
