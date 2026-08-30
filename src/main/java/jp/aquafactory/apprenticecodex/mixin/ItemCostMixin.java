package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCost.class)
public abstract class ItemCostMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$ignoreComponentsForErrandMagePayments(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var itemCost = (ItemCost) (Object) this;
        if (!ErrandMageTradeHelper.shouldIgnorePaymentTags(itemCost.item().value())) {
            // 1.21.1で保存・同期された旧安楽の果実取引は、FOOD 内の effect Supplier が
            // 復号時に別インスタンスとなるため、意味が同じでもvanillaの等価判定だけ失敗する。
            if (ErrandMageTradeHelper.matchesLegacyComfortBerriesPayment(stack, itemCost)) {
                cir.setReturnValue(true);
            }
            return;
        }

        // 既存ワールドに保存済みの取引は cost 側へ Iron's の既定 component が残り得るため、
        // 対象アイテムだけは component 判定を外して item identity ベースで受け付ける。
        cir.setReturnValue(ErrandMageTradeHelper.matchesPaymentItem(stack, itemCost));
    }
}
