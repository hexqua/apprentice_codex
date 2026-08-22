package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientConfigureSpellcasterAccessoryCaseMenuPacket(
        int containerId,
        int maxVisibleCuriosColumns
) implements CustomPacketPayload {
    public static final Type<ClientConfigureSpellcasterAccessoryCaseMenuPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    "client_configure_spellcaster_accessory_case_menu"
            )
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfigureSpellcasterAccessoryCaseMenuPacket>
            STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.containerId());
                buffer.writeVarInt(packet.maxVisibleCuriosColumns());
            },
            ClientConfigureSpellcasterAccessoryCaseMenuPacket::decode
    );

    private static ClientConfigureSpellcasterAccessoryCaseMenuPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfigureSpellcasterAccessoryCaseMenuPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ClientConfigureSpellcasterAccessoryCaseMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)
                    || packet.maxVisibleCuriosColumns() < 0
                    || !(sender.containerMenu instanceof SpellcasterAccessoryCaseMenu menu)
                    || menu.containerId != packet.containerId()) {
                return;
            }

            // clientは表示許容量だけを選び、実際のslot数と全inventory移動はserver側menuが決定する。
            menu.configureMaxVisibleCuriosColumns(packet.maxVisibleCuriosColumns());
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
