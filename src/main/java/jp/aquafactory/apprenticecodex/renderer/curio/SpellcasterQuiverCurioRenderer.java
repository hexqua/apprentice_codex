package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@OnlyIn(Dist.CLIENT)
public class SpellcasterQuiverCurioRenderer implements ICurioRenderer {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float BACK_OFFSET_X = -2.8F * PIXEL;
    private static final float BACK_OFFSET_Y = 2.85F * PIXEL;
    private static final float BACK_OFFSET_Z = -2.9F * PIXEL;
    private static final float BACK_ROTATE_DEG_Z = 214F;
    private static final float BELT_OFFSET_X = -10F * PIXEL;
    private static final float BELT_OFFSET_Y = 5F * PIXEL;
    private static final float BELT_OFFSET_Z = -2.7F * PIXEL;
    private static final float BELT_ROTATE_DEG_Z = 260F;
    private static final float ARMORED_OFFSET_Z = -0.45F * PIXEL;
    private static final float QUIVER_SCALE = 1.45F;

    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack itemStack, SlotContext slotContext,
                                                                          PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent,
                                                                          MultiBufferSource renderTypeBuffer, int light, float limbSwing,
                                                                          float limbSwingAmount, float partialTicks, float ageInTicks,
                                                                          float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> rawHumanoidModel)) {
            return;
        }

        var entity = slotContext.entity();
        if (entity == null) {
            return;
        }

        var humanoidModel = (HumanoidModel<LivingEntity>) rawHumanoidModel;
        var armorOffset = entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0F : ARMORED_OFFSET_Z;
        float offsetX;
        float offsetY;
        float offsetZ;
        float rotateDegZ;
        switch (slotContext.identifier()) {
            case CuriosSlotConstants.BACK -> {
                offsetX = BACK_OFFSET_X;
                offsetY = BACK_OFFSET_Y;
                offsetZ = BACK_OFFSET_Z;
                rotateDegZ = BACK_ROTATE_DEG_Z;
            }
            case CuriosSlotConstants.BELT -> {
                offsetX = BELT_OFFSET_X;
                offsetY = BELT_OFFSET_Y;
                offsetZ = BELT_OFFSET_Z;
                rotateDegZ = BELT_ROTATE_DEG_Z;
            }
            default -> {
                return;
            }
        }

        poseStack.pushPose();
        humanoidModel.body.translateAndRotate(poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotateDegZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        poseStack.translate(offsetX, offsetY, offsetZ + armorOffset);
        poseStack.scale(QUIVER_SCALE, QUIVER_SCALE, QUIVER_SCALE);
        itemRenderer.renderStatic(itemStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                poseStack, renderTypeBuffer, entity.level(), entity.getId());
        poseStack.popPose();
    }
}
