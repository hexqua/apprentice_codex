package jp.aquafactory.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.sammy.malum.core.handlers.SoulDataHandler;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.sammy.malum.core.handlers.SoulDataHandler", remap = false)
public abstract class MalumSoulDataHandlerMixin {
    @Inject(method = "getScytheWeapon", at = @At("RETURN"), cancellable = true)
    private static void apprenticecodex$recognizeSpellReaperScythe(
            DamageSource source,
            LivingEntity attacker, CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!cir.getReturnValue().isEmpty()) {
            return;
        }

        // Malumの投擲大鎌を含む攻撃元解決は維持し、Spell Reaper Scytheだけ判定結果を補完する。
        var candidate = SoulDataHandler.getSoulHunterWeapon(source, attacker);
        if (candidate.is(ItemRegistry.SPELL_REAPER_SCYTHE.get())) cir.setReturnValue(candidate);
    }
}
