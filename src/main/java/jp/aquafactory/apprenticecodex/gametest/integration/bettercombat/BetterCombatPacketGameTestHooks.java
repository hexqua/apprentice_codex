package jp.aquafactory.apprenticecodex.gametest.integration.bettercombat;

import net.bettercombat.forge.network.NetworkHandler;
import net.bettercombat.forge.network.PacketWrapper;
import net.bettercombat.network.Packets;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;

final class BetterCombatPacketGameTestHooks {
    private BetterCombatPacketGameTestHooks() {}

    static void assertStopWrapperUsesRegisteredChannel(GameTestHelper helper) {
        var expected = Packets.AttackAnimation.stop(37, 0);
        var source = expected.write();
        FriendlyByteBuf wire = null;
        PacketWrapper decoded = null;
        try {
            var wrapper = new PacketWrapper(false, Packets.AttackAnimation.ID, source);
            var packet = NetworkHandler.INSTANCE.toVanillaPacket(wrapper, NetworkDirection.PLAY_TO_SERVER);
            helper.assertTrue(packet instanceof ServerboundCustomPayloadPacket,
                    "Registered stop wrapper must produce a serverbound custom payload");
            var payload = (ServerboundCustomPayloadPacket) packet;
            wire = payload.getData();
            helper.assertTrue(payload.getIdentifier().equals(ResourceLocation.fromNamespaceAndPath("bettercombat", "network")),
                    "Stop wrapper must use Better Combat's registered channel");
            // discriminatorは登録順で変わる。登録された経路で変換できたことと、その後のwire内容を確認する。
            wire.readVarInt();
            wire.markReaderIndex();
            helper.assertFalse(wire.readBoolean(), "Stop wrapper must target the server handler");
            wire.resetReaderIndex();
            decoded = PacketWrapper.decode(wire);
            helper.assertTrue(decoded.packetType.equals(Packets.AttackAnimation.ID),
                    "Stop wrapper must preserve the attack-animation packet id");
            var body = new FriendlyByteBuf(decoded.data);
            helper.assertTrue(Packets.AttackAnimation.read(body).equals(expected) && !body.isReadable(),
                    "Registered stop wrapper must preserve the complete stop message");
        } finally {
            if (decoded != null) decoded.data.release();
            if (wire != null) wire.release();
            source.release();
        }
    }
}
