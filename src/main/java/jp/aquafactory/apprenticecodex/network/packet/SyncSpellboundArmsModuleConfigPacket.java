package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleConfigState;
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

public record SyncSpellboundArmsModuleConfigPacket(Values values) implements CustomPacketPayload {
    public static final Type<SyncSpellboundArmsModuleConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_spellbound_arms_module_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSpellboundArmsModuleConfigPacket> STREAM_CODEC =
            StreamCodec.of(SyncSpellboundArmsModuleConfigPacket::encode, SyncSpellboundArmsModuleConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, SyncSpellboundArmsModuleConfigPacket packet) {
        buffer.writeDouble(packet.values.firstModuleMultiplier());
        buffer.writeDouble(packet.values.additionalModuleMultiplier());
        buffer.writeDouble(packet.values.normalManaCostMultiplier());
        buffer.writeDouble(packet.values.magicManaCostMultiplier());
    }

    private static SyncSpellboundArmsModuleConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellboundArmsModuleConfigPacket(new Values(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncSpellboundArmsModuleConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncSpellboundArmsModuleConfigPacket packet) {
            SpellboundArmsModuleConfigState.set(packet.values);
        }
    }
}
