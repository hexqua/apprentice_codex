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

public final class SyncBoundSwordStatePacket implements CustomPacketPayload {
    public static final Type<SyncBoundSwordStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_bound_sword_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBoundSwordStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncBoundSwordStatePacket::decode);

    private final boolean active;
    private final @Nullable UUID instanceId;
    private final ItemStack storedMainhandStack;
    private final ItemStack storedOffhandStack;
    private final boolean offhandSwordGenerated;
    private final float displayDamage;

    public SyncBoundSwordStatePacket(boolean active, @Nullable UUID instanceId, ItemStack storedMainhandStack,
                                     ItemStack storedOffhandStack, boolean offhandSwordGenerated, float displayDamage) {
        this.active = active;
        this.instanceId = instanceId;
        this.storedMainhandStack = storedMainhandStack.copy();
        this.storedOffhandStack = storedOffhandStack.copy();
        this.offhandSwordGenerated = offhandSwordGenerated;
        this.displayDamage = displayDamage;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncBoundSwordStatePacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.instanceId != null);
        if (packet.instanceId != null) {
            buffer.writeUUID(packet.instanceId);
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.storedMainhandStack);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.storedOffhandStack);
        buffer.writeBoolean(packet.offhandSwordGenerated);
        buffer.writeFloat(packet.displayDamage);
    }

    public static SyncBoundSwordStatePacket decode(RegistryFriendlyByteBuf buffer) {
        var active = buffer.readBoolean();
        UUID instanceId = buffer.readBoolean() ? buffer.readUUID() : null;
        var storedMainhandStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        var storedOffhandStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        var offhandSwordGenerated = buffer.readBoolean();
        var displayDamage = buffer.readFloat();
        return new SyncBoundSwordStatePacket(
                active,
                instanceId,
                storedMainhandStack,
                storedOffhandStack,
                offhandSwordGenerated,
                displayDamage
        );
    }

    public static void handle(SyncBoundSwordStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncBoundSwordStatePacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            Capabilities.getSpellData(player).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.BOUND_SWORD_STATE, state -> {
                        state.active = packet.active;
                        state.setInstanceId(packet.instanceId);
                        state.setStoredMainhandStack(packet.storedMainhandStack);
                        state.setStoredOffhandStack(packet.storedOffhandStack);
                        state.setOffhandSwordGenerated(packet.offhandSwordGenerated);
                        state.displayDamage = packet.displayDamage;
                    })
            );
        }
    }
}
