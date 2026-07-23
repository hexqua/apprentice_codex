package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonancePulseRenderEvent;
import net.minecraft.core.Direction;
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

public class TerraResonancePulsePacket implements CustomPacketPayload {
    public static final Type<TerraResonancePulsePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "terra_resonance_pulse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerraResonancePulsePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), TerraResonancePulsePacket::decode);

    private final double x;
    private final double y;
    private final double z;
    private final Direction selectedFace;
    private final float maxRadius;

    public TerraResonancePulsePacket(Vec3 center, Direction selectedFace, float maxRadius) {
        this(center.x, center.y, center.z, selectedFace, maxRadius);
    }

    private TerraResonancePulsePacket(double x, double y, double z, Direction selectedFace, float maxRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.selectedFace = selectedFace;
        this.maxRadius = maxRadius;
    }

    public static void encode(TerraResonancePulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeEnum(packet.selectedFace);
        buffer.writeFloat(packet.maxRadius);
    }

    public static TerraResonancePulsePacket decode(FriendlyByteBuf buffer) {
        return new TerraResonancePulsePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readEnum(Direction.class),
                buffer.readFloat()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerraResonancePulsePacket packet, IPayloadContext context) {
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

        private static void handle(TerraResonancePulsePacket packet) {
            TerraResonancePulseRenderEvent.enqueuePulse(
                    new Vec3(packet.x, packet.y, packet.z),
                    packet.selectedFace,
                    packet.maxRadius
            );
        }
    }
}
