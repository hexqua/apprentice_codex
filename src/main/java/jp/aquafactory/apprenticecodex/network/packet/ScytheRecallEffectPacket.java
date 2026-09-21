package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheRecallRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ScytheRecallEffectPacket(Vec3 start, Vec3 end, int color, boolean narrow, float yaw) {
    public ScytheRecallEffectPacket(Vec3 start, Vec3 end, int color) {
        this(start, end, color, false, 0);
    }
    public static void encode(ScytheRecallEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.start.x); buffer.writeDouble(packet.start.y); buffer.writeDouble(packet.start.z);
        buffer.writeDouble(packet.end.x); buffer.writeDouble(packet.end.y); buffer.writeDouble(packet.end.z);
        buffer.writeInt(packet.color); buffer.writeBoolean(packet.narrow); buffer.writeFloat(packet.yaw);
    }
    public static ScytheRecallEffectPacket decode(FriendlyByteBuf buffer) {
        return new ScytheRecallEffectPacket(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readInt(), buffer.readBoolean(), buffer.readFloat());
    }
    public static void handle(ScytheRecallEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> { if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet); });
    }
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void handle(ScytheRecallEffectPacket packet) {
            ScytheRecallRenderEvent.add(packet.start, packet.end, packet.color, packet.narrow, packet.yaw);
        }
    }
}
