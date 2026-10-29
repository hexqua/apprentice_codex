package jp.aquafactory.apprenticecodex.network.packet;

import java.util.UUID;
import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsClientState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.MotionData;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record SyncPsychicDriveGauntletsPacket(UUID player, boolean reset, Snapshot right, Snapshot left, Mode mode) {
    public SyncPsychicDriveGauntletsPacket(UUID player, boolean reset, Snapshot right, Snapshot left) {
        this(player, reset, right, left, Mode.HIDDEN);
    }

    public static void encode(SyncPsychicDriveGauntletsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.player);
        buffer.writeBoolean(packet.reset);
        write(buffer, packet.right);
        write(buffer, packet.left);
        buffer.writeEnum(packet.mode);
    }

    public static SyncPsychicDriveGauntletsPacket decode(FriendlyByteBuf buffer) {
        return new SyncPsychicDriveGauntletsPacket(buffer.readUUID(), buffer.readBoolean(), read(buffer), read(buffer),
                buffer.readEnum(Mode.class));
    }

    private static void write(FriendlyByteBuf buffer, Snapshot arm) {
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

    private static Snapshot read(FriendlyByteBuf buffer) {
        return new Snapshot(buffer.readEnum(Phase.class), new Vec3(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble()), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readLong(), buffer.readLong(),
                buffer.readFloat(), buffer.readLong(), buffer.readFloat(), buffer.readEnum(Impact.class), buffer.readLong(),
                buffer.readVarInt(), new MotionData(buffer.readDouble(), readVector(buffer), readVector(buffer), readVector(buffer),
                buffer.readFloat(), buffer.readFloat()));
    }

    private static void writeVector(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(SyncPsychicDriveGauntletsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.accept(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(SyncPsychicDriveGauntletsPacket packet) {
            PsychicDriveGauntletsClientState.accept(packet);
        }
    }
}
