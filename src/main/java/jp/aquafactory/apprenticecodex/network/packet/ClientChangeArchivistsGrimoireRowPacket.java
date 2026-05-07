package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientChangeArchivistsGrimoireRowPacket(int delta) implements CustomPacketPayload {
    public static final Type<ClientChangeArchivistsGrimoireRowPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_change_archivists_grimoire_row"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientChangeArchivistsGrimoireRowPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientChangeArchivistsGrimoireRowPacket::decode);

    public ClientChangeArchivistsGrimoireRowPacket {
        delta = Integer.compare(delta, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientChangeArchivistsGrimoireRowPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.delta());
    }

    public static ClientChangeArchivistsGrimoireRowPacket decode(FriendlyByteBuf buffer) {
        return new ClientChangeArchivistsGrimoireRowPacket(buffer.readByte());
    }

    public static void handle(ClientChangeArchivistsGrimoireRowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || packet.delta() == 0) {
                return;
            }

            var spellbookStack = Utils.getPlayerSpellbookStack(sender);
            if (spellbookStack != null && spellbookStack.getItem() instanceof ArchivistsGrimoire) {
                ArchivistsGrimoire.changeSelectedRowToPopulatedRow(spellbookStack, packet.delta(), sender.registryAccess());
            }
        });
    }
}
