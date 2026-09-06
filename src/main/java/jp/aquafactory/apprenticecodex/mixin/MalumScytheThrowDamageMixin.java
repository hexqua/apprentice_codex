package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem", remap = false)
public abstract class MalumScytheThrowDamageMixin {
    @ModifyReturnValue(method = "getScytheDamage", at = @At("RETURN"))
    private static MalumScytheItem.ScytheDamage apprenticecodex$throwDamage(
            MalumScytheItem.ScytheDamage original, DamageSource source, LivingEntity attacker) {
        // Geasの遅延発動時にも、持ち替え後の攻撃力ではなく投擲の保持値を参照する。
        if (source.getDirectEntity() instanceof ScytheThrowEntity scythe && scythe.isRebound()) {
            return new MalumScytheItem.ScytheDamage(scythe.getPhysicalDamage(), scythe.getMagicDamage(), true);
        }
        return original;
    }
}
