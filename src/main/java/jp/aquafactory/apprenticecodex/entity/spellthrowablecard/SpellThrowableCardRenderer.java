package jp.aquafactory.apprenticecodex.entity.spellthrowablecard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

public final class SpellThrowableCardRenderer<T extends AbstractSpellThrowableCardEntity> extends EntityRenderer<T> {
    public SpellThrowableCardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTicks,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight
    ) {
        var yawPitch = entity.resolveRenderYawPitch(partialTicks);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch() + 90.0F));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getCardStack(),
                ItemDisplayContext.FIXED,
                packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
