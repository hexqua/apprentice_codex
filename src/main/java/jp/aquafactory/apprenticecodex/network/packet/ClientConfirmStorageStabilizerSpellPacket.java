package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.StorageStabilizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientConfirmStorageStabilizerSpellPacket(
        InteractionHand hand,
        int selectedIndex
) implements CustomPacketPayload {
    public static final Type<ClientConfirmStorageStabilizerSpellPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_confirm_storage_stabilizer_spell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfirmStorageStabilizerSpellPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientConfirmStorageStabilizerSpellPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientConfirmStorageStabilizerSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeVarInt(packet.selectedIndex());
    }

    public static ClientConfirmStorageStabilizerSpellPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmStorageStabilizerSpellPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readVarInt()
        );
    }

    public static void handle(ClientConfirmStorageStabilizerSpellPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            var magicData = MagicData.getPlayerMagicData(sender);
            if (magicData != null && magicData.isCasting()) {
                return;
            }

            var stack = sender.getItemInHand(packet.hand());
            if (!(stack.getItem() instanceof StorageStabilizer)
                    || !StorageStabilizer.isSelectableSpellIndex(packet.selectedIndex())) {
                return;
            }

            var previousIndex = StorageStabilizer.getSelectedSpellIndex(stack);
            StorageStabilizer.setSelectedSpellIndex(stack, packet.selectedIndex());
            if (previousIndex != packet.selectedIndex()) {
                sender.level().playSound(
                        null,
                        sender.blockPosition(),
                        SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.PLAYERS,
                        0.35F,
                        1.1F
                );
            }
        });
    }
}
