package jp.aquafactory.apprenticecodex.gametest.support;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class ConnectedPlayerTestSupport {
    private ConnectedPlayerTestSupport() {}

    public static PlayerFixture createPlayer(GameTestHelper helper) {
        // Epic Fightはserver側のPLAYERをServerPlayerとして扱うため、軽量mockを使わない。
        // doTickはPlayer/LivingEntityの装備更新を通り、FakePlayerの空tickを避けられる。
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "rifle_attributes");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 2, 1))));
        player.setNoGravity(true);
        return new PlayerFixture(player, channel);
    }

    public record PlayerFixture(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        @Override
        public void close() {
            channel.finishAndReleaseAll();
        }
    }

}
