package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonAction;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonMovement;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.MagiEngineersLegExoskeleton;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExoskeletonJumpEffectPacket(int entityId, ExoskeletonAction action) implements CustomPacketPayload {
    public static final Type<ExoskeletonJumpEffectPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "exoskeleton_jump_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExoskeletonJumpEffectPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVarInt(packet.entityId); buffer.writeEnum(packet.action); },
            buffer -> new ExoskeletonJumpEffectPacket(buffer.readVarInt(), buffer.readEnum(ExoskeletonAction.class)));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ExoskeletonJumpEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var self = context.player();
            if (MagiEngineersLegExoskeleton.available() && packet.entityId != self.getId()
                    && self.level().getEntity(packet.entityId) instanceof Player player) {
                // serverで成立済みの演出は、通信間隔の揺れでremote側のcooldownに拒否させない。
                if (packet.action.isStrider()) player.getCooldowns().removeCooldown(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get());
                ExoskeletonMovement.apply(player, packet.action);
            }
        });
    }
}
