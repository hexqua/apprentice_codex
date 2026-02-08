package jp.aquafactory.apprenticecodex.common.spells.archermultiple;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.common.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public class ArcherMultipleBowRenderer extends EntityRenderer<ArcherMultipleBowEntity> {

    private final ItemStack renderItem = new ItemStack(ItemRegistry.ARCHER_MULTIPLE_BOW.get());
    private int lastStage = Integer.MIN_VALUE;

    public ArcherMultipleBowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(ArcherMultipleBowEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var stage = entity.getStage();
        if (stage != lastStage) {
            CustomData.update(DataComponents.CUSTOM_DATA, renderItem, tag -> tag.putInt("Stage", stage));
            lastStage = stage;
        }

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

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
    public @NotNull ResourceLocation getTextureLocation(@NotNull ArcherMultipleBowEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
