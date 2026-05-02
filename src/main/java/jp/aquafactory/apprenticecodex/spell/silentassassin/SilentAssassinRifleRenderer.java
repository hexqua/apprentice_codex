package jp.aquafactory.apprenticecodex.spell.silentassassin;

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
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SilentAssassinRifleRenderer extends EntityRenderer<SilentAssassinRifleEntity> {
    private final ItemStack renderItem = new ItemStack(ItemRegistry.SILENT_ASSASSIN_RIFLE.get());

    public SilentAssassinRifleRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(SilentAssassinRifleEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var recoilTick = entity.getRecoilTick();
        var duringRecoil = entity.getIsReleased() && recoilTick > 0;
        var yawPitch = calculateYawPitchForRestoreRecoil(entity, recoilTick, partialTicks);

        poseStack.pushPose();
        poseStack.translate(0.0, -0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        // モデルは180度回転させる必要がある。
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        if (duringRecoil) {
            poseStack.mulPose(Axis.XP.rotationDegrees(calculateRecoilUpAngle(8, recoilTick, partialTicks)));
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull SilentAssassinRifleEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static RotationTools.YawPitch calculateYawPitchForRestoreRecoil(SilentAssassinRifleEntity entity, int recoilTick, float partialTicks) {
        var rawYawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        if (recoilTick <= 0) {
            return rawYawPitch;
        }

        var recoilAnimationTick = SilentAssassinRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        if (recoilAnimationTick < 5) {
            return new RotationTools.YawPitch(entity.getFireYaw(), entity.getFirePitch());
        }

        var v = recoilTick / (float) (SilentAssassinRifleEntity.MAX_RECOIL_TICK - 5);
        return new RotationTools.YawPitch(Mth.lerp(v, rawYawPitch.yaw(), entity.getFireYaw()), Mth.lerp(v, rawYawPitch.pitch(), entity.getFirePitch()));
    }

    private static float calculateRecoilUpAngle(float angle, float recoilTick, float partialTicks) {
        var recoilAnimationTick = SilentAssassinRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        if (recoilAnimationTick < 4) {
            return angle * (1 - recoilAnimationTick / 4);
        }

        return 0f;
    }
}
