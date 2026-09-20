package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayerEvents.class, remap = false)
public abstract class HeartstopSpellSupporterMixin {
    // 元のダメージ基準・同期・イベント順序を保ち、その被弾の追加蓄積量だけを半減する。
    @Redirect(method = "onBeforeDamageTaken", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/capabilities/magic/SyncedSpellData;addHeartstopDamage(F)V"))
    private static void reduceAddedDamage(SyncedSpellData data, float addedDamage,
                                         LivingDamageEvent event) {
        data.addHeartstopDamage(ProtectionSpellSupporter.applyHeartstopAccumulationDiscount(addedDamage, event.getEntity()));
    }
}
