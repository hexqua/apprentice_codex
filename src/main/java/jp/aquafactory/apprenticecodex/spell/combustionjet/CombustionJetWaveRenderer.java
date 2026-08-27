package jp.aquafactory.apprenticecodex.spell.combustionjet;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public final class CombustionJetWaveRenderer extends EntityRenderer<CombustionJetWaveEntity> {
    public CombustionJetWaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            @NotNull CombustionJetWaveEntity entity,
            float entityYaw,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight
    ) {
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CombustionJetWaveEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
