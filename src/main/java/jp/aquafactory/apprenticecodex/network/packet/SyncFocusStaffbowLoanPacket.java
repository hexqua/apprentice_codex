package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientLoanState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncFocusStaffbowLoanPacket(float remainingLoanMana) implements CustomPacketPayload {
    public static final Type<SyncFocusStaffbowLoanPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_focus_staffbow_loan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFocusStaffbowLoanPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncFocusStaffbowLoanPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncFocusStaffbowLoanPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.remainingLoanMana);
    }

    public static SyncFocusStaffbowLoanPacket decode(FriendlyByteBuf buffer) {
        return new SyncFocusStaffbowLoanPacket(buffer.readFloat());
    }

    public static void handle(SyncFocusStaffbowLoanPacket packet, IPayloadContext context) {
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

        private static void handle(SyncFocusStaffbowLoanPacket packet) {
            if (Minecraft.getInstance().player == null) {
                return;
            }

            FocusStaffbowClientLoanState.applySyncedState(packet.remainingLoanMana);
        }
    }
}
