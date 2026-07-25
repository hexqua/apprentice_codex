package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncLuminousDeviceConfigPacket(
        int maxStoredItems,
        int maxStoredMana,
        int upgradedMaxStoredMana,
        int cleanRadius,
        double mageLightExtendedRange
) implements CustomPacketPayload {
    public static final Type<SyncLuminousDeviceConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_luminous_device_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLuminousDeviceConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncLuminousDeviceConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public SyncLuminousDeviceConfigPacket(int maxStoredItems, int maxStoredMana, int cleanRadius) {
        this(
                maxStoredItems,
                maxStoredMana,
                jp.aquafactory.apprenticecodex.config.item.LuminousDeviceServerConfig.DEFAULT_UPGRADED_MAX_STORED_MANA,
                cleanRadius,
                jp.aquafactory.apprenticecodex.config.item.LuminousDeviceServerConfig.DEFAULT_MAGE_LIGHT_EXTENDED_RANGE
        );
    }

    public SyncLuminousDeviceConfigPacket(
            int maxStoredItems,
            int maxStoredMana,
            int cleanRadius,
            double mageLightExtendedRange
    ) {
        this(
                maxStoredItems,
                maxStoredMana,
                jp.aquafactory.apprenticecodex.config.item.LuminousDeviceServerConfig.DEFAULT_UPGRADED_MAX_STORED_MANA,
                cleanRadius,
                mageLightExtendedRange
        );
    }

    public static void encode(SyncLuminousDeviceConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.maxStoredItems);
        buffer.writeVarInt(packet.maxStoredMana);
        buffer.writeVarInt(packet.upgradedMaxStoredMana);
        buffer.writeVarInt(packet.cleanRadius);
        buffer.writeDouble(packet.mageLightExtendedRange);
    }

    public static SyncLuminousDeviceConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncLuminousDeviceConfigPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readDouble()
        );
    }

    public static void handle(SyncLuminousDeviceConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncLuminousDeviceConfigPacket packet) {
            LuminousDeviceConfigState.set(
                    packet.maxStoredItems,
                    packet.maxStoredMana,
                    packet.upgradedMaxStoredMana,
                    packet.cleanRadius,
                    packet.mageLightExtendedRange
            );
        }
    }
}
