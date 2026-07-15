package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientSpellgunCastPacket(BlockTargetData targetData) implements CustomPacketPayload {
    public static final Type<ClientSpellgunCastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_spellgun_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientSpellgunCastPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientSpellgunCastPacket::decode);

    public ClientSpellgunCastPacket {
        if (targetData == null) {
            targetData = new BlockTargetData();
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientSpellgunCastPacket packet, FriendlyByteBuf buffer) {
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientSpellgunCastPacket decode(FriendlyByteBuf buffer) {
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientSpellgunCastPacket(targetData);
    }

    public static void handle(ClientSpellgunCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            if (sender.getMainHandItem().getItem() instanceof AbstractSpellGunItem spellgun) {
                spellgun.tryTriggerImbuedSpell(sender, InteractionHand.MAIN_HAND, packet.targetData());
            }
        });
    }
}
