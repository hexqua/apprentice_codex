package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.extrudedsprite.ExtrudedSpriteRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@OnlyIn(Dist.CLIENT)
public class ManaManeuverGearCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "textures/spell/mana_maneuver_gear_parts.png"
    );
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float BODY_WIDTH_PIXELS = 8.0F;
    private static final float INNER_GAP_PIXELS = BODY_WIDTH_PIXELS * 0.8F;
    private static final float SPRITE_SIZE_PIXELS = 8.0F;
    private static final float LEG_PIVOT_X_PIXELS = 2.8F;
    private static final float LOCAL_OFFSET_X_PIXELS =
            (INNER_GAP_PIXELS + SPRITE_SIZE_PIXELS) * 0.5F - LEG_PIVOT_X_PIXELS;
    private static final float LOCAL_OFFSET_Y = 8.0F * PIXEL;
    private static final float LOCAL_OFFSET_Z = 0.2F * PIXEL;
    private static final float SPRITE_SCALE = SPRITE_SIZE_PIXELS * PIXEL;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack itemStack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource renderTypeBuffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> rawHumanoidModel)) {
            return;
        }

        var humanoidModel = (HumanoidModel<LivingEntity>) rawHumanoidModel;
        renderPart(poseStack, renderTypeBuffer, light, humanoidModel.rightLeg, -1.0F);
        renderPart(poseStack, renderTypeBuffer, light, humanoidModel.leftLeg, 1.0F);
    }

    private static void renderPart(PoseStack poseStack, MultiBufferSource buffer, int light,
                                   ModelPart leg, float sideSign) {
        poseStack.pushPose();
        leg.translateAndRotate(poseStack);
        poseStack.translate(sideSign * LOCAL_OFFSET_X_PIXELS * PIXEL, LOCAL_OFFSET_Y, LOCAL_OFFSET_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign < 0.0F ? 90.0F : -90.0F));
        if (sideSign < 0.0F) {
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }
        // 24px の余白込みテクスチャを等倍にすると脚に対して大きすぎるため縮小する。
        poseStack.scale(SPRITE_SCALE, -SPRITE_SCALE, 1.0F);
        ExtrudedSpriteRenderer.renderCenteredWithIndependentRotation(poseStack, buffer, light, TEXTURE);
        poseStack.popPose();
    }
}
