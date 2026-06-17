package jp.aquafactory.apprenticecodex.spell.artisansmash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArtisanSmashLauncherRenderer extends EntityRenderer<ArtisanSmashLauncherEntity> {
    private static final float RECOIL_UP_ANGLE = 12.0f;

    private final ItemStack renderItem = new ItemStack(ItemRegistry.ARTISAN_SMASH_LAUNCHER.get());

    public ArtisanSmashLauncherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull ArtisanSmashLauncherEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.translate(0.0, -0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        if (entity.getIsReleased()) {
            poseStack.translate(0.0, 0.0, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(calculateRecoilUpAngle(entity.getRecoilTick(), partialTicks)));
        }

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
    public @NotNull ResourceLocation getTextureLocation(@NotNull ArtisanSmashLauncherEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static float calculateRecoilUpAngle(float recoilTick, float partialTicks) {
        var recoilAnimationTick = ArtisanSmashLauncherEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        if (recoilAnimationTick < 6.0f) {
            return RECOIL_UP_ANGLE * (1.0f - recoilAnimationTick / 6.0f);
        }

        return 0.0f;
    }
}
