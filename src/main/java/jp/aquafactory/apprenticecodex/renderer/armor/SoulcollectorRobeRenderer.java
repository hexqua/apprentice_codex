package jp.aquafactory.apprenticecodex.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.armor.SoulcollectorRobeItem;
import jp.aquafactory.apprenticecodex.model.SoulcollectorRobeModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Soulcollector 固有のルーンだけを常時発光させ、紫系の二色を往復させる。 */
public final class SoulcollectorRobeRenderer extends GeoArmorRenderer<SoulcollectorRobeItem> {
    private static final String RUNE_TINT_RIGHT_BONE = "rune_tint_right";
    private static final String RUNE_TINT_LEFT_BONE = "rune_tint_left";
    private static final int FIRST_RUNE_COLOR = 0xA42FB6;
    private static final int SECOND_RUNE_COLOR = 0x5F1BAF;
    private static final float RUNE_CYCLE_TICKS = 40.0F;

    private int runeColour = 0xFFFFFFFF;
    public SoulcollectorRobeRenderer() {
        super(new SoulcollectorRobeModel());
    }

    @Override
    public void preRender(PoseStack poseStack, SoulcollectorRobeItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        var level = Minecraft.getInstance().level;
        var tick = (level == null ? 0.0F : level.getGameTime()) + partialTick;
        var blend = (Mth.sin(Mth.TWO_PI * tick / RUNE_CYCLE_TICKS) + 1.0F) * 0.5F;
        runeColour = 0xFF000000 | Mth.lerpInt(blend, FIRST_RUNE_COLOR, SECOND_RUNE_COLOR);
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, int colour) {
        if (isRuneBone(bone)) {
            packedLight = LightTexture.FULL_BRIGHT;
            colour = runeColour;
        }
        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);
    }

    @Override
    public void doPostRenderCleanup() {
        super.doPostRenderCleanup();
        runeColour = 0xFFFFFFFF;
    }

    private static boolean isRuneBone(GeoBone bone) {
        return RUNE_TINT_RIGHT_BONE.equals(bone.getName()) || RUNE_TINT_LEFT_BONE.equals(bone.getName());
    }
}
