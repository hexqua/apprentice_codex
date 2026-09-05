package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScytheRecallEffectPacket(Vec3 start, Vec3 end, int color) implements CustomPacketPayload {
    public static final Type<ScytheRecallEffectPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "scythe_recall"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScytheRecallEffectPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVec3(packet.start); buffer.writeVec3(packet.end); buffer.writeInt(packet.color); },
            buffer -> new ScytheRecallEffectPacket(buffer.readVec3(), buffer.readVec3(), buffer.readInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ScytheRecallEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> { if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet); });
    }
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void handle(ScytheRecallEffectPacket packet) {
            jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheRecallRenderEvent.add(packet.start, packet.end, packet.color);
        }
    }
}
