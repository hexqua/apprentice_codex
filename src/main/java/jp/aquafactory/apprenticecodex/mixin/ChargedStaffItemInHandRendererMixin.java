package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

@Mixin(value = ItemInHandRenderer.class, priority = 900)
public abstract class ChargedStaffItemInHandRendererMixin {
    // 他 MOD の描画変更と競合した場合は、杖のポーズ補正を諦めて起動を優先する。
    @Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I", ordinal = 1),
            require = 0, expect = 0)
    private int apprenticecodex$preferSpinPose(AbstractClientPlayer instance,
            AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
            MultiBufferSource buffer, int combinedLight) {
        // 他 MOD も変更する使用中判定を避け、激流中の杖だけ使用ポーズの分岐を抜ける。
        return player.isAutoSpinAttack() && apprenticecodex$isMainHandStaff(player, hand, stack)
                ? 0 : instance.getUseItemRemainingTicks();
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/UseAnim;"),
            require = 0, expect = 0)
    private UseAnim apprenticecodex$hideMaintenancePose(ItemStack instance, Operation<UseAnim> original,
            AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
            float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack,
            MultiBufferSource buffer, int combinedLight) {
        return apprenticecodex$isMainHandStaff(player, hand, stack) && player.getUsedItemHand() == hand
                && ChargedTwinBladeStaffRiptide.isMaintenanceInput(player)
                ? UseAnim.NONE : original.call(instance);
    }

    @Unique
    private static boolean apprenticecodex$isMainHandStaff(AbstractClientPlayer player, InteractionHand hand,
            ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof ChargedTwinBladeStaff
                && ItemStack.isSameItemSameTags(stack, player.getMainHandItem());
    }
}
