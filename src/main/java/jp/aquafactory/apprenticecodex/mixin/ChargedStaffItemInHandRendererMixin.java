package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptide;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public abstract class ChargedStaffItemInHandRendererMixin {
    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"))
    private boolean apprenticecodex$preferSpinPose(AbstractClientPlayer player, Operation<Boolean> original,
                                                             @Local(argsOnly = true) InteractionHand hand,
                                                             @Local(argsOnly = true) ItemStack stack) {
        // 入力状態によらず激流ポーズを優先し、実際の使用状態は維持する。
        if (player.isAutoSpinAttack() && apprenticecodex$isMainHandStaff(player, hand, stack)) {
            return false;
        }
        return original.call(player);
    }

    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/UseAnim;"))
    private UseAnim apprenticecodex$hideMaintenancePose(ItemStack stack, Operation<UseAnim> original,
                                                       @Local(argsOnly = true) AbstractClientPlayer player,
                                                       @Local(argsOnly = true) InteractionHand hand) {
        return apprenticecodex$isMaintenancePose(player, hand, stack) ? UseAnim.NONE : original.call(stack);
    }

    @Unique
    private static boolean apprenticecodex$isMaintenancePose(AbstractClientPlayer player, InteractionHand hand,
                                                            ItemStack stack) {
        return apprenticecodex$isMainHandStaff(player, hand, stack) && player.getUsedItemHand() == hand
                && ChargedTwinBladeStaffRiptide.isMaintenanceInput(player);
    }

    @Unique
    private static boolean apprenticecodex$isMainHandStaff(AbstractClientPlayer player, InteractionHand hand,
                                                          ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND && stack.getItem() instanceof ChargedTwinBladeStaff
                && ItemStack.isSameItemSameComponents(stack, player.getMainHandItem());
    }
}
