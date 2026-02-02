package jp.aquafactory.apprenticecodex.common.spells.quickarms;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class QuickArmsHandgunRenderer extends EntityRenderer<QuickArmsHandgunEntity> {
    private final ItemStack renderItem = new ItemStack(ItemRegistry.QUICK_ARMS_HANDGUN.get());

    private record YawPitch(float yaw, float pitch) {}

    public QuickArmsHandgunRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    public void render(@NotNull QuickArmsHandgunEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        // todo:諸々挙動を盛り込む(今はコメンスファイアほぼそのまま)
        var yawPitch = calculateYawPitch(entity, partialTicks);
        var yaw = yawPitch.yaw();
        var pitch = yawPitch.pitch();

        poseStack.pushPose();
        poseStack.translate(0.0, -0.2, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

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
    public @NotNull ResourceLocation getTextureLocation(@NotNull QuickArmsHandgunEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private static YawPitch calculateYawPitch(Entity entity, float partialTicks) {
        var yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        var pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        return new YawPitch(yaw, pitch);
    }
}
