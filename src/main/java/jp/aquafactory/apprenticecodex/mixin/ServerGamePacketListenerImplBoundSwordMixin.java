package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBowManager;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordManager;
import jp.aquafactory.apprenticecodex.spell.edgedancer.EdgeDancerManager;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplBoundSwordMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$finishBoundSwordOnForbiddenInventoryClick(
            ServerboundContainerClickPacket packet,
            CallbackInfo ci
    ) {
        if (BoundBowManager.handleContainerClick(player, packet)
                || BoundSwordManager.handleContainerClick(player, packet)
                || EdgeDancerManager.handleContainerClick(player, packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$finishBoundSwordOnDropAction(
            ServerboundPlayerActionPacket packet,
            CallbackInfo ci
    ) {
        if (BoundBowManager.handlePlayerAction(player, packet)
                || BoundSwordManager.handlePlayerAction(player, packet)
                || EdgeDancerManager.handlePlayerAction(player, packet)) {
            ci.cancel();
        }
    }
}
