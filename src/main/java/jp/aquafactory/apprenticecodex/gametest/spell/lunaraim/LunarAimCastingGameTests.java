package jp.aquafactory.apprenticecodex.gametest.spell.lunaraim;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimArrowEntity;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimCastData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.spell.lunaraim.LunarAimFixture.BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.lunaraim.LunarAimFixture.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LunarAimCastingGameTests {
    private LunarAimCastingGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castFreezesSelectionAndFiresFourEvenDirections(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var target = s.zombie(8, 0);
            var data = s.preCast();
            h.assertTrue(data.getAdditionalCastData() instanceof LunarAimCastData captured && target.getUUID().equals(captured.targetId()),
                    "Precast must snapshot the target: expected=" + target.getUUID() + ", captured=" + data.getAdditionalCastData());
            s.owner.setYRot(35);
            s.owner.setXRot(-25);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            var arrows = s.castArrows();
            h.assertTrue(arrows.size() == 4, "Exactly four arrows must spawn");
            for (int i = 0; i < 4; i++) {
                var direction = Vec3.directionFromRotation(-25, 35 - 60 + 40 * i).scale(2);
                h.assertTrue(arrows.stream().anyMatch(a -> a.getDeltaMovement().distanceTo(direction) < 1.0e-6), "Launch yaw spacing must be 40 degrees and preserve pitch");
            }
            h.assertTrue(arrows.stream().allMatch(LunarAimArrowEntity::hasTarget), "Turning during casting must preserve the selected target");
            h.assertTrue(!(data.getAdditionalCastData() instanceof LunarAimCastData), "Casting must clear the snapshot");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void emptyCancelledAndDirectCasts(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var data = s.preCast();
            var target = s.zombie(8, 0);
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(s.castArrows().size() == 4 && s.castArrows().stream().noneMatch(LunarAimArrowEntity::hasTarget), "An empty snapshot must not reselect");
            s.castArrows().forEach(Entity::discard);
            data = s.preCast();
            s.spell.onServerCastComplete(h.getLevel(), 1, s.owner, data, true);
            h.assertTrue(data.getAdditionalCastData() == null, "Cancellation must clear target state");
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SWORD, data);
            h.assertTrue(s.castArrows().size() == 4 && s.castArrows().stream().allMatch(LunarAimArrowEntity::hasTarget), "Direct casts must perform the missing selection");
            s.castArrows().forEach(Entity::discard);
            data = s.preCast();
            target.discard();
            s.spell.onCast(h.getLevel(), 1, s.owner, CastSource.SPELLBOOK, data);
            h.assertTrue(s.castArrows().stream().noneMatch(LunarAimArrowEntity::hasTarget), "A target removed during casting must be lost");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void precastSyncsTargetMarkerForLivingTargetsAndCrystals(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var packets = new ArrayList<Packet<?>>();
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            var previous = s.owner.connection;
            var closed = new boolean[1];
            Runnable closeConnection = () -> {
                if (!closed[0]) {
                    closed[0] = true;
                    s.owner.connection = previous;
                    channel.finishAndReleaseAll();
                }
            };
            // 接続の構築途中・assertion失敗・timeoutでも同じ一回だけの解放を通す。
            s.scope.onClose(closeConnection);
            NetworkRegistry.configureMockConnection(connection);
            var cookie = CommonListenerCookie.createInitial(s.owner.getGameProfile(), false);
            new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, s.owner, cookie) {
                @Override
                public void send(Packet<?> packet) {
                    packets.add(packet);
                }
            };
            try {
                var living = s.zombie(8, 0);
                var crystal = new EndCrystal(EntityType.END_CRYSTAL, h.getLevel());
                crystal.setPos(s.origin.add(12, 0, 0));
                s.add(crystal);
                for (var target : List.of(living, crystal)) {
                    packets.clear();
                    s.preCast();
                    var markers = packets.stream()
                            .filter(p -> p instanceof ClientboundCustomPayloadPacket payload
                                    && payload.payload() instanceof SyncTargetingDataPacket)
                            .map(p -> (SyncTargetingDataPacket)
                                    ((ClientboundCustomPayloadPacket) p).payload()).toList();
                    h.assertTrue(markers.size() == 1, "Successful precast must send one native targeting marker");
                    var buffer = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        markers.getFirst().write(buffer);
                        h.assertTrue(buffer.readUtf().equals(s.spell.getSpellId()) && buffer.readInt() == 1
                                        && buffer.readUUID().equals(target.getUUID()),
                                "The marker must carry LunarAim and the selected UUID, including non-living targets");
                    } finally {
                        buffer.release();
                    }
                    target.discard();
                }
                packets.clear();
                s.preCast();
                h.assertTrue(packets.isEmpty(), "No target must produce neither marker nor success notification");
            } finally {
                closeConnection.run();
            }
        }
        h.succeed();
    }
}
