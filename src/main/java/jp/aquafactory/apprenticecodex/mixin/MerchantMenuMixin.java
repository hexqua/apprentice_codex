package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
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
    private void apprenticecodex$allowErrandMageNbtAgnosticAutofill(int paymentSlotIndex, ItemStack paymentStack, CallbackInfo ci) {
        if (paymentStack.isEmpty() || !ErrandMageTradeHelper.shouldIgnorePaymentTags(paymentStack)) {
            return;
        }

        for (int i = 3; i < 39; ++i) {
            var slot = ((MerchantMenu) (Object) this).slots.get(i);
            var inventoryStack = slot.getItem();
            if (!ErrandMageTradeHelper.matchesPaymentItem(inventoryStack, paymentStack)) {
                continue;
            }

            var paymentSlotStack = tradeContainer.getItem(paymentSlotIndex);
            var currentCount = paymentSlotStack.isEmpty() ? 0 : paymentSlotStack.getCount();
            var moveCount = Math.min(paymentStack.getMaxStackSize() - currentCount, inventoryStack.getCount());
            var movedStack = inventoryStack.copy();
            var newCount = currentCount + moveCount;
            inventoryStack.shrink(moveCount);
            movedStack.setCount(newCount);
            tradeContainer.setItem(paymentSlotIndex, movedStack);
            if (newCount >= paymentStack.getMaxStackSize()) {
                break;
            }
        }

        ci.cancel();
    }
}
