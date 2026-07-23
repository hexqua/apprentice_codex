package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonanceHighlightRenderEvent;
import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonanceSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TerraResonanceHighlightsPacket {
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

    public static void handle(TerraResonanceHighlightsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(TerraResonanceHighlightsPacket packet) {
            TerraResonanceHighlightRenderEvent.enqueueHighlights(packet.targets);
        }
    }
}
