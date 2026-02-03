package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.common.registry.ItemRegistry;
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

public class CommenceFireRifleRenderer extends EntityRenderer<CommenceFireRifleEntity> {
    private final ItemStack renderItem = new ItemStack(ItemRegistry.COMMENCE_FIRE_RIFLE.get());

    private record YawPitch(float yaw, float pitch) {}

    public CommenceFireRifleRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(CommenceFireRifleEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        // duringRecoilはサーバー専用のため、クライアントは同期されている方を使う.
        var recoilTick = entity.getRecoilTick();
        var duringRecoil = recoilTick > 0;

        var yawPitch = calculateYawPitchForRestoreRecoil(entity, recoilTick, partialTicks);
        var yaw = yawPitch.yaw();
        var pitch = yawPitch.pitch();

        poseStack.pushPose();
        poseStack.translate(0.0, -0.2, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // モデルは180度回転させる必要がある.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // 反動中であればZ後退させて少し跳ねる.
        if (duringRecoil){
            poseStack.translate(0.0, 0.0, calculateRecoilDistance(0.3, recoilTick, partialTicks));
            poseStack.mulPose(Axis.XP.rotationDegrees(calculateRecoilUpAngle(10, recoilTick, partialTicks)));
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull CommenceFireRifleEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static YawPitch calculateYawPitchForRestoreRecoil(CommenceFireRifleEntity entity, int recoilTick, float partialTicks) {
        var yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        var pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        if(recoilTick <= 0) {
            return new YawPitch(yaw, pitch);
        }

        var recoilAnimationTick = CommenceFireRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        if (recoilAnimationTick < 5) {
            return new YawPitch(entity.getFireYaw(), entity.getFirePitch());
        }

        var v = recoilTick / (float) (CommenceFireRifleEntity.MAX_RECOIL_TICK - 5);
        return new YawPitch(Mth.lerp(v, yaw, entity.getFireYaw()), Mth.lerp(v, pitch, entity.getFirePitch()));

    }

    private static float calculateRecoilUpAngle(float angle, float recoilTick, float partialTicks) {
        var recoilAnimationTick = CommenceFireRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;
        if (recoilAnimationTick < 4) {
            return angle * (1 - recoilAnimationTick / 4);
        }

        return 0f;
    }

    private static double calculateRecoilDistance(double recoilDistance,int recoilTick, float partialTicks) {
        var recoilAnimationTick = CommenceFireRifleEntity.MAX_RECOIL_TICK - recoilTick + partialTicks;

        // 反動は即時反映
        if (recoilAnimationTick < 3) {
            return recoilDistance;
        }

        // 残りはゆっくり元の位置に戻す.
        return recoilDistance * (1 - (recoilAnimationTick - 3) / (float) (CommenceFireRifleEntity.MAX_RECOIL_TICK - 3));
    }
}
