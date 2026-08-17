package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.broom.BroomDeploymentState;
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
public final class BroomCurioRenderer implements ICurioRenderer {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float OFFSET_X = 1.0F * PIXEL;
    private static final float OFFSET_Y = 5.0F * PIXEL;
    private static final float OFFSET_Z = 4.0F * PIXEL;
    private static final float ARMORED_OFFSET_Z = 0.45F * PIXEL;
    private static final float SCALE = 0.75F;

    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

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
        if (!CuriosSlotConstants.BACK.equals(slotContext.identifier())
                || BroomDeploymentState.isDeployed(itemStack)
                || !(renderLayerParent.getModel() instanceof HumanoidModel<?> rawHumanoidModel)) {
            return;
        }

        var entity = slotContext.entity();
        if (entity == null) {
            return;
        }

        var humanoidModel = (HumanoidModel<LivingEntity>) rawHumanoidModel;
        var armorOffset = entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0F : ARMORED_OFFSET_Z;

        poseStack.pushPose();
        humanoidModel.body.translateAndRotate(poseStack);

        // 先に移動させることで回転の微調整の影響を受けない.
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z + armorOffset);

        // 回転は後に書けて微調整.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-140.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        poseStack.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(
                itemStack,
                ItemDisplayContext.NONE,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                renderTypeBuffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
    }
}
