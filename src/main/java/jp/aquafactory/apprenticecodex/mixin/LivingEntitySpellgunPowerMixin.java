package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunCastContext;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpellgunPowerMixin {
    @Inject(method = "getAttributeValue(Lnet/minecraft/core/Holder;)D", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$useSpellgunForcedSummonDamage(
            Holder<Attribute> attribute,
            CallbackInfoReturnable<Double> cir
    ) {
        if (!attribute.equals(AttributeRegistry.SUMMON_DAMAGE)) {
            return;
        }

        var forcedValue = SpellgunCastContext.resolveSummonDamage((LivingEntity) (Object) this);
        if (forcedValue != null) {
            // 召喚後のダメージ計算まで状態を保持せず、魔法の同期的な発動処理だけを上書きする。
            cir.setReturnValue(forcedValue);
        }
    }
}
