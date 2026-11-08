package jp.aquafactory.apprenticecodex.gametest.equipment.broom.support;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;

import java.util.UUID;

public final class BroomTestSupport {
    private BroomTestSupport() {}

    public static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    public static FloatmountBroomEntity spawnBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    public static HoverrideBroomEntity spawnHoverrideBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    public static ServerPlayer serverRider(GameTestHelper helper) {
        return serverRider(helper, "broom_rider_" + UUID.randomUUID().toString().substring(0, 4));
    }

    public static ServerPlayer serverRider(GameTestHelper helper, String profileName) {
        // Epic Fightはplayer tickでclient型を参照するため、world tickされる騎乗者には
        // 軽量mockではなく接続を備えたserver playerを使う。
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), profileName);
        var player = new ServerPlayer(level.getServer(), level, profile);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }
}
