package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SyncElementalBowOverheatPacket {
    private final CompoundTag data;

    public SyncElementalBowOverheatPacket(@Nullable CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(SyncElementalBowOverheatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncElementalBowOverheatPacket decode(FriendlyByteBuf buffer) {
        return new SyncElementalBowOverheatPacket(buffer.readNbt());
    }

    public static void handle(SyncElementalBowOverheatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncElementalBowOverheatPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            ElementalBowOverheatManager.applySyncedState(player, packet.data);
        }
    }
}
