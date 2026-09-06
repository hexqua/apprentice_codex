package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScytheClientConfigState;
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

public record SyncSpellReaperScytheConfigPacket(
        SpellReaperScytheServerConfig.Values values
) implements CustomPacketPayload {
    public static final Type<SyncSpellReaperScytheConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_spell_reaper_scythe_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSpellReaperScytheConfigPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> encode(packet, buffer),
                    SyncSpellReaperScytheConfigPacket::decode
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncSpellReaperScytheConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.ascensionBaseManaCost());
        buffer.writeVarInt(packet.values.ascensionManaCostReductionPerLevel());
        buffer.writeVarInt(packet.values.ascensionCooldownTicks());
        buffer.writeVarInt(packet.values.throwManaCost());
        buffer.writeVarInt(packet.values.throwManaPerTick());
        buffer.writeVarInt(packet.values.reboundBaseManaCost());
        buffer.writeVarInt(packet.values.reboundManaCostReductionPerLevel());
        buffer.writeVarInt(packet.values.maelstromBaseManaCost());
        buffer.writeVarInt(packet.values.maelstromManaCostReductionPerLevel());
    }

    public static SyncSpellReaperScytheConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellReaperScytheConfigPacket(new SpellReaperScytheServerConfig.Values(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()
        ));
    }

    public static void handle(SyncSpellReaperScytheConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncSpellReaperScytheConfigPacket packet) {
            SpellReaperScytheClientConfigState.set(packet.values);
        }
    }
}
