package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScytheClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncSpellReaperScytheConfigPacket(
        SpellReaperScytheServerConfig.Values values
) {
    public static void encode(SyncSpellReaperScytheConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.ascensionBaseManaCost());
        buffer.writeVarInt(packet.values.ascensionManaCostReductionPerLevel());
        buffer.writeVarInt(packet.values.ascensionCooldownTicks());
        buffer.writeVarInt(packet.values.throwManaCost());
        buffer.writeVarInt(packet.values.throwManaPerTick());
        buffer.writeVarInt(packet.values.reboundBaseManaCost());
        buffer.writeVarInt(packet.values.reboundManaCostReductionPerLevel());
    }

    public static SyncSpellReaperScytheConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellReaperScytheConfigPacket(new SpellReaperScytheServerConfig.Values(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt()
        ));
    }

    public static void handle(SyncSpellReaperScytheConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);
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
