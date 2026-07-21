package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.EquipmentSpellTimingConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SyncEquipmentSpellTimingConfigPacket implements CustomPacketPayload {
    public static final Type<SyncEquipmentSpellTimingConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    "sync_equipment_spell_timing_config"
            ));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEquipmentSpellTimingConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncEquipmentSpellTimingConfigPacket::decode);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncEquipmentSpellTimingConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.craftsmansDelightCooldownMultiplier);
        buffer.writeDouble(packet.magiAgentSuitBootsCooldownMultiplier);
        buffer.writeDouble(packet.magiAgentSuitBootsCastTimeMultiplier);
    }

    private static SyncEquipmentSpellTimingConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncEquipmentSpellTimingConfigPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    public static void handle(SyncEquipmentSpellTimingConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncEquipmentSpellTimingConfigPacket packet) {
            EquipmentSpellTimingConfigState.set(
                    packet.craftsmansDelightCooldownMultiplier,
                    packet.magiAgentSuitBootsCooldownMultiplier,
                    packet.magiAgentSuitBootsCastTimeMultiplier
            );
        }
    }
}
