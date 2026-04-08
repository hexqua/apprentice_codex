package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTamersPocketCountPacket {
    private final int storedPetCount;

    public SyncTamersPocketCountPacket(int storedPetCount) {
        this.storedPetCount = storedPetCount;
    }

    public static void encode(SyncTamersPocketCountPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.storedPetCount);
    }

    public static SyncTamersPocketCountPacket decode(FriendlyByteBuf buffer) {
        return new SyncTamersPocketCountPacket(buffer.readVarInt());
    }

    public static void handle(SyncTamersPocketCountPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncTamersPocketCountPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.getCapability(Capabilities.SPELL_DATA).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE, state ->
                            state.setClientSyncedStoredPetCount(packet.storedPetCount)
                    )
            );
        }
    }
}
