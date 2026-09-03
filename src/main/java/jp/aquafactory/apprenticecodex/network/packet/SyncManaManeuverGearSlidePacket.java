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

public record SyncManaManeuverGearSlidePacket(double ySpeed) implements CustomPacketPayload {
    public static final Type<SyncManaManeuverGearSlidePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_maneuver_gear_slide")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaManeuverGearSlidePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncManaManeuverGearSlidePacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncManaManeuverGearSlidePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.ySpeed);
    }

    public static SyncManaManeuverGearSlidePacket decode(FriendlyByteBuf buffer) {
        return new SyncManaManeuverGearSlidePacket(buffer.readDouble());
    }

    public static void handle(SyncManaManeuverGearSlidePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> applyTo(context.player(), packet.ySpeed));
    }

    public static void applyTo(LivingEntity entity, double ySpeed) {
        var currentVelocity = entity.getDeltaMovement();
        entity.setDeltaMovement(currentVelocity.x, ySpeed, currentVelocity.z);
        entity.hasImpulse = true;
        entity.fallDistance = 0.0F;
    }
}
