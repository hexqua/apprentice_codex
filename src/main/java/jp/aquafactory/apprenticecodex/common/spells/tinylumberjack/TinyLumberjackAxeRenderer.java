package jp.aquafactory.apprenticecodex.common.spells.tinylumberjack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
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

public class TinyLumberjackAxeRenderer extends EntityRenderer<TinyLumberjackAxeEntity> {
    private final ItemStack renderItem = new ItemStack(Items.DIAMOND_AXE);

    public TinyLumberjackAxeRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull TinyLumberjackAxeEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.translate(0.0, -0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

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
    public @NotNull ResourceLocation getTextureLocation(@NotNull TinyLumberjackAxeEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
