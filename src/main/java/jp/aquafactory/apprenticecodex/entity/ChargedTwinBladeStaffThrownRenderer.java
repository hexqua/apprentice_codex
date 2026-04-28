package jp.aquafactory.apprenticecodex.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

public final class ChargedTwinBladeStaffThrownRenderer extends EntityRenderer<ChargedTwinBladeStaffThrownEntity> {
    public ChargedTwinBladeStaffThrownRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ChargedTwinBladeStaffThrownEntity entity,
            float entityYaw,
            float partialTicks,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight
    ) {
        var yawPitch = entity.resolveRenderYawPitch(partialTicks);
        var renderLight = (entity.isImpacted() || entity.isClientPredictingBlockImpact())
                ? resolveImpactPackedLight(entity, packedLight)
                : packedLight;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getRenderStack(),
                ItemDisplayContext.FIXED,
                renderLight,
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull ChargedTwinBladeStaffThrownEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static int resolveImpactPackedLight(ChargedTwinBladeStaffThrownEntity entity, int packedLight) {
        var basePos = entity.blockPosition();
        var brightest = packedLight;

        brightest = maxPackedLight(brightest, LevelRenderer.getLightColor(entity.level(), basePos));
        for (var direction : Direction.values()) {
            brightest = maxPackedLight(brightest, LevelRenderer.getLightColor(entity.level(), basePos.relative(direction)));
        }

        return brightest;
    }

    private static int maxPackedLight(int first, int second) {
        return LightTexture.pack(
                Math.max(LightTexture.block(first), LightTexture.block(second)),
                Math.max(LightTexture.sky(first), LightTexture.sky(second))
        );
    }
}
