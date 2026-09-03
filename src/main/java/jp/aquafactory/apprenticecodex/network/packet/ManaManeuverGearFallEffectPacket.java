package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.client.ManaManeuverGearFallEffectRenderEvent;
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

public record ManaManeuverGearFallEffectPacket(double x, double y, double z,
                                                float maxRadius) implements CustomPacketPayload {
    public static final Type<ManaManeuverGearFallEffectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_maneuver_gear_fall_effect")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaManeuverGearFallEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ManaManeuverGearFallEffectPacket::decode);

    public ManaManeuverGearFallEffectPacket(Vec3 center, float maxRadius) {
        this(center.x, center.y, center.z, maxRadius);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ManaManeuverGearFallEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.maxRadius);
    }

    private static ManaManeuverGearFallEffectPacket decode(FriendlyByteBuf buffer) {
        return new ManaManeuverGearFallEffectPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    public static void handle(ManaManeuverGearFallEffectPacket packet, IPayloadContext context) {
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

        private static void handle(ManaManeuverGearFallEffectPacket packet) {
            ManaManeuverGearFallEffectRenderEvent.enqueuePulse(
                    new Vec3(packet.x, packet.y, packet.z),
                    packet.maxRadius
            );
        }
    }
}
