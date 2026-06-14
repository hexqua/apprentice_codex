package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@OnlyIn(Dist.CLIENT)
public class MagiCompressorGadgetCurioRenderer implements ICurioRenderer {
    public static final ModelResourceLocation EQUIPPED_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "item/magi_compressor_gadget_3d"),
            "standalone"
    );

    private static final float PIXEL = 1.0F / 16.0F;
    private static final float HIP_OFFSET_X = 0.0F;
    private static final float HIP_OFFSET_Y = 2.5F * PIXEL;
    private static final float HIP_OFFSET_Z = 3.75F * PIXEL;
    private static final float ARMORED_OFFSET_Z = 0.4F * PIXEL;
    private static final float GADGET_SCALE = 1.2F;

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
        var model = itemRenderer.getItemModelShaper().getModelManager().getModel(EQUIPPED_MODEL);

        poseStack.pushPose();
        humanoidModel.body.translateAndRotate(poseStack);
        poseStack.translate(HIP_OFFSET_X, HIP_OFFSET_Y, HIP_OFFSET_Z + armorOffset);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(GADGET_SCALE, GADGET_SCALE, GADGET_SCALE);
        itemRenderer.render(itemStack, ItemDisplayContext.NONE, false, poseStack, renderTypeBuffer, light,
                OverlayTexture.NO_OVERLAY, model);
        poseStack.popPose();
    }
}
