package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncMirageAvoidanceStatePacket {
    private final int entityId;
    private final long startGameTime;
    private final long activeUntilGameTime;
    private final long invulnerableUntilGameTime;
    private final float movementForward;
    private final float movementStrafe;

    public SyncMirageAvoidanceStatePacket(int entityId, long startGameTime, long activeUntilGameTime, long invulnerableUntilGameTime,
                                          float movementForward, float movementStrafe) {
        this.entityId = entityId;
        this.startGameTime = startGameTime;
        this.activeUntilGameTime = activeUntilGameTime;
        this.invulnerableUntilGameTime = invulnerableUntilGameTime;
        this.movementForward = movementForward;
        this.movementStrafe = movementStrafe;
    }

    public static void encode(SyncMirageAvoidanceStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeLong(packet.startGameTime);
        buffer.writeLong(packet.activeUntilGameTime);
        buffer.writeLong(packet.invulnerableUntilGameTime);
        buffer.writeFloat(packet.movementForward);
        buffer.writeFloat(packet.movementStrafe);
    }

    public static SyncMirageAvoidanceStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncMirageAvoidanceStatePacket(
                buffer.readInt(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readLong(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncMirageAvoidanceStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncMirageAvoidanceStatePacket packet) {
            var level = Minecraft.getInstance().level;
            if (level == null || !(level.getEntity(packet.entityId) instanceof Player player)) {
                return;
            }

            player.getCapability(Capabilities.SPELL_DATA).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, state -> {
                        state.startGameTime = packet.startGameTime;
                        state.activeUntilGameTime = packet.activeUntilGameTime;
                        state.invulnerableUntilGameTime = packet.invulnerableUntilGameTime;
                        state.movementForward = packet.movementForward;
                        state.movementStrafe = packet.movementStrafe;
                    })
            );
        }
    }
}
