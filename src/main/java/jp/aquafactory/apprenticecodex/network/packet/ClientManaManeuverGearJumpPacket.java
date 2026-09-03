package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientManaManeuverGearJumpPacket() implements CustomPacketPayload {
    public static final Type<ClientManaManeuverGearJumpPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_mana_maneuver_gear_jump")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientManaManeuverGearJumpPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientManaManeuverGearJumpPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientManaManeuverGearJumpPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientManaManeuverGearJumpPacket decode(FriendlyByteBuf buffer) {
        return new ClientManaManeuverGearJumpPacket();
    }

    public static void handle(ClientManaManeuverGearJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender && !sender.isSpectator()) {
                ManaManeuverGearManager.tryWallJump(sender);
            }
        });
    }
}
