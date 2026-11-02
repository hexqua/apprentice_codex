package jp.aquafactory.apprenticecodex.compat.bettercombat;

import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
        // 1.20.1 ForgeではPacketWrapperだけが登録されているため、Better Combatの送信APIで包む。
        ClientPlayNetworking.send(Packets.AttackAnimation.ID,
                Packets.AttackAnimation.stop(mc.player.getId(), 0).write());
    }
}
