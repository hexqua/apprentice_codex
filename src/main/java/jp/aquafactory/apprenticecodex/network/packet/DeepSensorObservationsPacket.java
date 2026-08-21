package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorClientDisplay;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorObservationBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DeepSensorObservationsPacket implements CustomPacketPayload {
    public static final Type<DeepSensorObservationsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "deep_sensor_observations"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeepSensorObservationsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), DeepSensorObservationsPacket::decode);

    private final ResourceKey<Level> dimension;
    private final List<Observation> observations;

    public DeepSensorObservationsPacket(ResourceKey<Level> dimension,
                                        List<DeepSensorObservationBuffer.DisplayObservation> observations) {
        this.dimension = dimension;
        this.observations = observations.stream()
                .map(observation -> new Observation(
                        observation.position(), observation.distance(), observation.expiresAtGameTime()))
                .toList();
        validateSize(this.observations.size());
    }

    private DeepSensorObservationsPacket(ResourceKey<Level> dimension, ArrayList<Observation> observations) {
        validateSize(observations.size());
        this.dimension = dimension;
        this.observations = List.copyOf(observations);
    }

    private static void validateSize(int size) {
        if (size > DeepSensorObservationBuffer.MAX_DISPLAYED_OBSERVATIONS) {
            throw new IllegalArgumentException("Too many Deep Sensor observations: " + size);
        }
    }

    private static void encode(DeepSensorObservationsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension.location());
        buffer.writeVarInt(packet.observations.size());
        for (var observation : packet.observations) {
            buffer.writeBlockPos(observation.position());
            buffer.writeFloat(observation.distance());
            buffer.writeLong(observation.expiresAtGameTime());
        }
    }

    private static DeepSensorObservationsPacket decode(FriendlyByteBuf buffer) {
        var dimension = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
        var size = buffer.readVarInt();
        if (size < 0 || size > DeepSensorObservationBuffer.MAX_DISPLAYED_OBSERVATIONS) {
            throw new IllegalArgumentException("Invalid Deep Sensor observation count: " + size);
        }
        var observations = new ArrayList<Observation>(size);
        for (var i = 0; i < size; i++) {
            observations.add(new Observation(buffer.readBlockPos(), buffer.readFloat(), buffer.readLong()));
        }
        return new DeepSensorObservationsPacket(dimension, observations);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeepSensorObservationsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    public record Observation(BlockPos position, float distance, long expiresAtGameTime) {
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(DeepSensorObservationsPacket packet) {
            DeepSensorClientDisplay.replace(packet.dimension, packet.observations);
        }
    }
}
