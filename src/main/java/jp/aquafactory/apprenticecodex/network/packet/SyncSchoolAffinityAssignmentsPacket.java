package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncSchoolAffinityAssignmentsPacket {
    private final List<ResourceLocation> schoolIdsBySlot;

    public SyncSchoolAffinityAssignmentsPacket(List<ResourceLocation> schoolIdsBySlot) {
        this.schoolIdsBySlot = java.util.Collections.unmodifiableList(new ArrayList<>(schoolIdsBySlot));
    }

    public static void encode(SyncSchoolAffinityAssignmentsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.schoolIdsBySlot.size());
        for (var schoolId : packet.schoolIdsBySlot) {
            buffer.writeBoolean(schoolId != null);
            if (schoolId != null) {
                buffer.writeResourceLocation(schoolId);
            }
        }
    }

    public static SyncSchoolAffinityAssignmentsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var schoolIdsBySlot = new ArrayList<ResourceLocation>(size);
        for (int i = 0; i < size; i++) {
            schoolIdsBySlot.add(buffer.readBoolean() ? buffer.readResourceLocation() : null);
        }
        return new SyncSchoolAffinityAssignmentsPacket(schoolIdsBySlot);
    }

    public static void handle(SyncSchoolAffinityAssignmentsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncSchoolAffinityAssignmentsPacket packet) {
            SchoolAffinityRegistry.applySyncedAssignments(packet.schoolIdsBySlot);
        }
    }
}
