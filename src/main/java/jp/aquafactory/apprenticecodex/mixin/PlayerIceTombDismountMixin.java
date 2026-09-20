package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedIceTomb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerIceTombDismountMixin {
    @Redirect(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;stopRiding()V"))
    private void allowManualRelease(Player player) {
        // 通常の降車入力だけを許可し、ログアウト等のstopRidingは対象にしない。
        if (player.getVehicle() instanceof SupportedIceTomb tomb) {
            tomb.apprenticecodex$withShatterRelease(player::stopRiding);
        } else {
            player.stopRiding();
        }
    }
}
