package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientOpenSpellcasterAccessoryCasePacket(int sourceSlot) implements CustomPacketPayload {
    public static final Type<ClientOpenSpellcasterAccessoryCasePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_open_spellcaster_accessory_case")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientOpenSpellcasterAccessoryCasePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> buffer.writeVarInt(packet.sourceSlot()),
                    ClientOpenSpellcasterAccessoryCasePacket::decode
            );

    private static ClientOpenSpellcasterAccessoryCasePacket decode(FriendlyByteBuf buffer) {
        return new ClientOpenSpellcasterAccessoryCasePacket(buffer.readVarInt());
    }

    public static void handle(ClientOpenSpellcasterAccessoryCasePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender && sender.isCreative()) {
                SpellcasterAccessoryCase.openFromInventorySlot(sender, packet.sourceSlot());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
