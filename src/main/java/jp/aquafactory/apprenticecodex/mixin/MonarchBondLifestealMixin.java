package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.damage.DamageSources;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondHealing;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DamageSources.class, remap = false)
public abstract class MonarchBondLifestealMixin {
    // Forge版Iron'sの吸収回復だけを包み、先行ハンドラの減額・キャンセルをそのまま尊重する。
    // 注入先が変わった場合は吸収分配だけを無効化し、検証モードでも注入件数不足をエラーにしない。
    @Redirect(method = "postHitEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V", remap = true),
            require = 0, expect = 0)
    private static void distributeActualLifesteal(LivingEntity attacker, float amount) {
        MonarchBondHealing.healWithLifestealOverflow(attacker, amount);
    }
}
