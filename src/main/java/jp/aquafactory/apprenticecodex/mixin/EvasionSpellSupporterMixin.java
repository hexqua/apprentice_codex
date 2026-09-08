package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.ender.EvasionSpell;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = EvasionSpell.class, remap = false)
public abstract class EvasionSpellSupporterMixin {
    // 効果全体への介入ではポーションにも波及するため、魔法による付与だけを変更する。
    @WrapOperation(method = "onCast", at = @At(value = "NEW", target = "(Lnet/minecraft/core/Holder;IIZZZ)Lnet/minecraft/world/effect/MobEffectInstance;"))
    private MobEffectInstance applyAmplifierBonus(Holder<MobEffect> effect, int duration, int amplifier,
                                                 boolean ambient, boolean visible, boolean showIcon,
                                                 Operation<MobEffectInstance> original,
                                                 Level level, int spellLevel, LivingEntity caster,
                                                 CastSource source, MagicData data) {
        return original.call(effect, duration, ProtectionSpellSupporter.applyEvasionAmplifierBonus(amplifier, caster),
                ambient, visible, showIcon);
    }

    @Inject(method = "getUniqueInfo", at = @At("RETURN"), cancellable = true)
    private void showSupportedHits(int spellLevel, LivingEntity caster, CallbackInfoReturnable<List<MutableComponent>> cir) {
        if (ProtectionSpellSupporter.isEquippedBy(caster)) {
            var spell = (AbstractSpell) (Object) this;
            int amplifier = ProtectionSpellSupporter.applyEvasionAmplifierBonus((int) spell.getSpellPower(spellLevel, caster), caster);
            cir.setReturnValue(List.of(Component.translatable("ui.irons_spellbooks.hits_dodged", amplifier + 1)));
        }
    }
}
