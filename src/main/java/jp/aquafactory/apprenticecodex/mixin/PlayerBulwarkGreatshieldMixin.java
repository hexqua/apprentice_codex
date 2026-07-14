package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerBulwarkGreatshieldMixin {
    @Redirect(
            method = "blockUsingShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;canDisableShield()Z")
    )
    private boolean apprenticecodex$preventBulwarkDisable(LivingEntity attacker) {
        var player = (Player) (Object) this;
        // 攻撃者への通常の盾反動は維持し、斧の使用不能判定だけ Bulwark では通さない。
        return !(player.getUseItem().getItem() instanceof BulwarkGreatshield)
                && attacker.canDisableShield();
    }
}
