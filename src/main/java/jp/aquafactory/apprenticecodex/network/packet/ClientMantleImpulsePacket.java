package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientMantleImpulsePacket(long sequence, float forward, float strafe) implements CustomPacketPayload {
    public static final Type<ClientMantleImpulsePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mantle_impulse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMantleImpulsePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeLong(packet.sequence); buffer.writeFloat(packet.forward); buffer.writeFloat(packet.strafe); },
            buffer -> new ClientMantleImpulsePacket(buffer.readLong(), buffer.readFloat(), buffer.readFloat()));

    @Override
    public @NotNull Type<ClientMantleImpulsePacket> type() { return TYPE; }

    public static void handle(ClientMantleImpulsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ShootingStarMantleRuntime.impulse(player, packet.sequence, packet.forward, packet.strafe);
            }
        });
    }
}
