package jp.aquafactory.apprenticecodex.spell.flyswatter;

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

public class FlySwatterLauncherRenderer extends EntityRenderer<FlySwatterLauncherEntity> {
    private static final float RECOIL_PEAK_TICK = 0.5f;
    private static final float RECOIL_DURATION_TICKS = 3.0f;
    private static final double RECOIL_DISTANCE = 0.18;
    private static final float RECOIL_UP_DEGREES = 5.0f;

    private final ItemStack renderItem = new ItemStack(ItemRegistry.FLY_SWATTER_LAUNCHER.get());

    public FlySwatterLauncherRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public void render(@NotNull FlySwatterLauncherEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);
        poseStack.pushPose();
        poseStack.translate(0.0, -0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));

        // モデルの向き補正前は+Zが発射方向。実座標・照準を変えず、仰角に沿って後退させる。
        var recoil = calculateRecoilAmount(entity.getClientRecoilTicks(partialTicks));
        poseStack.translate(0.0, 0.0, -RECOIL_DISTANCE * recoil);
        poseStack.mulPose(Axis.XP.rotationDegrees(-RECOIL_UP_DEGREES * recoil));

        // モデルは180度回転させる必要がある.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

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
    public @NotNull ResourceLocation getTextureLocation(@NotNull FlySwatterLauncherEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static float calculateRecoilAmount(float age) {
        if (age < 0.0f || age >= RECOIL_DURATION_TICKS) {
            return 0.0f;
        }
        if (age < RECOIL_PEAK_TICK) {
            var progress = age / RECOIL_PEAK_TICK;
            return 1.0f - (1.0f - progress) * (1.0f - progress);
        }

        // 次弾の3tickまでに復帰し、斉射中も一発ずつの後退を読めるようにする。
        var progress = (age - RECOIL_PEAK_TICK) / (RECOIL_DURATION_TICKS - RECOIL_PEAK_TICK);
        return 1.0f - progress * progress * (3.0f - 2.0f * progress);
    }
}
