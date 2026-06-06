package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientSwingMagicAttackPacket(boolean bypassChargeCheck, InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ClientSwingMagicAttackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_swing_magic_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientSwingMagicAttackPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientSwingMagicAttackPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientSwingMagicAttackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.bypassChargeCheck());
        buffer.writeEnum(packet.hand());
    }

    public static ClientSwingMagicAttackPacket decode(FriendlyByteBuf buffer) {
        return new ClientSwingMagicAttackPacket(buffer.readBoolean(), buffer.readEnum(InteractionHand.class));
    }

    public static void handle(ClientSwingMagicAttackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            var stack = sender.getItemInHand(packet.hand());
            if (stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem) {
                swingTriggeredMagicItem.tryTriggerSpellOnSwing(
                        sender,
                        packet.hand(),
                        packet.bypassChargeCheck()
                );
            }
        });
    }
}
