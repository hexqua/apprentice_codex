package jp.aquafactory.apprenticecodex.renderer.curio;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.ShootingStarMantleClient;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class ShootingStarMantleRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/entity/shooting_star_mantle_elytra.png");
    private final ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA);
    private final ElytraModel<LivingEntity> model = new ElytraModel<>(root);

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot,
            PoseStack pose, RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light,
            float limbSwing, float limbSwingAmount, float partialTicks, float age, float yaw, float pitch) {
        if (!slot.visible() || !(slot.entity() instanceof Player player)) return;
        // 同じ外套のコスメは許可するが、コスメだけの装備では翼を出さない。
        boolean equipped = CuriosApi.getCuriosInventory(player).map(inventory -> {
            var back = inventory.getCurios().get("back");
            return back != null && "back".equals(slot.identifier()) && slot.index() < back.getStacks().getSlots()
                    && back.getStacks().getStackInSlot(slot.index()).getItem() instanceof ShootingStarMantle;
        }).orElse(false);
        if (!equipped) return;
        pose.pushPose();
        pose.translate(0, 0, 0.125);
        model.young = parent.getModel().young;
        model.setupAnim(player, limbSwing, limbSwingAmount, age, yaw, pitch);
        float openAmount = ShootingStarMantleClient.wingOpenAmount(player, partialTicks);
        if (openAmount > 0) {
            // 通常の飛行・しゃがみ姿勢から浮遊の翼へ補間し、解除時も同じ進行度で戻す。
            // fallFlyingやvanillaの共有elytraRotを書き換えず、外套の描画だけに適用する。
            var left = root.getChild("left_wing");
            var right = root.getChild("right_wing");
            left.xRot = Mth.lerp(openAmount, left.xRot, 0.2617994F);
            right.xRot = Mth.lerp(openAmount, right.xRot, 0.2617994F);
            left.zRot = Mth.lerp(openAmount, left.zRot, -1.3089969F);
            right.zRot = Mth.lerp(openAmount, right.zRot, 1.3089969F);
            left.yRot = Mth.lerp(openAmount, left.yRot, 0);
            right.yRot = Mth.lerp(openAmount, right.yRot, 0);
        }
        model.renderToBuffer(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
    }
}
