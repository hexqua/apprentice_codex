package jp.aquafactory.apprenticecodex.gametest.equipment.broom.support;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.ArrayList;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.FloatmountBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestCleanup.onTestEnd;

public final class BroomTestSupport {
    public static final String TEMPLATE = "gametest/basic_floor";
    public static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    private BroomTestSupport() {}

    public static FloatmountBroomEntity spawnBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        trackEntity(helper, broom);
        helper.assertTrue(helper.getLevel().addFreshEntity(broom), "Broom fixture entity must be registered");
        return broom;
    }

    public static HoverrideBroomEntity spawnHoverrideBroom(GameTestHelper helper, double relativeY) {
        var pos = helper.absolutePos(TEST_POS);
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(pos.getX() + 0.5D, pos.getY() + relativeY, pos.getZ() + 0.5D);
        trackEntity(helper, broom);
        helper.assertTrue(helper.getLevel().addFreshEntity(broom), "Broom fixture entity must be registered");
        return broom;
    }

    public static Player player(GameTestHelper helper, String name) {
        return trackEntity(helper, helper.makeMockPlayer(GameType.SURVIVAL));
    }

    public static ServerPlayer serverRider(GameTestHelper helper) {
        return serverRider(helper, "broom_test_rider");
    }

    public static ServerPlayer serverRider(GameTestHelper helper, String profileName) {
        // Epic FightはEntityType.PLAYERをServerPlayerとしてpatchするため、world tickされる騎乗者には
        // local player扱いの軽量mockではなく、接続を備えたserver側GameTest playerを使う。
        var level = helper.getLevel();
        var server = level.getServer();
        var profile = new GameProfile(UUID.randomUUID(), profileName);
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        onTestEnd(helper, () -> {
            try {
                // Call Broomが生成した実体もownerで絞り、通常の箒や隣のテストには触れない。
                var owned = new ArrayList<AbstractBroomEntity>();
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof AbstractBroomEntity broom && broom.isOwnedBy(player)) owned.add(broom);
                }
                owned.forEach(Entity::discard);
                player.stopRiding();
                player.discard();
            } finally {
                channel.finishAndReleaseAll();
            }
        });
        NetworkRegistry.configureMockConnection(connection);
        new ServerGamePacketListenerImpl(server, connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }

    public static <T extends Entity> T trackEntity(GameTestHelper helper, T entity) {
        onTestEnd(helper, () -> {
            entity.stopRiding();
            entity.ejectPassengers();
            entity.discard();
        });
        return entity;
    }

    public static FakePlayer equipmentPlayer(GameTestHelper helper, BlockPos pos, String profileName) {
        return trackEntity(helper, PlayerTestSupport.createEquipmentTestPlayer(helper, pos, profileName));
    }

    public static MagicData magicData(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Floatmount Broom test could not resolve player mana data");
        return magicData;
    }
}
