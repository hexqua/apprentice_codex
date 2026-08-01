package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSpellgunConfigPacket(SpellgunConfigState.Values values) {
    public static void encode(SyncSpellgunConfigPacket packet, FriendlyByteBuf buffer) {
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

    public static SyncSpellgunConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellgunConfigPacket(new SpellgunConfigState.Values(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean()
        ));
    }

    public static void handle(
            SyncSpellgunConfigPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
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

        private static void handle(SyncSpellgunConfigPacket packet) {
            SpellgunConfigState.setValues(packet.values);
        }
    }
}
