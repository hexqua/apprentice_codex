package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsEffects;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsEffect.Kind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PsychicDriveGauntletsEffectPacket(UUID player, PsychicDriveGauntletsEffect effect)
        implements CustomPacketPayload {
    public static final Type<PsychicDriveGauntletsEffectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "psychic_drive_gauntlets_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PsychicDriveGauntletsEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                var effect = packet.effect;
                buffer.writeUUID(packet.player);
                buffer.writeBoolean(effect.right());
                buffer.writeEnum(effect.kind());
                writeVector(buffer, effect.position());
                writeVector(buffer, effect.direction());
                buffer.writeFloat(effect.charge());
            }, buffer -> new PsychicDriveGauntletsEffectPacket(buffer.readUUID(),
                    new PsychicDriveGauntletsEffect(buffer.readBoolean(), buffer.readEnum(Kind.class),
                            readVector(buffer), readVector(buffer), buffer.readFloat())));

    private static void writeVector(RegistryFriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVector(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PsychicDriveGauntletsEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.accept(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(PsychicDriveGauntletsEffectPacket packet) {
            PsychicDriveGauntletsEffects.accept(packet.player, packet.effect);
        }
    }
}
