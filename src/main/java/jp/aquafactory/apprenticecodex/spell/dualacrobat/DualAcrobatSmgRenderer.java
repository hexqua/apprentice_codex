package jp.aquafactory.apprenticecodex.spell.dualacrobat;

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

public class DualAcrobatSmgRenderer extends EntityRenderer<DualAcrobatSmgEntity> {
    private static final float RECOIL_DURATION_TICKS = 3.0f;
    private static final float RECOIL_DISTANCE = 0.06f;
    private static final float RECOIL_PITCH_DEGREES = -4.0f;

    private final ItemStack renderItem = new ItemStack(ItemRegistry.DUAL_ACROBAT_SMG.get());

    public DualAcrobatSmgRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull DualAcrobatSmgEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        renderSmg(entity, partialTicks, poseStack, buffer, packedLight, true);
        renderSmg(entity, partialTicks, poseStack, buffer, packedLight, false);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DualAcrobatSmgEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    private void renderSmg(DualAcrobatSmgEntity entity, float partialTicks, PoseStack poseStack,
                           MultiBufferSource buffer, int packedLight, boolean rightSide) {
        var sideSign = rightSide ? 1.0 : -1.0;
        var sideOffset = DualAcrobatSmgEntity.calculateSideOffset(entity.getFormationYaw(), sideSign);
        var yawPitch = RotationTools.calculateYawPitchByEntity(entity, partialTicks);

        poseStack.pushPose();
        poseStack.translate(sideOffset.x, sideOffset.y + DualAcrobatSmgEntity.RENDER_Y_OFFSET, sideOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawPitch.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(yawPitch.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));

        if (entity.getStartupTicksRemaining() > DualAcrobatSmgEntity.STARTUP_SETTLE_TICKS) {
            var spin = (entity.tickCount + partialTicks) * DualAcrobatSmgEntity.SPIN_DEGREES_PER_TICK * (float) sideSign;
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
        } else if (entity.getStartupTicksRemaining() > 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(calculateStartupSettleSpin(entity, partialTicks, (float) sideSign)));
        }

        applyRecoil(entity, partialTicks, poseStack, rightSide);

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
    }

    private void applyRecoil(DualAcrobatSmgEntity entity, float partialTicks, PoseStack poseStack, boolean rightSide) {
        var recoilTicks = entity.getRecoilTicks(rightSide);
        if (recoilTicks <= 0) {
            return;
        }

        var recoil = Math.max(0.0f, (recoilTicks - partialTicks) / RECOIL_DURATION_TICKS);
        poseStack.mulPose(Axis.XP.rotationDegrees(RECOIL_PITCH_DEGREES * recoil));
        poseStack.translate(0.0f, 0.0f, -RECOIL_DISTANCE * recoil);
    }

    private float calculateStartupSettleSpin(DualAcrobatSmgEntity entity, float partialTicks, float sideSign) {
        var remaining = entity.getStartupTicksRemaining();
        var elapsed = DualAcrobatSmgEntity.STARTUP_SETTLE_TICKS - remaining + partialTicks;
        if (elapsed >= DualAcrobatSmgEntity.STARTUP_SETTLE_TICKS) {
            return 0.0f;
        }

        var progress = Mth.clamp(elapsed / DualAcrobatSmgEntity.STARTUP_SETTLE_TICKS, 0.0f, 1.0f);
        var spin = Mth.lerp(progress, entity.getStartupSettleSpinDegrees(), 360.0f);
        return spin * sideSign;
    }
}
