package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncScarletThirstHealthPacket implements CustomPacketPayload {
    public static final Type<SyncScarletThirstHealthPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_scarlet_thirst_health"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncScarletThirstHealthPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncScarletThirstHealthPacket::decode);

    private final float health;

    public SyncScarletThirstHealthPacket(float health) {
        this.health = health;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncScarletThirstHealthPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.health);
    }

    private static SyncScarletThirstHealthPacket decode(FriendlyByteBuf buffer) {
        return new SyncScarletThirstHealthPacket(buffer.readFloat());
    }

    public static void handle(SyncScarletThirstHealthPacket packet, IPayloadContext context) {
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

        private static void handle(SyncScarletThirstHealthPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.setHealth(packet.health);
        }
    }
}
