package jp.aquafactory.apprenticecodex.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptide;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class ChargedStaffItemInHandRendererMixin {
    @Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z", ordinal = 1))
    private boolean apprenticecodex$preferSpinPose(AbstractClientPlayer instance,
            AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
            MultiBufferSource buffer, int combinedLight) {
        // 入力状態によらず激流ポーズを優先し、実際の使用状態は維持する。
        return (!player.isAutoSpinAttack() || !apprenticecodex$isMainHandStaff(player, hand, stack))
                && instance.isUsingItem();
    }

    @Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/UseAnim;"))
    private UseAnim apprenticecodex$hideMaintenancePose(ItemStack instance,
            AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
            MultiBufferSource buffer, int combinedLight) {
        return apprenticecodex$isMainHandStaff(player, hand, stack) && player.getUsedItemHand() == hand
                && ChargedTwinBladeStaffRiptide.isMaintenanceInput(player)
                ? UseAnim.NONE : instance.getUseAnimation();
    }

    @Unique
    private static boolean apprenticecodex$isMainHandStaff(AbstractClientPlayer player, InteractionHand hand,
            ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof ChargedTwinBladeStaff
                && ItemStack.isSameItemSameTags(stack, player.getMainHandItem());
    }
}
