package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncMirageAvoidanceStatePacket implements CustomPacketPayload {
    public static final Type<SyncMirageAvoidanceStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mirage_avoidance_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMirageAvoidanceStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncMirageAvoidanceStatePacket::decode);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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

    public static void handle(SyncMirageAvoidanceStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncMirageAvoidanceStatePacket packet) {
            var level = Minecraft.getInstance().level;
            if (level == null || !(level.getEntity(packet.entityId) instanceof Player player)) {
                return;
            }

            Capabilities.getSpellData(player).ifPresent(data ->
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
