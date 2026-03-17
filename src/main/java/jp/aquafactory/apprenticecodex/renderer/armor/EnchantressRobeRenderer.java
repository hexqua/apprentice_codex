package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import jp.aquafactory.apprenticecodex.item.armor.EnchantressRobeItem;
import jp.aquafactory.apprenticecodex.model.EnchantressRobeModel;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class EnchantressRobeRenderer extends GeoArmorRenderer<EnchantressRobeItem> {
    static final String RUNE_TINT_RIGHT_BONE = "rune_tint_right";
    static final String RUNE_TINT_LEFT_BONE = "rune_tint_left";

    private int runeColour = 0xFFFFFFFF;
    private boolean renderRunes;

    public EnchantressRobeRenderer() {
        super(new EnchantressRobeModel());
        addRenderLayer(new EnchantressRobeGlowLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, EnchantressRobeItem animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        updateRuneRenderState();
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, int colour) {
        if (isRuneBoneName(bone.getName())) {
            if (!this.renderRunes) {
                return;
            }

            colour = this.runeColour;
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        this.runeColour = 0xFFFFFFFF;
        this.renderRunes = false;
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

    static int resolveSchoolTintColor(SchoolType schoolType) {
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
