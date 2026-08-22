package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerWaterSupplyRenderEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AlchemyBrewerWaterSupplyEffectPacket(
        BlockPos brewerPos,
        Direction brewerFacing,
        BlockPos targetPos,
        long startGameTime
) {

    public AlchemyBrewerWaterSupplyEffectPacket {
        brewerPos = brewerPos.immutable();
        targetPos = targetPos.immutable();
    }

    public static void encode(AlchemyBrewerWaterSupplyEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.brewerPos());
        buffer.writeEnum(packet.brewerFacing());
        buffer.writeBlockPos(packet.targetPos());
        buffer.writeLong(packet.startGameTime());
    }

    public static AlchemyBrewerWaterSupplyEffectPacket decode(FriendlyByteBuf buffer) {
        return new AlchemyBrewerWaterSupplyEffectPacket(
                buffer.readBlockPos(),
                buffer.readEnum(Direction.class),
                buffer.readBlockPos(),
                buffer.readLong()
        );
    }

    public static void handle(AlchemyBrewerWaterSupplyEffectPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(AlchemyBrewerWaterSupplyEffectPacket packet) {
            AlchemyBrewerWaterSupplyRenderEvent.enqueueEffect(packet);
        }
    }
}
