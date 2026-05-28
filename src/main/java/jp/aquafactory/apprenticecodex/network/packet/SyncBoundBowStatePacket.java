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

public class SyncBoundBowStatePacket {
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

    public static void encode(SyncBoundBowStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.instanceId != null);
        if (packet.instanceId != null) {
            buffer.writeUUID(packet.instanceId);
        }
        buffer.writeItem(packet.storedMainhandStack);
        buffer.writeInt(packet.powerLevel);
    }

    public static SyncBoundBowStatePacket decode(FriendlyByteBuf buffer) {
        var active = buffer.readBoolean();
        UUID instanceId = buffer.readBoolean() ? buffer.readUUID() : null;
        var storedMainhandStack = buffer.readItem();
        var powerLevel = buffer.readInt();
        return new SyncBoundBowStatePacket(active, instanceId, storedMainhandStack, powerLevel);
    }

    public static void handle(SyncBoundBowStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncBoundBowStatePacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.getCapability(Capabilities.SPELL_DATA).ifPresent(data ->
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
