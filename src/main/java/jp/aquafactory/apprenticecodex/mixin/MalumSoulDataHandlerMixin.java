package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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
    @ModifyReturnValue(method = "getScytheWeapon", at = @At("RETURN"), require = 0)
    private static ItemStack apprenticecodex$recognizeSpellReaperScythe(
            ItemStack original,
            DamageSource source,
            LivingEntity attacker
    ) {
        if (!original.isEmpty()) {
            return original;
        }

        // Malumの投擲大鎌を含む攻撃元解決は維持し、Spell Reaper Scytheだけ判定結果を補完する。
        var candidate = SoulDataHandler.getSoulHunterWeapon(source, attacker);
        return candidate.is(ItemRegistry.SPELL_REAPER_SCYTHE.get()) ? candidate : original;
    }
}
