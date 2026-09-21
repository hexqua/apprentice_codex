package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncManaSoulTransducerConfigPacket(double castRate, int manaCost) {
    public static void encode(SyncManaSoulTransducerConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.castRate);
        buffer.writeVarInt(packet.manaCost);
    }
    public static SyncManaSoulTransducerConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaSoulTransducerConfigPacket(buffer.readDouble(), buffer.readVarInt());
    }
    public static void handle(SyncManaSoulTransducerConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ManaSoulTransducerConfigState.set(packet.castRate, packet.manaCost)));
        context.setPacketHandled(true);
    }
}
