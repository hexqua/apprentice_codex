package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncMagiEngineersStaffCastPacket(int entityId, ResourceLocation spellId, ItemStack stack,
                                              InteractionHand hand, Phase phase, int chargeTicks)
        implements CustomPacketPayload {
    public enum Phase { START, END, CANCEL }

    public static final Type<SyncMagiEngineersStaffCastPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_magi_engineers_staff_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMagiEngineersStaffCastPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.entityId);
                buffer.writeResourceLocation(packet.spellId);
                ItemStack.STREAM_CODEC.encode(buffer, packet.stack);
                buffer.writeEnum(packet.hand);
                buffer.writeEnum(packet.phase);
                buffer.writeVarInt(packet.chargeTicks);
            }, buffer -> new SyncMagiEngineersStaffCastPacket(buffer.readVarInt(), buffer.readResourceLocation(),
                    ItemStack.STREAM_CODEC.decode(buffer), buffer.readEnum(InteractionHand.class),
                    buffer.readEnum(Phase.class), buffer.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncMagiEngineersStaffCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!MagiEngineersStaff.available()) return;
            var entity = context.player().level().getEntity(packet.entityId);
            if (entity instanceof LivingEntity caster) MagiEngineersStaffCompat.receive(caster, packet);
        });
    }
}
