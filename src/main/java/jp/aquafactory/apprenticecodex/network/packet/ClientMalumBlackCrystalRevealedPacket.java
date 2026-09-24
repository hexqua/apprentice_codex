package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientMalumBlackCrystalRevealedPacket() implements CustomPacketPayload {
    public static final Type<ClientMalumBlackCrystalRevealedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_malum_black_crystal_revealed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMalumBlackCrystalRevealedPacket> STREAM_CODEC =
            StreamCodec.unit(new ClientMalumBlackCrystalRevealedPacket());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientMalumBlackCrystalRevealedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AdvancementTools.award(player, AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED,
                        AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED_CRITERION);
            }
        });
    }
}
