package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientSwingMagicAttackPacket(boolean bypassChargeCheck) implements CustomPacketPayload {
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
    }

    public static ClientSwingMagicAttackPacket decode(FriendlyByteBuf buffer) {
        return new ClientSwingMagicAttackPacket(buffer.readBoolean());
    }

    public static void handle(ClientSwingMagicAttackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            var mainHandItem = sender.getMainHandItem().getItem();
            if (mainHandItem instanceof AbstractSwingMagicItem swingMagicItem) {
                swingMagicItem.tryTriggerImbuedSpellOnSwing(sender, packet.bypassChargeCheck());
            }
        });
    }
}
