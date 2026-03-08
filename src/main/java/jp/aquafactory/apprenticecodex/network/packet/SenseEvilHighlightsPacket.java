package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightRenderEvent;
import jp.aquafactory.apprenticecodex.spell.senseevil.SenseEvilHighlightVariant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class SenseEvilHighlightsPacket implements CustomPacketPayload {
    public static final Type<SenseEvilHighlightsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sense_evil_highlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SenseEvilHighlightsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SenseEvilHighlightsPacket::decode);

    private final List<TargetData> targets;

    public SenseEvilHighlightsPacket(List<TargetData> targets) {
        this.targets = List.copyOf(targets);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SenseEvilHighlightsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targets.size());
        for (var target : packet.targets) {
            buffer.writeDouble(target.position().x);
            buffer.writeDouble(target.position().y);
            buffer.writeDouble(target.position().z);
            buffer.writeFloat(target.scale());
            buffer.writeVarInt(target.variant().toNetworkId());
        }
    }

    public static SenseEvilHighlightsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var targets = new ArrayList<TargetData>(size);
        for (int i = 0; i < size; i++) {
            targets.add(new TargetData(
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    buffer.readFloat(),
                    SenseEvilHighlightVariant.byNetworkId(buffer.readVarInt())
            ));
        }
        return new SenseEvilHighlightsPacket(targets);
    }

    public static void handle(SenseEvilHighlightsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    public record TargetData(Vec3 position, float scale, SenseEvilHighlightVariant variant) {
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SenseEvilHighlightsPacket packet) {
            var targets = new ArrayList<SenseEvilHighlightRenderEvent.HighlightTarget>(packet.targets.size());
            for (var target : packet.targets) {
                targets.add(new SenseEvilHighlightRenderEvent.HighlightTarget(target.position(), target.scale(), target.variant()));
            }
            SenseEvilHighlightRenderEvent.enqueueHighlights(targets);
        }
    }
}
