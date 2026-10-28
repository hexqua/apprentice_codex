package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.config.item.SpellboundArmsModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record SyncSpellboundArmsModuleConfigPacket(Values values) {
    public static void encode(SyncSpellboundArmsModuleConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.values.firstModuleMultiplier());
        buffer.writeDouble(packet.values.additionalModuleMultiplier());
        buffer.writeDouble(packet.values.normalManaCostMultiplier());
        buffer.writeDouble(packet.values.magicManaCostMultiplier());
    }

    public static SyncSpellboundArmsModuleConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellboundArmsModuleConfigPacket(new Values(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncSpellboundArmsModuleConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncSpellboundArmsModuleConfigPacket packet) {
            SpellboundArmsModuleConfigState.set(packet.values);
        }
    }
}
