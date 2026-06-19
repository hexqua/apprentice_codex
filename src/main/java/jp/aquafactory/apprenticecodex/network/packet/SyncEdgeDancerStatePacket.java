package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncEdgeDancerStatePacket {
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

    public static void encode(SyncEdgeDancerStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.instanceId != null);
        if (packet.instanceId != null) {
            buffer.writeUUID(packet.instanceId);
        }
        buffer.writeItem(packet.storedOffhandStack);
        buffer.writeBoolean(packet.hadStoredOffhand);
    }

    public static SyncEdgeDancerStatePacket decode(FriendlyByteBuf buffer) {
        var active = buffer.readBoolean();
        UUID instanceId = buffer.readBoolean() ? buffer.readUUID() : null;
        var storedOffhandStack = buffer.readItem();
        var hadStoredOffhand = buffer.readBoolean();
        return new SyncEdgeDancerStatePacket(active, instanceId, storedOffhandStack, hadStoredOffhand);
    }

    public static void handle(SyncEdgeDancerStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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

            player.getCapability(Capabilities.SPELL_DATA).ifPresent(data ->
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
