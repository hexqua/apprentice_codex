package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.block.atelierstation.AtelierStationFluidRenderEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record AtelierStationFluidEffectPacket(EffectKind kind, long startGameTime, BlockPos stationPos,
                                              Direction stationFacing, BlockPos sourcePos, Direction sourceFacing,
                                              int targetEntityId, List<SupplyOrbData> supplyOrbs) {
    public AtelierStationFluidEffectPacket {
        supplyOrbs = List.copyOf(supplyOrbs);
    }

    public static AtelierStationFluidEffectPacket createCauldronToStation(BlockPos stationPos, Direction stationFacing,
                                                                          BlockPos cauldronPos, long startGameTime) {
        return new AtelierStationFluidEffectPacket(
                EffectKind.CAULDRON_TO_STATION,
                startGameTime,
                stationPos.immutable(),
                stationFacing,
                cauldronPos.immutable(),
                Direction.NORTH,
                -1,
                List.of()
        );
    }

    public static AtelierStationFluidEffectPacket createAlchemyBrewerToStation(
            BlockPos stationPos,
            Direction stationFacing,
            BlockPos brewerPos,
            Direction brewerFacing,
            long startGameTime
    ) {
        return new AtelierStationFluidEffectPacket(
                EffectKind.ALCHEMY_BREWER_TO_STATION,
                startGameTime,
                stationPos.immutable(),
                stationFacing,
                brewerPos.immutable(),
                brewerFacing,
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
                stationFacing,
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
        buffer.writeEnum(packet.sourceFacing);
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
        var sourceFacing = buffer.readEnum(Direction.class);
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
                sourceFacing, targetEntityId, supplyOrbs);
    }

    public static void handle(AtelierStationFluidEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    public enum EffectKind {
        CAULDRON_TO_STATION,
        ALCHEMY_BREWER_TO_STATION,
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
