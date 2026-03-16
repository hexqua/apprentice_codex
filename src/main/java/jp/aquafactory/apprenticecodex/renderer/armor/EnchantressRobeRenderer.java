package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.model.EnchantressRobeModel;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class EnchantressRobeRenderer extends GeoArmorRenderer<EnchantressRobeItem> {
    static final String RUNE_TINT_RIGHT_BONE = "rune_tint_right";
    static final String RUNE_TINT_LEFT_BONE = "rune_tint_left";
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    private int runeColour = 0xFFFFFFFF;
    private boolean renderRunes;

    public EnchantressRobeRenderer() {
        super(new EnchantressRobeModel());
    }

    @Override
    public void preRender(PoseStack poseStack, EnchantressRobeItem animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        updateRuneRenderState();
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.runeColour = 0xFFFFFFFF;
        this.renderRunes = false;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, EnchantressRobeItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        if (!isRuneBoneName(bone.getName())) {
            super.renderRecursively(
                    poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour
            );
            return;
        }

        if (this.renderRunes) {
            var emissiveRenderType = RenderType.entityTranslucent(getTextureLocation(animatable));
            var emissiveBuffer = ItemRenderer.getArmorFoilBuffer(
                    bufferSource,
                    emissiveRenderType,
                    this.currentStack != null && this.currentStack.hasFoil()
            );
            super.renderRecursively(
                    poseStack, animatable, bone, emissiveRenderType, bufferSource, emissiveBuffer, isReRender, partialTick,
                    FULL_BRIGHT_LIGHT, packedOverlay, this.runeColour
            );
            return;
        }

        renderChildBones(
                poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour
        );
    }

    private void updateRuneRenderState() {
        this.renderRunes = false;
        this.runeColour = 0xFFFFFFFF;

        var schoolType = MagicTools.getImbuedSpellSchool(getCurrentStack());
        if (schoolType == null) {
            return;
        }

        this.runeColour = 0xFF000000 | resolveSchoolTintColor(schoolType);
        this.renderRunes = true;
    }

    private static int resolveSchoolTintColor(SchoolType schoolType) {
        var color = schoolType.getDisplayName().getStyle().getColor();
        if (color == null) {
            return PastelStaff.DEFAULT_STONE_TINT_COLOR;
        }

        return color.getValue();
    }

    private static boolean isRuneBoneName(String boneName) {
        return RUNE_TINT_RIGHT_BONE.equals(boneName) || RUNE_TINT_LEFT_BONE.equals(boneName);
    }
}
