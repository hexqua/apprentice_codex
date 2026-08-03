package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.GunSpellTracerRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record GunSpellTracerPacket(Vec3 start, Vec3 end, float speedBlocksPerTick, float length)
        implements CustomPacketPayload {
    public static final Type<GunSpellTracerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gun_spell_tracer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunSpellTracerPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), GunSpellTracerPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(GunSpellTracerPacket packet, FriendlyByteBuf buffer) {
        writeVec3(buffer, packet.start());
        writeVec3(buffer, packet.end());
        buffer.writeFloat(packet.speedBlocksPerTick());
        buffer.writeFloat(packet.length());
    }

    private static GunSpellTracerPacket decode(FriendlyByteBuf buffer) {
        return new GunSpellTracerPacket(
                readVec3(buffer),
                readVec3(buffer),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(GunSpellTracerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(GunSpellTracerPacket packet) {
            GunSpellTracerRenderEvent.enqueueTracer(
                    packet.start(),
                    packet.end(),
                    packet.speedBlocksPerTick(),
                    packet.length()
            );
        }
    }
}
