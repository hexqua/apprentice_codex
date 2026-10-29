package jp.aquafactory.apprenticecodex.network.packet;

import java.util.UUID;
import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsEffects;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect.Kind;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record PsychicDriveGauntletsEffectPacket(UUID player, PsychicDriveGauntletsEffect effect) {
    public static void encode(PsychicDriveGauntletsEffectPacket packet, FriendlyByteBuf buffer) {
        var effect = packet.effect;
        buffer.writeUUID(packet.player);
        buffer.writeBoolean(effect.right());
        buffer.writeEnum(effect.kind());
        writeVector(buffer, effect.position());
        writeVector(buffer, effect.direction());
        buffer.writeFloat(effect.charge());
    }

    public static PsychicDriveGauntletsEffectPacket decode(FriendlyByteBuf buffer) {
        return new PsychicDriveGauntletsEffectPacket(buffer.readUUID(),
                new PsychicDriveGauntletsEffect(buffer.readBoolean(), buffer.readEnum(Kind.class),
                        readVector(buffer), readVector(buffer), buffer.readFloat()));
    }

    private static void writeVector(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(PsychicDriveGauntletsEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.accept(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(PsychicDriveGauntletsEffectPacket packet) {
            PsychicDriveGauntletsEffects.accept(packet.player, packet.effect);
        }
    }
}
