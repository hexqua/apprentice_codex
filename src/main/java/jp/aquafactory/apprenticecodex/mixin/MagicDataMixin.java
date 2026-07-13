package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.item.shield.ShieldCastUseContext;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MagicData.class, remap = false)
public abstract class MagicDataMixin {
    @Redirect(
            method = "resetCastingState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;stopUsingItem()V",
                    remap = true
            )
    )
    private void apprenticecodex$preserveShieldUseDuringCastCleanup(ServerPlayer player) {
        // 盾独自の CONTINUOUS 終了時は魔法状態だけを片付け、構えの解除は盾の releaseUsing に任せる.
        if (!ShieldCastUseContext.shouldPreserveShieldUse((MagicData) (Object) this)) {
            player.stopUsingItem();
        }
    }
}
