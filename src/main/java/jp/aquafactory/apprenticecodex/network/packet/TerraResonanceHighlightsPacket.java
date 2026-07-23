package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonanceHighlightRenderEvent;
import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonanceSearch;
import net.minecraft.core.BlockPos;
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

public class TerraResonanceHighlightsPacket implements CustomPacketPayload {
    public static final Type<TerraResonanceHighlightsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "terra_resonance_highlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerraResonanceHighlightsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), TerraResonanceHighlightsPacket::decode);

    private final List<BlockPos> targets;

    public TerraResonanceHighlightsPacket(List<BlockPos> targets) {
        if (targets.size() > TerraResonanceSearch.MAX_HIGHLIGHT_TARGETS) {
            throw new IllegalArgumentException("Too many Terra Resonance highlight targets: " + targets.size());
        }
        this.targets = List.copyOf(targets);
    }

    public static void encode(TerraResonanceHighlightsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targets.size());
        for (var target : packet.targets) {
            buffer.writeBlockPos(target);
        }
    }

    public static TerraResonanceHighlightsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        if (size < 0 || size > TerraResonanceSearch.MAX_HIGHLIGHT_TARGETS) {
            throw new IllegalArgumentException("Invalid Terra Resonance highlight target count: " + size);
        }

        var targets = new ArrayList<BlockPos>(size);
        for (var i = 0; i < size; i++) {
            targets.add(buffer.readBlockPos());
        }
        return new TerraResonanceHighlightsPacket(targets);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TerraResonanceHighlightsPacket packet, IPayloadContext context) {
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

        private static void handle(TerraResonanceHighlightsPacket packet) {
            TerraResonanceHighlightRenderEvent.enqueueHighlights(packet.targets);
        }
    }
}
