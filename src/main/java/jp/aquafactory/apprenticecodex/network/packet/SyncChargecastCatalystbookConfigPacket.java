package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncChargecastCatalystbookConfigPacket(
        ChargecastCatalystbookServerConfig.Values values
) implements CustomPacketPayload {
    public static final Type<SyncChargecastCatalystbookConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_chargecast_catalystbook_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChargecastCatalystbookConfigPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> encode(packet, buffer),
                    SyncChargecastCatalystbookConfigPacket::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncChargecastCatalystbookConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.castTimeTicks());
        buffer.writeDouble(packet.values.spellPowerMultiplier());
        buffer.writeDouble(packet.values.silverRingCastTimeBonusFactor());
        buffer.writeVarInt(packet.values.spellDenylist().size());
        for (var spellId : packet.values.spellDenylist()) {
            buffer.writeResourceLocation(spellId);
        }
    }

    public static SyncChargecastCatalystbookConfigPacket decode(FriendlyByteBuf buffer) {
        var castTimeTicks = buffer.readVarInt();
        var spellPowerMultiplier = buffer.readDouble();
        var silverRingCastTimeBonusFactor = buffer.readDouble();
        var spellDenylistSize = buffer.readVarInt();
        var spellDenylist = new java.util.ArrayList<ResourceLocation>(spellDenylistSize);
        for (var index = 0; index < spellDenylistSize; ++index) {
            spellDenylist.add(buffer.readResourceLocation());
        }
        return new SyncChargecastCatalystbookConfigPacket(new ChargecastCatalystbookServerConfig.Values(
                castTimeTicks, spellPowerMultiplier, silverRingCastTimeBonusFactor, spellDenylist
        ));
    }

    public static void handle(SyncChargecastCatalystbookConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncChargecastCatalystbookConfigPacket packet) {
            ChargecastCatalystbookClientConfigState.set(packet.values);
        }
    }
}
