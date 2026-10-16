package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsClientState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.MotionData;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SyncPsychicDriveGauntletsPacket(UUID player, boolean reset, Snapshot right, Snapshot left, Mode mode) implements CustomPacketPayload {
    public SyncPsychicDriveGauntletsPacket(UUID player, boolean reset, Snapshot right, Snapshot left) {
        this(player, reset, right, left, Mode.HIDDEN);
    }
    public static final Type<SyncPsychicDriveGauntletsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_psychic_drive_gauntlets"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPsychicDriveGauntletsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeUUID(packet.player);
                buffer.writeBoolean(packet.reset);
                write(buffer, packet.right);
                write(buffer, packet.left);
                buffer.writeEnum(packet.mode);
            }, buffer -> new SyncPsychicDriveGauntletsPacket(buffer.readUUID(), buffer.readBoolean(), read(buffer), read(buffer),
                    buffer.readEnum(Mode.class)));
    private static void write(RegistryFriendlyByteBuf buffer, Snapshot arm) {
        buffer.writeEnum(arm.phase());
        buffer.writeDouble(arm.position().x);
        buffer.writeDouble(arm.position().y);
        buffer.writeDouble(arm.position().z);
        buffer.writeFloat(arm.yaw());
        buffer.writeFloat(arm.pitch());
        buffer.writeFloat(arm.drawnAmount());
        buffer.writeDouble(arm.flightStart().x);
        buffer.writeDouble(arm.flightStart().y);
        buffer.writeDouble(arm.flightStart().z);
        buffer.writeDouble(arm.destination().x);
        buffer.writeDouble(arm.destination().y);
        buffer.writeDouble(arm.destination().z);
        buffer.writeLong(arm.launchTick());
        buffer.writeLong(arm.sampleTick());
        buffer.writeFloat(arm.charge());
        buffer.writeLong(arm.recoveryStartedTick());
        buffer.writeFloat(arm.returnRecovery());
        buffer.writeEnum(arm.impact());
        buffer.writeLong(arm.impactTick());
        buffer.writeVarInt(arm.holdTicks());
        buffer.writeDouble(arm.motion().phaseStartedTick());
        writeVector(buffer, arm.motion().returnStart());
        writeVector(buffer, arm.motion().returnEnd());
        writeVector(buffer, arm.motion().returnSide());
        buffer.writeFloat(arm.motion().returnPower());
        buffer.writeFloat(arm.motion().returnTwist());
    }
    private static Snapshot read(RegistryFriendlyByteBuf buffer) {
        return new Snapshot(buffer.readEnum(Phase.class), new Vec3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readLong(), buffer.readLong(),
                buffer.readFloat(), buffer.readLong(), buffer.readFloat(), buffer.readEnum(Impact.class), buffer.readLong(),
                buffer.readVarInt(), new MotionData(buffer.readDouble(), readVector(buffer), readVector(buffer), readVector(buffer),
                        buffer.readFloat(), buffer.readFloat()));
    }

    private static void writeVector(RegistryFriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncPsychicDriveGauntletsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.accept(packet);
        });
    }
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(SyncPsychicDriveGauntletsPacket packet) {
            PsychicDriveGauntletsClientState.accept(packet);
        }
    }
}
