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

public class SyncBoundSwordStatePacket {
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

    public static void encode(SyncBoundSwordStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.instanceId != null);
        if (packet.instanceId != null) {
            buffer.writeUUID(packet.instanceId);
        }
        buffer.writeItem(packet.storedMainhandStack);
        buffer.writeItem(packet.storedOffhandStack);
        buffer.writeBoolean(packet.offhandSwordGenerated);
        buffer.writeFloat(packet.displayDamage);
    }

    public static SyncBoundSwordStatePacket decode(FriendlyByteBuf buffer) {
        var active = buffer.readBoolean();
        UUID instanceId = buffer.readBoolean() ? buffer.readUUID() : null;
        var storedMainhandStack = buffer.readItem();
        var storedOffhandStack = buffer.readItem();
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

    public static void handle(SyncBoundSwordStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncBoundSwordStatePacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.getCapability(Capabilities.SPELL_DATA).ifPresent(data ->
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
