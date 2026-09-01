package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientOpenStorageStabilizerEnderChestPacket(int sourceSlot) implements CustomPacketPayload {
    public static final Type<ClientOpenStorageStabilizerEnderChestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_open_storage_stabilizer_ender_chest")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientOpenStorageStabilizerEnderChestPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeVarInt(packet.sourceSlot()),
                    ClientOpenStorageStabilizerEnderChestPacket::decode
            );

    private static ClientOpenStorageStabilizerEnderChestPacket decode(FriendlyByteBuf buffer) {
        return new ClientOpenStorageStabilizerEnderChestPacket(buffer.readVarInt());
    }

    public static void handle(ClientOpenStorageStabilizerEnderChestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender && sender.isCreative()) {
                StorageStabilizer.openEnderChestFromInventorySlot(sender, packet.sourceSlot());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
