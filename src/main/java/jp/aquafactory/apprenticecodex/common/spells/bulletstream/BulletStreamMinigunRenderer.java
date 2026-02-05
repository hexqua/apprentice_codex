package jp.aquafactory.apprenticecodex.common.spells.bulletstream;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.common.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

// todo:GeckoLibを使って砲身回転を対応する.
public class BulletStreamMinigunRenderer extends EntityRenderer<BulletStreamMinigunEntity> {
    private static final RandomSource RNG = RandomSource.create();
    private final ItemStack renderItem = new ItemStack(ItemRegistry.BULLET_STREAM_MINIGUN.get());

    public BulletStreamMinigunRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull BulletStreamMinigunEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.translate(0.0, -0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        // モデルは180度回転させる必要がある.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // リコイル表現はシンプルに.
        if (entity.getIsRecoilTick()) {
            // 同一tickであればランダムにブレないように.
            RNG.setSeed(entity.tickCount + entity.getId());
            var randomPitch = (RNG.nextFloat() * 6f - 3f) * (1 - partialTicks);
            var randomYaw = (RNG.nextFloat() * 2f - 1f) * (1 - partialTicks);
            poseStack.mulPose(Axis.XP.rotationDegrees(randomPitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(randomYaw));
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull BulletStreamMinigunEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}

