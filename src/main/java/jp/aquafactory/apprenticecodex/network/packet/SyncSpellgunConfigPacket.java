package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunConfigState;
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

public final class SyncSpellgunConfigPacket implements CustomPacketPayload {
    public static final Type<SyncSpellgunConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_spellgun_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSpellgunConfigPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer),
            SyncSpellgunConfigPacket::decode
    );

    private final SpellgunConfigState.Values values;

    public SyncSpellgunConfigPacket(SpellgunConfigState.Values values) {
        this.values = values;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncSpellgunConfigPacket packet, FriendlyByteBuf buffer) {
        var values = packet.values;
        buffer.writeVarInt(values.ironMaxInstantImbueCooldownTicks());
        buffer.writeVarInt(values.ironOverriddenSpellCooldownTicks());
        buffer.writeVarInt(values.copperMaxInstantImbueCooldownTicks());
        buffer.writeVarInt(values.copperOverriddenSpellCooldownTicks());
        buffer.writeVarInt(values.goldReducedCooldownMinimumTicks());
        buffer.writeVarInt(values.goldCooldownReductionTicks());
        buffer.writeBoolean(values.ironIgnoreMaxMana());
        buffer.writeBoolean(values.copperIgnoreMaxMana());
        buffer.writeBoolean(values.goldIgnoreMaxMana());
        buffer.writeBoolean(values.diamondIgnoreMaxMana());
    }

    private static SyncSpellgunConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellgunConfigPacket(new SpellgunConfigState.Values(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean()
        ));
    }

    public static void handle(SyncSpellgunConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncSpellgunConfigPacket packet) {
            SpellgunConfigState.setValues(packet.values);
        }
    }
}
