package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import net.minecraft.server.level.ServerPlayer;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeCompat;

public record ClientManaStabilizerInputPacket(boolean aiming, boolean hook, long shot) implements CustomPacketPayload {
    public static final Type<ClientManaStabilizerInputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_mana_stabilizer_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientManaStabilizerInputPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeBoolean(packet.aiming()); buffer.writeBoolean(packet.hook()); buffer.writeVarLong(packet.shot()); },
            buffer -> new ClientManaStabilizerInputPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarLong()));
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ClientManaStabilizerInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && packet.shot() >= 0) {
                IronsArtificeCompat.manaStabilizerInput(player, packet.aiming(), packet.hook(), packet.shot());
            }
        });
    }
}

