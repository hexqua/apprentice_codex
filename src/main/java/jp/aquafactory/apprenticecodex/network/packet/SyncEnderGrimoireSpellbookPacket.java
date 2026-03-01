package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncEnderGrimoireSpellbookPacket implements CustomPacketPayload {
    public static final Type<SyncEnderGrimoireSpellbookPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_ender_grimoire_spellbook"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEnderGrimoireSpellbookPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncEnderGrimoireSpellbookPacket::decode);

    private final CompoundTag data;

    public SyncEnderGrimoireSpellbookPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncEnderGrimoireSpellbookPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    private static SyncEnderGrimoireSpellbookPacket decode(FriendlyByteBuf buffer) {
        var data = buffer.readNbt();
        return new SyncEnderGrimoireSpellbookPacket(data);
    }

    public static void handle(SyncEnderGrimoireSpellbookPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncEnderGrimoireSpellbookPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            Capabilities.getEnderGrimoireSpellbook(player).ifPresent(data -> data.load(packet.data.copy()));
            ClientMagicData.updateSpellSelectionManager();
        }
    }
}
