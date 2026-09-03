package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncManaManeuverGearJumpPacket(Vec3 impulse) implements CustomPacketPayload {
    public static final Type<SyncManaManeuverGearJumpPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_maneuver_gear_jump")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaManeuverGearJumpPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncManaManeuverGearJumpPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncManaManeuverGearJumpPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.impulse.x);
        buffer.writeDouble(packet.impulse.y);
        buffer.writeDouble(packet.impulse.z);
    }

    public static SyncManaManeuverGearJumpPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaManeuverGearJumpPacket(new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        ));
    }

    public static void handle(SyncManaManeuverGearJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ManaManeuverGearMovement.applyWallJump(context.player(), packet.impulse));
    }
}
