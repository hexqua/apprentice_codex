package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEffectRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ForceFieldDefenseEffectPacket {
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
        this(position.x, position.y, position.z, (float) normal.x, (float) normal.y, (float) normal.z, sizeScale, lifetimeScale, renderWave, failed);
    }

    private ForceFieldDefenseEffectPacket(double x, double y, double z, float normalX, float normalY, float normalZ,
                                          float sizeScale, float lifetimeScale, boolean renderWave, boolean failed) {
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
    }

    public static void encode(ForceFieldDefenseEffectPacket packet, FriendlyByteBuf buffer) {
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
    }

    public static ForceFieldDefenseEffectPacket decode(FriendlyByteBuf buffer) {
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
                buffer.readBoolean()
        );
    }

    public static void handle(ForceFieldDefenseEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
                    packet.failed
            );
        }
    }
}
