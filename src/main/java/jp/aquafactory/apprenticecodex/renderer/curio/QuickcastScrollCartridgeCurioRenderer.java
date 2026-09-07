package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import jp.aquafactory.apprenticecodex.renderer.item.QuickcastScrollCartridgeRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

@OnlyIn(Dist.CLIENT)
public class QuickcastScrollCartridgeCurioRenderer implements ICurioRenderer {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float BACK_OFFSET_X = 0F * PIXEL;
    private static final float BACK_OFFSET_Y = 4F * PIXEL;
    private static final float BACK_OFFSET_Z = 4F * PIXEL;
    private static final float BACK_ROTATE_DEG_Z = 0F;
    private static final float BELT_OFFSET_X = 0F * PIXEL;
    private static final float BELT_OFFSET_Y = 5F * PIXEL;
    private static final float BELT_OFFSET_Z = 4F * PIXEL;
    private static final float BELT_ROTATE_DEG_Z = 90F;
    private static final float ARMORED_OFFSET_Z = 0.45F * PIXEL;
    private static final float CARTRIDGE_SCALE = 0.75F;

    private final QuickcastScrollCartridgeRenderer itemRenderer = new QuickcastScrollCartridgeRenderer();

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
        // 移動を先に行い、回転を変更しても装備位置の調整値が変わらないようにする。
        poseStack.translate(offsetX, offsetY, offsetZ + armorOffset);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotateDegZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
        poseStack.scale(CARTRIDGE_SCALE, CARTRIDGE_SCALE, CARTRIDGE_SCALE);
        // 通常アイコンのモデルを経由せず、装備時だけ GeckoLib を描画する。
        // ItemRenderer が行っていた原点補正を維持し、装備位置の調整値を変えない。
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        itemRenderer.renderByItem(itemStack, ItemDisplayContext.NONE, poseStack, renderTypeBuffer,
                light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
