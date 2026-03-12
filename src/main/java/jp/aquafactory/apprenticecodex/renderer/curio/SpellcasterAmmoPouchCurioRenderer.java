package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class SpellcasterAmmoPouchCurioRenderer implements ICurioRenderer {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float HIP_OFFSET_X = 3.4F * PIXEL;
    private static final float HIP_OFFSET_Y = 2.25F * PIXEL;
    private static final float HIP_OFFSET_Z = 0.5F * PIXEL;
    private static final float ARMORED_OFFSET_X = 0.35F * PIXEL;
    private static final float POUCH_SCALE = 1.35F;

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
        var armorOffset = entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0F : ARMORED_OFFSET_X;

        poseStack.pushPose();
        humanoidModel.body.translateAndRotate(poseStack);
        // 胴体基準で右腰寄りに固定し、しゃがみ時も胴体アニメーションへ追従させる。
        poseStack.translate(HIP_OFFSET_X + armorOffset, HIP_OFFSET_Y, HIP_OFFSET_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(8.0F));
        poseStack.scale(POUCH_SCALE, POUCH_SCALE, POUCH_SCALE);
        itemRenderer.renderStatic(itemStack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                poseStack, renderTypeBuffer, entity.level(), entity.getId());
        poseStack.popPose();
    }
}
