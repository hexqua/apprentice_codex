package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerPlayerEvents.class, remap = false)
public abstract class HeartstopSpellSupporterMixin {
    // 元のダメージ基準・同期・イベント順序を保ち、その被弾の追加蓄積量だけを半減する。
    @WrapOperation(method = "onBeforeDamageTaken", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/capabilities/magic/SyncedSpellData;addHeartstopDamage(F)V"))
    private static void reduceAddedDamage(SyncedSpellData data, float addedDamage, Operation<Void> original,
                                         LivingDamageEvent.Pre event) {
        original.call(data, ProtectionSpellSupporter.applyHeartstopAccumulationDiscount(addedDamage, event.getEntity()));
    }
}
