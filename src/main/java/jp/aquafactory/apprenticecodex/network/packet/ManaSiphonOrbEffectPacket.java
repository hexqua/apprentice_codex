package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.ManaSiphonOrbRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ManaSiphonOrbEffectPacket {
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

    public static void encode(ManaSiphonOrbEffectPacket packet, FriendlyByteBuf buffer) {
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

    public static ManaSiphonOrbEffectPacket decode(FriendlyByteBuf buffer) {
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

    public static void handle(ManaSiphonOrbEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(ManaSiphonOrbEffectPacket packet) {
            ManaSiphonOrbRenderEvent.enqueueEffect(
                    new Vec3(packet.impactX, packet.impactY, packet.impactZ),
                    packet.ownerEntityId,
                    packet.orbs
            );
        }
    }

    public record OrbData(float scatterX, float scatterY, float scatterZ, int returnDelayTicks, int returnDurationTicks,
                          float scale, float phaseOffset) {
    }
}
