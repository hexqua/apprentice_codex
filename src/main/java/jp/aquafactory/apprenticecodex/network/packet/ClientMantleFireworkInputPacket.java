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

public record ClientMantleFireworkInputPacket(long sequence, boolean jump) implements CustomPacketPayload {
    public static final Type<ClientMantleFireworkInputPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mantle_firework_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMantleFireworkInputPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeLong(packet.sequence); buffer.writeBoolean(packet.jump); },
            buffer -> new ClientMantleFireworkInputPacket(buffer.readLong(), buffer.readBoolean()));

    @Override
    public @NotNull Type<ClientMantleFireworkInputPacket> type() { return TYPE; }

    public static void handle(ClientMantleFireworkInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) ShootingStarMantleRuntime.state(player).firework
                    .input(player, packet.sequence, packet.jump);
        });
    }
}
