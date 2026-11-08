package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowCastPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.begin;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.player;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongObserversGameTests {
    private ElementalBowLongObserversGameTests() {}
    private static final String TEMPLATE = "gametest/bow_casting";

    @GameTest(template = TEMPLATE)
    public static void newObserversReceiveOnlyCurrentBowCast(GameTestHelper h) {
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
            var spell = SpellRegistry.LUNAR_AIM.get();
            var caster = player(h, InteractionHand.MAIN_HAND, spell);
            var observer = player(h, InteractionHand.MAIN_HAND, spell);
            var packets = new ArrayList<SyncElementalBowCastPacket>();
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            BowTestScope.of(h).onClose(channel::finishAndReleaseAll);
            NetworkRegistry.configureMockConnection(connection);
            var previous = observer.connection;
            new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, observer,
                    CommonListenerCookie.createInitial(observer.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket payload
                            && payload.payload() instanceof SyncElementalBowCastPacket cast) {
                        packets.add(cast);
                    }
                }
            };
            try {
                begin(h, caster, InteractionHand.MAIN_HAND);
                var event = new PlayerEvent.StartTracking(observer, caster);
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.size() == 1 && packets.getFirst().active()
                                && packets.getFirst().playerId().equals(caster.getUUID())
                                && packets.getFirst().spellId().equals(spell.getSpellId()),
                        "A new observer must receive the caster's active bow spell");
                packets.clear();
                // 終了を観測できなかった後の再追跡で、過去の弓詠唱を再送しない。
                ElementalBowPendingCast.cancel(caster);
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.isEmpty(), "Tracking after cancellation must not restore a finished bow cast");
                begin(h, caster, InteractionHand.MAIN_HAND);
                caster.stopUsingItem();
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.isEmpty(), "Invalid pending state must not be sent before the next cleanup tick");
                ElementalBowPendingCast.cancel(caster);
                var magic = MagicData.getPlayerMagicData(caster);
                magic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                NeoForge.EVENT_BUS.post(event);
                h.assertTrue(packets.isEmpty(), "Ordinary casting must not be synchronized as a bow cast");
                magic.resetCastingState();
            } finally {
                ElementalBowPendingCast.cancel(caster);
                observer.connection = previous;
                channel.finishAndReleaseAll();
            }
        }
        h.succeed();
    }
}
