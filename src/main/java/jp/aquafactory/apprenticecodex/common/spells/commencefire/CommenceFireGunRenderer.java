package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class CommenceFireGunRenderer  extends EntityRenderer<CommenceFireGunEntity> {
    private final ItemStack renderItem = new ItemStack(Items.CROSSBOW);

    public CommenceFireGunRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(CommenceFireGunEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        // todo:モデル描画にする(今はアーチャーオプションのアイテム描画を使いまわし)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // 左上に発射口が向いているアイテムが先端を向くように調整.
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(+45.0f));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                renderItem,
                ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CommenceFireGunEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
