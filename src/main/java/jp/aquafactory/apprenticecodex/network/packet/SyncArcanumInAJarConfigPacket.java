package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public final class SyncArcanumInAJarConfigPacket {
    private final @Nullable ResourceLocation materialItemId;
    private final @Nullable ResourceLocation productItemId;
    private final int processingTimeTicks;

    public SyncArcanumInAJarConfigPacket(
            @Nullable ResourceLocation materialItemId,
            @Nullable ResourceLocation productItemId,
            int processingTimeTicks
    ) {
        var valid = materialItemId != null && productItemId != null;
        this.materialItemId = valid ? materialItemId : null;
        this.productItemId = valid ? productItemId : null;
        this.processingTimeTicks = Math.max(1, processingTimeTicks);
    }

    public boolean isValid() {
        return materialItemId != null && productItemId != null;
    }

    public @Nullable ResourceLocation materialItemId() {
        return materialItemId;
    }

    public @Nullable ResourceLocation productItemId() {
        return productItemId;
    }

    public int processingTimeTicks() {
        return processingTimeTicks;
    }

    public static void encode(SyncArcanumInAJarConfigPacket packet, FriendlyByteBuf buffer) {
        var materialItemId = packet.materialItemId;
        var productItemId = packet.productItemId;
        var valid = materialItemId != null && productItemId != null;
        buffer.writeBoolean(valid);
        if (valid) {
            buffer.writeResourceLocation(materialItemId);
            buffer.writeResourceLocation(productItemId);
        }
        buffer.writeVarInt(packet.processingTimeTicks);
    }

    public static SyncArcanumInAJarConfigPacket decode(FriendlyByteBuf buffer) {
        var valid = buffer.readBoolean();
        var materialItemId = valid ? buffer.readResourceLocation() : null;
        var productItemId = valid ? buffer.readResourceLocation() : null;
        return new SyncArcanumInAJarConfigPacket(materialItemId, productItemId, buffer.readVarInt());
    }

    public static void handle(SyncArcanumInAJarConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncArcanumInAJarConfigPacket packet) {
            ArcanumInAJarConfigState.set(
                    packet.materialItemId,
                    packet.productItemId,
                    packet.processingTimeTicks
            );
        }
    }
}
