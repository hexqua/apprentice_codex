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
import java.util.Map;
import java.util.function.Supplier;

public class SyncSchoolAffinityAssignmentsPacket {
    private final List<ResourceLocation> schoolIdsBySlot;
    private final Map<ResourceLocation, Integer> catalystSlotsByItemId;

    public SyncSchoolAffinityAssignmentsPacket(
            List<ResourceLocation> schoolIdsBySlot,
            Map<ResourceLocation, Integer> catalystSlotsByItemId
    ) {
        this.schoolIdsBySlot = java.util.Collections.unmodifiableList(new ArrayList<>(schoolIdsBySlot));
        this.catalystSlotsByItemId = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(catalystSlotsByItemId));
    }

    public static void encode(SyncSchoolAffinityAssignmentsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.schoolIdsBySlot.size());
        for (var schoolId : packet.schoolIdsBySlot) {
            buffer.writeBoolean(schoolId != null);
            if (schoolId != null) {
                buffer.writeResourceLocation(schoolId);
            }
        }

        buffer.writeVarInt(packet.catalystSlotsByItemId.size());
        for (var entry : packet.catalystSlotsByItemId.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }
    }

    public static SyncSchoolAffinityAssignmentsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var schoolIdsBySlot = new ArrayList<ResourceLocation>(size);
        for (int i = 0; i < size; i++) {
            schoolIdsBySlot.add(buffer.readBoolean() ? buffer.readResourceLocation() : null);
        }

        var catalystBindingCount = buffer.readVarInt();
        var catalystSlotsByItemId = new java.util.LinkedHashMap<ResourceLocation, Integer>(catalystBindingCount);
        for (int i = 0; i < catalystBindingCount; i++) {
            catalystSlotsByItemId.put(buffer.readResourceLocation(), buffer.readVarInt());
        }

        return new SyncSchoolAffinityAssignmentsPacket(schoolIdsBySlot, catalystSlotsByItemId);
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
            SchoolAffinityRegistry.applySyncedAssignments(packet.schoolIdsBySlot, packet.catalystSlotsByItemId);
        }
    }
}
