package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedIceTomb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerIceTombDismountMixin {
    @WrapOperation(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;stopRiding()V"))
    private void allowManualRelease(Player player, Operation<Void> original) {
        // 通常の降車入力だけを許可し、ログアウト等のstopRidingは対象にしない。
        if (player.getVehicle() instanceof SupportedIceTomb tomb) {
            tomb.apprenticecodex$withShatterRelease(() -> original.call(player));
        } else {
            original.call(player);
        }
    }
}
