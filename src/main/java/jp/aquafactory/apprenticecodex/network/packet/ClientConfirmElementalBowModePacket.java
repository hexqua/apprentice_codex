package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ClientConfirmElementalBowModePacket(
        InteractionHand hand,
        String shotMode,
        @Nullable ResourceLocation selectionId,
        boolean continueUse
) implements CustomPacketPayload {
    public static final Type<ClientConfirmElementalBowModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_confirm_elemental_bow_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfirmElementalBowModePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientConfirmElementalBowModePacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientConfirmElementalBowModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeUtf(packet.shotMode());
        if ("magic".equals(packet.shotMode())) {
            buffer.writeVarInt(ElementalBow.scrollSlot(packet.selectionId()));
        } else {
            buffer.writeBoolean(packet.selectionId() != null);
            if (packet.selectionId() != null) buffer.writeResourceLocation(packet.selectionId());
        }
        buffer.writeBoolean(packet.continueUse());
    }

    public static ClientConfirmElementalBowModePacket decode(FriendlyByteBuf buffer) {
        var hand = buffer.readEnum(InteractionHand.class);
        var shotMode = buffer.readUtf();
        ResourceLocation selectionId;
        if ("magic".equals(shotMode)) selectionId = ElementalBow.selectionIdForSlot(buffer.readVarInt());
        else selectionId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        var continueUse = buffer.readBoolean();
        return new ClientConfirmElementalBowModePacket(hand, shotMode, selectionId, continueUse);
    }

    public static void handle(ClientConfirmElementalBowModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            ElementalBow.applyClientSelection(sender, packet.hand(), packet.shotMode(), packet.selectionId(), packet.continueUse());
        });
    }
}
