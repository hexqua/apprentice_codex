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

public final class SyncBoundBowStatePacket implements CustomPacketPayload {
    public static final Type<SyncBoundBowStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_bound_bow_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBoundBowStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncBoundBowStatePacket::decode);

    private final boolean active;
    private final @Nullable UUID instanceId;
    private final ItemStack storedMainhandStack;
    private final int powerLevel;

    public SyncBoundBowStatePacket(boolean active, @Nullable UUID instanceId, ItemStack storedMainhandStack,
                                   int powerLevel) {
        this.active = active;
        this.instanceId = instanceId;
        this.storedMainhandStack = storedMainhandStack.copy();
        this.powerLevel = powerLevel;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncBoundBowStatePacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.instanceId != null);
        if (packet.instanceId != null) {
            buffer.writeUUID(packet.instanceId);
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.storedMainhandStack);
        buffer.writeInt(packet.powerLevel);
    }

    public static SyncBoundBowStatePacket decode(RegistryFriendlyByteBuf buffer) {
        var active = buffer.readBoolean();
        UUID instanceId = buffer.readBoolean() ? buffer.readUUID() : null;
        var storedMainhandStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        var powerLevel = buffer.readInt();
        return new SyncBoundBowStatePacket(active, instanceId, storedMainhandStack, powerLevel);
    }

    public static void handle(SyncBoundBowStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncBoundBowStatePacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            Capabilities.getSpellData(player).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.BOUND_BOW_STATE, state -> {
                        state.active = packet.active;
                        state.setInstanceId(packet.instanceId);
                        state.setStoredMainhandStack(packet.storedMainhandStack);
                        state.powerLevel = packet.powerLevel;
                    })
            );
        }
    }
}
