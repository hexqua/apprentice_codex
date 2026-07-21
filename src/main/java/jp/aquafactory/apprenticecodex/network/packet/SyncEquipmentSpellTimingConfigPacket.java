package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.EquipmentSpellTimingConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncEquipmentSpellTimingConfigPacket {
    private final double craftsmansDelightCooldownMultiplier;
    private final double magiAgentSuitBootsCooldownMultiplier;
    private final double magiAgentSuitBootsCastTimeMultiplier;

    public SyncEquipmentSpellTimingConfigPacket(
            double craftsmansDelightCooldownMultiplier,
            double magiAgentSuitBootsCooldownMultiplier,
            double magiAgentSuitBootsCastTimeMultiplier
    ) {
        this.craftsmansDelightCooldownMultiplier = craftsmansDelightCooldownMultiplier;
        this.magiAgentSuitBootsCooldownMultiplier = magiAgentSuitBootsCooldownMultiplier;
        this.magiAgentSuitBootsCastTimeMultiplier = magiAgentSuitBootsCastTimeMultiplier;
    }

    public static void encode(SyncEquipmentSpellTimingConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.craftsmansDelightCooldownMultiplier);
        buffer.writeDouble(packet.magiAgentSuitBootsCooldownMultiplier);
        buffer.writeDouble(packet.magiAgentSuitBootsCastTimeMultiplier);
    }

    public static SyncEquipmentSpellTimingConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncEquipmentSpellTimingConfigPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(
            SyncEquipmentSpellTimingConfigPacket packet,
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

        private static void handle(SyncEquipmentSpellTimingConfigPacket packet) {
            EquipmentSpellTimingConfigState.set(
                    packet.craftsmansDelightCooldownMultiplier,
                    packet.magiAgentSuitBootsCooldownMultiplier,
                    packet.magiAgentSuitBootsCastTimeMultiplier
            );
        }
    }
}
