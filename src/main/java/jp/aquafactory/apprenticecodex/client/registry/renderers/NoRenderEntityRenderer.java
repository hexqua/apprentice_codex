package jp.aquafactory.apprenticecodex.client.registry.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class NoRenderEntityRenderer<T extends Entity> extends EntityRenderer<T> {
    public NoRenderEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight) {
        // do nothing.
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
