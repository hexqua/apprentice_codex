package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SyncEdgeDancerStatePacket implements CustomPacketPayload {
    public static final Type<SyncEdgeDancerStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_edge_dancer_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEdgeDancerStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncEdgeDancerStatePacket::decode);

    private final boolean active;
    private final @Nullable UUID instanceId;
    private final ItemStack storedOffhandStack;
    private final boolean hadStoredOffhand;

    public SyncEdgeDancerStatePacket(boolean active, @Nullable UUID instanceId, ItemStack storedOffhandStack,
                                     boolean hadStoredOffhand) {
        this.active = active;
        this.instanceId = instanceId;
        this.storedOffhandStack = storedOffhandStack.copy();
        this.hadStoredOffhand = hadStoredOffhand;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncEdgeDancerStatePacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.instanceId != null);
        if (packet.instanceId != null) {
            buffer.writeUUID(packet.instanceId);
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.storedOffhandStack);
        buffer.writeBoolean(packet.hadStoredOffhand);
    }

    public static SyncEdgeDancerStatePacket decode(RegistryFriendlyByteBuf buffer) {
        var active = buffer.readBoolean();
        UUID instanceId = buffer.readBoolean() ? buffer.readUUID() : null;
        var storedOffhandStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        var hadStoredOffhand = buffer.readBoolean();
        return new SyncEdgeDancerStatePacket(active, instanceId, storedOffhandStack, hadStoredOffhand);
    }

    public static void handle(SyncEdgeDancerStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncEdgeDancerStatePacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            Capabilities.getSpellData(player).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.EDGE_DANCER_STATE, state -> {
                        state.active = packet.active;
                        state.setInstanceId(packet.instanceId);
                        state.setStoredOffhandStack(packet.storedOffhandStack);
                        state.setHadStoredOffhand(packet.hadStoredOffhand);
                    })
            );
        }
    }
}
