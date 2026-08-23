package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorClientDisplay;
import jp.aquafactory.apprenticecodex.spell.deepsensor.DeepSensorObservationBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DeepSensorObservationsPacket {
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

    public static void encode(DeepSensorObservationsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension.location());
        buffer.writeVarInt(packet.observations.size());
        for (var observation : packet.observations) {
            buffer.writeBlockPos(observation.position());
            buffer.writeFloat(observation.distance());
            buffer.writeLong(observation.expiresAtGameTime());
        }
    }

    public static DeepSensorObservationsPacket decode(FriendlyByteBuf buffer) {
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

    public static void handle(DeepSensorObservationsPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
