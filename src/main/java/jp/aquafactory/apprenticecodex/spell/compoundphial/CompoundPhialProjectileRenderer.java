package jp.aquafactory.apprenticecodex.spell.compoundphial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

public class CompoundPhialProjectileRenderer extends EntityRenderer<CompoundPhialProjectileEntity> {

    private static final float AMP_DEG = 2.0f;
    private static final float FREQ = 0.35f;

    public CompoundPhialProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull CompoundPhialProjectileEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {

        var motion = entity.getDeltaMovement();
        var vx = motion.x;
        var vy = motion.y;
        var vz = motion.z;
        var horizontal = Math.sqrt(vx * vx + vz * vz);
        var tick = entity.tickCount + partialTicks;
        var seed = (entity.getId() & 1023) * 0.17f;

        float yawDeg;
        float pitchDeg;
        if (horizontal < 1.0e-5 && Math.abs(vy) < 1.0e-5) {
            // 低速の時のフォールバック.
            yawDeg = entityYaw;
            pitchDeg = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        } else {
            yawDeg = (float)(Mth.atan2(vx, vz) * (180 / (float)Math.PI));
            pitchDeg = (float)(Mth.atan2(vy, horizontal) * (180 / (float)Math.PI));
        }

        var speed = Math.round(motion.length());
        var damping = Mth.clamp(speed * 3.0f, 0.0f, 1.0f);
        var swayYaw   = Mth.sin(tick * FREQ + seed) * AMP_DEG * damping;
        var swayPitch = Mth.cos(tick * (FREQ * 0.9f) + seed) * (AMP_DEG * 0.6f) * damping;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg + swayYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg + swayPitch));

        // モデルは180度回転させる必要がある.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        // ItemRendererで描画.
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getPotionItem(),
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull CompoundPhialProjectileEntity pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
