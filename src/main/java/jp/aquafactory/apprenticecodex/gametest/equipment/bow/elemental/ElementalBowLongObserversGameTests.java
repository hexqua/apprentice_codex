package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.netty.channel.embedded.EmbeddedChannel;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport;
import jp.aquafactory.apprenticecodex.gametest.support.ForgePacketTestSupport;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowCastPacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongTargetGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongObserversGameTests {
    private ElementalBowLongObserversGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void newObserversReceiveOnlyCurrentBowCast(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var spell = SpellRegistry.LUNAR_AIM.get();
            var caster = player(h, InteractionHand.MAIN_HAND, spell);
            var observer = player(h, InteractionHand.MAIN_HAND, spell);
            var packets = new ArrayList<SyncElementalBowCastPacket>();
            var connection = new Connection(PacketFlow.SERVERBOUND) {
                @Override public void send(Packet<?> packet) {
                    var cast = ForgePacketTestSupport.decode(packet,
                            Networks.class, "CHANNEL",
                            new SyncElementalBowCastPacket(caster.getUUID(), spell.getSpellId(), true),
                            SyncElementalBowCastPacket::decode);
                    if (cast != null) packets.add(cast);
                }
            };
            var channel = new EmbeddedChannel(connection);
            var previous = observer.connection;
            new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, observer);
            try {
                begin(h, caster, InteractionHand.MAIN_HAND);
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.StartTracking(observer, caster));
                h.assertTrue(packets.size() == 1 && packets.get(0).active()
                                && packets.get(0).playerId().equals(caster.getUUID())
                                && packets.get(0).spellId().equals(spell.getSpellId()),
                        "A new observer must receive the caster's active bow spell");
                packets.clear();
                // 終了を観測できなかった後の再追跡で、過去の弓詠唱を再送しない。
                ElementalBowPendingCast.cancel(caster);
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.StartTracking(observer, caster));
                h.assertTrue(packets.isEmpty(), "Tracking after cancellation must not restore a finished bow cast");
                begin(h, caster, InteractionHand.MAIN_HAND);
                caster.stopUsingItem();
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.StartTracking(observer, caster));
                h.assertTrue(packets.isEmpty(), "Invalid pending state must not be sent before the next cleanup tick");
                ElementalBowPendingCast.cancel(caster);
                var magic = MagicData.getPlayerMagicData(caster);
                magic.initiateCast(spell, 1, 20, CastSource.SPELLBOOK, "mainhand");
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.StartTracking(observer, caster));
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
