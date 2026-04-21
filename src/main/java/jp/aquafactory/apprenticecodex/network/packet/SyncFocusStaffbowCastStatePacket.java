package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientCastState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SyncFocusStaffbowCastStatePacket {
    private final CompoundTag data;

    public SyncFocusStaffbowCastStatePacket(@Nullable CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(SyncFocusStaffbowCastStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncFocusStaffbowCastStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncFocusStaffbowCastStatePacket(buffer.readNbt());
    }

    public static void handle(SyncFocusStaffbowCastStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncFocusStaffbowCastStatePacket packet) {
            if (Minecraft.getInstance().player == null) {
                return;
            }

            FocusStaffbowClientCastState.applySyncedState(packet.data);
        }
    }
}
