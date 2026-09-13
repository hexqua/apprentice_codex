package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerPlayerEvents.class, remap = false)
public abstract class ElementalBowDamageInterruptionMixin {
    // 魔法側の override に依存せず、被ダメージによる中断だけを弓の引き絞りから除外する。
    @WrapOperation(method = "onLivingIncomingDamage", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;canBeInterrupted(Lnet/minecraft/world/entity/player/Player;)Z"))
    private static boolean preserveBowDraw(AbstractSpell spell, Player player, Operation<Boolean> original) {
        if (player instanceof ServerPlayer serverPlayer && ElementalBowPendingCast.isManagedCast(serverPlayer)) {
            return false;
        }
        return original.call(spell, player);
    }
}
