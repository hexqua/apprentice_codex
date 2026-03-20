package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationFluidRenderEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record AtelierStationFluidEffectPacket(EffectKind kind, long startGameTime, BlockPos stationPos,
                                              Direction stationFacing, BlockPos sourcePos, int targetEntityId, List<SupplyOrbData> supplyOrbs)
        implements CustomPacketPayload {
    public static final Type<AtelierStationFluidEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "atelier_station_fluid_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AtelierStationFluidEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), AtelierStationFluidEffectPacket::decode);

    public AtelierStationFluidEffectPacket {
        supplyOrbs = List.copyOf(supplyOrbs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static AtelierStationFluidEffectPacket createCauldronToStation(BlockPos stationPos, Direction stationFacing,
                                                                          BlockPos cauldronPos, long startGameTime) {
        return new AtelierStationFluidEffectPacket(
                EffectKind.CAULDRON_TO_STATION,
                startGameTime,
                stationPos.immutable(),
                stationFacing,
                cauldronPos.immutable(),
                -1,
                List.of()
        );
    }

    public static AtelierStationFluidEffectPacket createStationToPlayer(BlockPos stationPos, Direction stationFacing,
                                                                        int targetEntityId, long startGameTime,
                                                                        List<SupplyOrbData> supplyOrbs) {
        return new AtelierStationFluidEffectPacket(
                EffectKind.STATION_TO_PLAYER,
                startGameTime,
                stationPos.immutable(),
                stationFacing,
                stationPos.immutable(),
                targetEntityId,
                supplyOrbs
        );
    }

    public static void encode(AtelierStationFluidEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.kind);
        buffer.writeLong(packet.startGameTime);
        buffer.writeBlockPos(packet.stationPos);
        buffer.writeEnum(packet.stationFacing);
        buffer.writeBlockPos(packet.sourcePos);
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeVarInt(packet.supplyOrbs.size());
        for (var orb : packet.supplyOrbs) {
            buffer.writeFloat(orb.controlOffsetX());
            buffer.writeFloat(orb.controlOffsetY());
            buffer.writeFloat(orb.controlOffsetZ());
            buffer.writeVarInt(orb.startDelayTicks());
            buffer.writeVarInt(orb.durationTicks());
            buffer.writeFloat(orb.spinOffsetDegrees());
            buffer.writeFloat(orb.spinSpeedDegreesPerTick());
        }
    }

    public static AtelierStationFluidEffectPacket decode(FriendlyByteBuf buffer) {
        var kind = buffer.readEnum(EffectKind.class);
        var startGameTime = buffer.readLong();
        var stationPos = buffer.readBlockPos();
        var stationFacing = buffer.readEnum(Direction.class);
        var sourcePos = buffer.readBlockPos();
        var targetEntityId = buffer.readVarInt();
        var orbCount = buffer.readVarInt();
        var supplyOrbs = new ArrayList<SupplyOrbData>(orbCount);
        for (var i = 0; i < orbCount; ++i) {
            supplyOrbs.add(new SupplyOrbData(
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat()
            ));
        }
        return new AtelierStationFluidEffectPacket(kind, startGameTime, stationPos, stationFacing, sourcePos,
                targetEntityId, supplyOrbs);
    }

    public static void handle(AtelierStationFluidEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    public enum EffectKind {
        CAULDRON_TO_STATION,
        STATION_TO_PLAYER
    }

    public record SupplyOrbData(float controlOffsetX, float controlOffsetY, float controlOffsetZ, int startDelayTicks,
                                int durationTicks, float spinOffsetDegrees, float spinSpeedDegreesPerTick) {
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(AtelierStationFluidEffectPacket packet) {
            AtelierStationFluidRenderEvent.enqueueEffect(packet);
        }
    }
}
