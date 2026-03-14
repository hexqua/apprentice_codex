package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.ManaSiphonOrbRenderEvent;
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

public class ManaSiphonOrbEffectPacket implements CustomPacketPayload {
    public static final Type<ManaSiphonOrbEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_siphon_orb_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaSiphonOrbEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ManaSiphonOrbEffectPacket::decode);

    private final double impactX;
    private final double impactY;
    private final double impactZ;
    private final int ownerEntityId;
    private final List<OrbData> orbs;

    public ManaSiphonOrbEffectPacket(Vec3 impactPosition, int ownerEntityId, List<OrbData> orbs) {
        this(impactPosition.x, impactPosition.y, impactPosition.z, ownerEntityId, List.copyOf(orbs));
    }

    private ManaSiphonOrbEffectPacket(double impactX, double impactY, double impactZ, int ownerEntityId, List<OrbData> orbs) {
        this.impactX = impactX;
        this.impactY = impactY;
        this.impactZ = impactZ;
        this.ownerEntityId = ownerEntityId;
        this.orbs = orbs;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ManaSiphonOrbEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.impactX);
        buffer.writeDouble(packet.impactY);
        buffer.writeDouble(packet.impactZ);
        buffer.writeVarInt(packet.ownerEntityId);
        buffer.writeVarInt(packet.orbs.size());
        for (var orb : packet.orbs) {
            buffer.writeFloat(orb.scatterX());
            buffer.writeFloat(orb.scatterY());
            buffer.writeFloat(orb.scatterZ());
            buffer.writeVarInt(orb.returnDelayTicks());
            buffer.writeVarInt(orb.returnDurationTicks());
            buffer.writeFloat(orb.scale());
            buffer.writeFloat(orb.phaseOffset());
        }
    }

    private static ManaSiphonOrbEffectPacket decode(FriendlyByteBuf buffer) {
        var impactX = buffer.readDouble();
        var impactY = buffer.readDouble();
        var impactZ = buffer.readDouble();
        var ownerEntityId = buffer.readVarInt();
        var orbCount = buffer.readVarInt();
        var orbs = new ArrayList<OrbData>(orbCount);
        for (int i = 0; i < orbCount; i++) {
            orbs.add(new OrbData(
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat()
            ));
        }
        return new ManaSiphonOrbEffectPacket(impactX, impactY, impactZ, ownerEntityId, orbs);
    }

    public static void handle(ManaSiphonOrbEffectPacket packet, IPayloadContext context) {
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

        private static void handle(ManaSiphonOrbEffectPacket packet) {
            ManaSiphonOrbRenderEvent.enqueueEffect(
                    new Vec3(packet.impactX, packet.impactY, packet.impactZ),
                    packet.ownerEntityId,
                    packet.orbs
            );
        }
    }

    public record OrbData(
            float scatterX,
            float scatterY,
            float scatterZ,
            int returnDelayTicks,
            int returnDurationTicks,
            float scale,
            float phaseOffset
    ) {
    }
}
