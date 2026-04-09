package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow
    @Final
    private MerchantContainer tradeContainer;

    @Inject(method = "moveFromInventoryToPaymentSlot", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$allowErrandMageNbtAgnosticAutofill(int paymentSlotIndex, ItemCost paymentCost, CallbackInfo ci) {
        if (!ErrandMageTradeHelper.shouldIgnorePaymentTags(paymentCost.item().value())) {
            return;
        }

        for (int i = 3; i < 39; ++i) {
            var slot = ((MerchantMenu) (Object) this).slots.get(i);
            var inventoryStack = slot.getItem();
            if (!ErrandMageTradeHelper.matchesPaymentItem(inventoryStack, paymentCost)) {
                continue;
            }

            var paymentSlotStack = tradeContainer.getItem(paymentSlotIndex);
            if (!paymentSlotStack.isEmpty() && !ErrandMageTradeHelper.matchesPaymentItem(paymentSlotStack, paymentCost)) {
                continue;
            }

            var maxStackSize = inventoryStack.getMaxStackSize();
            var currentCount = paymentSlotStack.getCount();
            var moveCount = Math.min(maxStackSize - currentCount, inventoryStack.getCount());
            var movedStack = inventoryStack.copyWithCount(currentCount + moveCount);
            inventoryStack.shrink(moveCount);
            tradeContainer.setItem(paymentSlotIndex, movedStack);
            if (movedStack.getCount() >= maxStackSize) {
                break;
            }
        }

        ci.cancel();
    }
}
