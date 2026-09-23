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

public record ClientMantleDashInputPacket(long sequence, boolean jump, float forward, float strafe) implements CustomPacketPayload {
    public static final Type<ClientMantleDashInputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mantle_dash_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMantleDashInputPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeLong(packet.sequence); buffer.writeBoolean(packet.jump); buffer.writeFloat(packet.forward); buffer.writeFloat(packet.strafe); },
            buffer -> new ClientMantleDashInputPacket(buffer.readLong(), buffer.readBoolean(), buffer.readFloat(), buffer.readFloat()));

    @Override
    public @NotNull Type<ClientMantleDashInputPacket> type() { return TYPE; }

    public static void handle(ClientMantleDashInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) ShootingStarMantleRuntime.state(player).elemental
                    .input(player, packet.sequence, packet.jump, packet.forward, packet.strafe);
        });
    }
}
