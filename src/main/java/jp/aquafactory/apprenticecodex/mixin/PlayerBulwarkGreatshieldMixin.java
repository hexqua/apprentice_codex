package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerBulwarkGreatshieldMixin {
    @Redirect(
            method = "blockUsingShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canDisableShield(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false)
    )
    private boolean apprenticecodex$preventBulwarkDisable(
            ItemStack attackingStack,
            ItemStack shieldStack,
            LivingEntity defender,
            LivingEntity attacker
    ) {
        // 攻撃者への通常の盾反動は維持し、斧の使用不能判定だけ Bulwark では通さない。
        return !(shieldStack.getItem() instanceof BulwarkGreatshield)
                && attackingStack.canDisableShield(shieldStack, defender, attacker);
    }
}
