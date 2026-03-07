package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SenseEvilHighlightsPacket {
    private final List<TargetData> targets;

    public SenseEvilHighlightsPacket(List<TargetData> targets) {
        this.targets = List.copyOf(targets);
    }

    public static void encode(SenseEvilHighlightsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targets.size());
        for (var target : packet.targets) {
            buffer.writeDouble(target.position().x);
            buffer.writeDouble(target.position().y);
            buffer.writeDouble(target.position().z);
            buffer.writeFloat(target.scale());
        }
    }

    public static SenseEvilHighlightsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var targets = new ArrayList<TargetData>(size);
        for (int i = 0; i < size; i++) {
            targets.add(new TargetData(
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    buffer.readFloat()
            ));
        }
        return new SenseEvilHighlightsPacket(targets);
    }

    public static void handle(SenseEvilHighlightsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    public record TargetData(Vec3 position, float scale) {
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SenseEvilHighlightsPacket packet) {
            var targets = new ArrayList<SenseEvilHighlightRenderEvent.HighlightTarget>(packet.targets.size());
            for (var target : packet.targets) {
                targets.add(new SenseEvilHighlightRenderEvent.HighlightTarget(target.position(), target.scale()));
            }
            SenseEvilHighlightRenderEvent.enqueueHighlights(targets);
        }
    }
}
