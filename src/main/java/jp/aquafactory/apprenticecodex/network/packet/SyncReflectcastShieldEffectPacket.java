package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldClientEffectState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncReflectcastShieldEffectPacket(InteractionHand hand, String spellId) implements CustomPacketPayload {
    public static final Type<SyncReflectcastShieldEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_reflectcast_shield_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncReflectcastShieldEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncReflectcastShieldEffectPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncReflectcastShieldEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.spellId);
    }

    public static SyncReflectcastShieldEffectPacket decode(FriendlyByteBuf buffer) {
        return new SyncReflectcastShieldEffectPacket(buffer.readEnum(InteractionHand.class), buffer.readUtf());
    }

    public static void handle(
            SyncReflectcastShieldEffectPacket packet,
            IPayloadContext context
    ) {
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

        private static void handle(SyncReflectcastShieldEffectPacket packet) {
            ReflectcastShieldClientEffectState.beginLocalSuccessFlash(packet.hand(), packet.spellId());
        }
    }
}
