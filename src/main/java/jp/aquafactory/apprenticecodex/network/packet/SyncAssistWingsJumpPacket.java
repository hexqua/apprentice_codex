package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncAssistWingsJumpPacket(float jumpHeight) implements CustomPacketPayload {
    public static final Type<SyncAssistWingsJumpPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_assist_wings_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAssistWingsJumpPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncAssistWingsJumpPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncAssistWingsJumpPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.jumpHeight);
    }

    public static SyncAssistWingsJumpPacket decode(FriendlyByteBuf buffer) {
        return new SyncAssistWingsJumpPacket(buffer.readFloat());
    }

    public static void handle(SyncAssistWingsJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> applyTo(context.player(), packet.jumpHeight));
    }

    public static void applyTo(LivingEntity entity, float jumpHeight) {
        var currentDelta = entity.getDeltaMovement();
        entity.setDeltaMovement(currentDelta.x, jumpHeight, currentDelta.z);
        entity.hasImpulse = true;
        entity.fallDistance = 0;
    }
}
