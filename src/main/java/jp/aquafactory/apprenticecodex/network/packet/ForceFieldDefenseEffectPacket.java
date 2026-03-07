package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEffectRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ForceFieldDefenseEffectPacket implements CustomPacketPayload {
    public static final Type<ForceFieldDefenseEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "force_field_defense_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceFieldDefenseEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ForceFieldDefenseEffectPacket::decode);

    private static final float DEFAULT_SIZE_SCALE = 1.0f;
    private static final float DEFAULT_LIFETIME_SCALE = 1.0f;

    private final double x;
    private final double y;
    private final double z;
    private final float normalX;
    private final float normalY;
    private final float normalZ;
    private final float sizeScale;
    private final float lifetimeScale;
    private final boolean renderWave;
    private final boolean failed;
    private final boolean absorb;

    public ForceFieldDefenseEffectPacket(Vec3 position, Vec3 normal) {
        this(position, normal, DEFAULT_SIZE_SCALE, DEFAULT_LIFETIME_SCALE, true);
    }

    public ForceFieldDefenseEffectPacket(Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale) {
        this(position, normal, sizeScale, lifetimeScale, true, false);
    }

    public ForceFieldDefenseEffectPacket(Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale, boolean renderWave) {
        this(position, normal, sizeScale, lifetimeScale, renderWave, false);
    }

    public ForceFieldDefenseEffectPacket(Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale, boolean renderWave, boolean failed) {
        this(position, normal, sizeScale, lifetimeScale, renderWave, failed, false);
    }

    public ForceFieldDefenseEffectPacket(Vec3 position, Vec3 normal, float sizeScale, float lifetimeScale, boolean renderWave, boolean failed, boolean absorb) {
        this(position.x, position.y, position.z, (float) normal.x, (float) normal.y, (float) normal.z, sizeScale, lifetimeScale, renderWave, failed, absorb);
    }

    private ForceFieldDefenseEffectPacket(double x, double y, double z, float normalX, float normalY, float normalZ,
                                          float sizeScale, float lifetimeScale, boolean renderWave, boolean failed, boolean absorb) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.sizeScale = sizeScale;
        this.lifetimeScale = lifetimeScale;
        this.renderWave = renderWave;
        this.failed = failed;
        this.absorb = absorb;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ForceFieldDefenseEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.normalX);
        buffer.writeFloat(packet.normalY);
        buffer.writeFloat(packet.normalZ);
        buffer.writeFloat(packet.sizeScale);
        buffer.writeFloat(packet.lifetimeScale);
        buffer.writeBoolean(packet.renderWave);
        buffer.writeBoolean(packet.failed);
        buffer.writeBoolean(packet.absorb);
    }

    private static ForceFieldDefenseEffectPacket decode(FriendlyByteBuf buffer) {
        return new ForceFieldDefenseEffectPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(ForceFieldDefenseEffectPacket packet, IPayloadContext context) {
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

        private static void handle(ForceFieldDefenseEffectPacket packet) {
            ForceFieldDefenseEffectRenderEvent.enqueueEffect(
                    new Vec3(packet.x, packet.y, packet.z),
                    new Vec3(packet.normalX, packet.normalY, packet.normalZ),
                    packet.sizeScale,
                    packet.lifetimeScale,
                    packet.renderWave,
                    packet.failed,
                    packet.absorb
            );
        }
    }
}
