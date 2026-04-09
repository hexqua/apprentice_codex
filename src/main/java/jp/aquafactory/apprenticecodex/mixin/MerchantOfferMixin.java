package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.utility.ErrandMageTradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {
    @Inject(method = "isRequiredItem", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$ignoreTagsForErrandMagePayments(ItemStack offer, ItemStack cost, CallbackInfoReturnable<Boolean> cir) {
        if (!ErrandMageTradeHelper.shouldIgnorePaymentTags(cost)) {
            return;
        }

        // 既存ワールドに保存済みの offer は cost 側へ Iron's の既定 NBT が残り得るため、
        // 取引照合側でも対象アイテムだけは item identity ベースに寄せて救済する。
        cir.setReturnValue(ErrandMageTradeHelper.matchesPaymentItem(offer, cost));
    }
}
