package jp.aquafactory.apprenticecodex.gametest;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

final class ForgePacketTestSupport {
    private ForgePacketTestSupport() {}

    // Forge の wire discriminator は登録順に依存するため、固定値をテストへ埋め込まない。
    static <T> T decode(Packet<?> packet, Class<?> owner, String fieldName, T example,
                        Function<FriendlyByteBuf, T> decoder) {
        if (!(packet instanceof ClientboundCustomPayloadPacket actual)) return null;
        try {
            var field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            var channel = (SimpleChannel) field.get(null);
            var expected = (ClientboundCustomPayloadPacket) channel.toVanillaPacket(example, NetworkDirection.PLAY_TO_CLIENT);
            var expectedData = expected.getData();
            var actualData = actual.getData();
            try {
                if (!actual.getIdentifier().equals(expected.getIdentifier())
                        || actualData.readVarInt() != expectedData.readVarInt()) return null;
                return decoder.apply(actualData);
            } finally {
                actualData.release();
                expectedData.release();
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to inspect the registered test packet channel", exception);
        }
    }
}
