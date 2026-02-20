package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncEnderGrimoireSpellbookPacket {
    private final CompoundTag data;

    public SyncEnderGrimoireSpellbookPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(SyncEnderGrimoireSpellbookPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncEnderGrimoireSpellbookPacket decode(FriendlyByteBuf buffer) {
        var data = buffer.readNbt();
        return new SyncEnderGrimoireSpellbookPacket(data);
    }

    public static void handle(SyncEnderGrimoireSpellbookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncEnderGrimoireSpellbookPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(data -> data.load(packet.data.copy()));
            ClientMagicData.updateSpellSelectionManager();
        }
    }
}
