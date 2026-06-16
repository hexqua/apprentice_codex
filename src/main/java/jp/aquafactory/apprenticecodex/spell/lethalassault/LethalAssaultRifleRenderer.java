package jp.aquafactory.apprenticecodex.spell.lethalassault;

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

public class LethalAssaultRifleRenderer extends EntityRenderer<LethalAssaultRifleEntity> {
    private final ItemStack renderItem = new ItemStack(ItemRegistry.LETHAL_ASSAULT_RIFLE.get());

    public LethalAssaultRifleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LethalAssaultRifleEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        var recoilTick = entity.getRecoilTick();
        var duringRecoil = recoilTick > 0;
        var yawPitch = calculateYawPitchForRecoil(entity, recoilTick, partialTicks);

        poseStack.pushPose();
        poseStack.translate(0.0, -0.2, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        if (duringRecoil) {
            poseStack.translate(0.0, 0.0, calculateRecoilDistance(0.25, recoilTick, partialTicks));
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull LethalAssaultRifleEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static RotationTools.YawPitch calculateYawPitchForRecoil(LethalAssaultRifleEntity entity, int recoilTick, float partialTicks) {
        var rawYawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        if (recoilTick <= 0) {
            return rawYawPitch;
        }

        var restore = Math.min(1.0f, (LethalAssaultRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks) / LethalAssaultRifleEntity.MAX_RECOIL_TICK);
        return new RotationTools.YawPitch(
                Mth.lerp(restore, entity.getFireYaw(), rawYawPitch.yaw()),
                Mth.lerp(restore, entity.getFirePitch(), rawYawPitch.pitch())
        );
    }

    private static float calculateRecoilUpAngle(float angle, float recoilTick, float partialTicks) {
        var recoilAnimationTick = LethalAssaultRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        return angle * Math.max(0.0f, 1 - recoilAnimationTick / LethalAssaultRifleEntity.MAX_RECOIL_TICK);
    }

    private static double calculateRecoilDistance(double recoilDistance, int recoilTick, float partialTicks) {
        var recoilAnimationTick = LethalAssaultRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        return recoilDistance * Math.max(0.0f, 1 - recoilAnimationTick / LethalAssaultRifleEntity.MAX_RECOIL_TICK);
    }
}
