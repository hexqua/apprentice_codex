package jp.aquafactory.apprenticecodex.spell.gracedrain;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class GracedRainCloudRenderer extends EntityRenderer<GracedRainCloudEntity> {

    public GracedRainCloudRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GracedRainCloudEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
