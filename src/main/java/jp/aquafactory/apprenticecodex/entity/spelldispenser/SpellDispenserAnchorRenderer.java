package jp.aquafactory.apprenticecodex.entity.spelldispenser;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public final class SpellDispenserAnchorRenderer extends EntityRenderer<SpellDispenserAnchorEntity> {
    public SpellDispenserAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            @NotNull SpellDispenserAnchorEntity entity,
            float entityYaw,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight
    ) {
    }

    @Override
    public boolean shouldRender(
            @NotNull SpellDispenserAnchorEntity livingEntity,
            @NotNull net.minecraft.client.renderer.culling.Frustum camera,
            double camX,
            double camY,
            double camZ
    ) {
        return false;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpellDispenserAnchorEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
