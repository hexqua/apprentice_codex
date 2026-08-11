package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.alchemybrewer.AlchemyBrewerWaterSupplyRenderEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AlchemyBrewerWaterSupplyEffectPacket(
        BlockPos brewerPos,
        Direction brewerFacing,
        BlockPos targetPos,
        long startGameTime
) implements CustomPacketPayload {
    public static final Type<AlchemyBrewerWaterSupplyEffectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "alchemy_brewer_water_supply_effect")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyBrewerWaterSupplyEffectPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> {
                        buffer.writeBlockPos(packet.brewerPos());
                        buffer.writeEnum(packet.brewerFacing());
                        buffer.writeBlockPos(packet.targetPos());
                        buffer.writeLong(packet.startGameTime());
                    },
                    buffer -> new AlchemyBrewerWaterSupplyEffectPacket(
                            buffer.readBlockPos(),
                            buffer.readEnum(Direction.class),
                            buffer.readBlockPos(),
                            buffer.readLong()
                    )
            );

    public AlchemyBrewerWaterSupplyEffectPacket {
        brewerPos = brewerPos.immutable();
        targetPos = targetPos.immutable();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AlchemyBrewerWaterSupplyEffectPacket packet, IPayloadContext context) {
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

        private static void handle(AlchemyBrewerWaterSupplyEffectPacket packet) {
            AlchemyBrewerWaterSupplyRenderEvent.enqueueEffect(packet);
        }
    }
}
