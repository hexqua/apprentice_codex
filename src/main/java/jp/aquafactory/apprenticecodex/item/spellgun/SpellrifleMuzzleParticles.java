package jp.aquafactory.apprenticecodex.item.spellgun;

import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SpellrifleMuzzleParticlePacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;

public final class SpellrifleMuzzleParticles {
    private SpellrifleMuzzleParticles() {
    }

    public static void send(ServerPlayer shooter, ParticleOptions options, double x, double y, double z,
                            int count, double xDist, double yDist, double zDist, double speed) {
        // 視点とitem modelはサーバーにないため、射撃者だけclientで一人称の表示位置を補正する。
        // 他の観測者には従来のワールド座標を送り、射撃者へ二重に配信しない。
        var level = shooter.serverLevel();
        for (var observer : level.players()) {
            if (observer != shooter) {
                level.sendParticles(observer, options, false, x, y, z, count, xDist, yDist, zDist, speed);
            }
        }
        Networks.sendToPlayer(shooter, new SpellrifleMuzzleParticlePacket(new ClientboundLevelParticlesPacket(
                options, false, x, y, z, (float) xDist, (float) yDist, (float) zDist, (float) speed, count)));
    }
}
