package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
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
import java.util.Map;

public class SyncSchoolAffinityAssignmentsPacket implements CustomPacketPayload {
    public static final Type<SyncSchoolAffinityAssignmentsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_school_affinity_assignments"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSchoolAffinityAssignmentsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncSchoolAffinityAssignmentsPacket::decode);

    private final List<ResourceLocation> schoolIdsBySlot;
    private final Map<ResourceLocation, Integer> catalystSlotsByItemId;

    public SyncSchoolAffinityAssignmentsPacket(
            List<ResourceLocation> schoolIdsBySlot,
            Map<ResourceLocation, Integer> catalystSlotsByItemId
    ) {
        this.schoolIdsBySlot = java.util.Collections.unmodifiableList(new ArrayList<>(schoolIdsBySlot));
        this.catalystSlotsByItemId = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(catalystSlotsByItemId));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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

    public static void handle(SyncSchoolAffinityAssignmentsPacket packet, IPayloadContext context) {
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

        private static void handle(SyncSchoolAffinityAssignmentsPacket packet) {
            SchoolAffinityRegistry.applySyncedAssignments(packet.schoolIdsBySlot, packet.catalystSlotsByItemId);
        }
    }
}
