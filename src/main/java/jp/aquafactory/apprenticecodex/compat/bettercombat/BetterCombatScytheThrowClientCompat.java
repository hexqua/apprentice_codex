package jp.aquafactory.apprenticecodex.compat.bettercombat;

import net.bettercombat.Platform;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.network.Packets;
import net.minecraft.client.Minecraft;

public final class BetterCombatScytheThrowClientCompat {
    private BetterCombatScytheThrowClientCompat() {}

    public static void stopSwing() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // 属性解除だけでは既に再生中の振り終わりモーションが残るため、投擲開始時に終了を通知する。
        ((MinecraftClient_BetterCombat) mc).cancelUpswing();
        ((PlayerAttackAnimatable) mc.player).stopAttackAnimation(0);
        Platform.networkC2S_Send(Packets.AttackAnimation.stop(mc.player.getId(), 0));
    }
}
