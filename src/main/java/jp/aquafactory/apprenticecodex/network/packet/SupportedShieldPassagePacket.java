package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedShieldPassage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SupportedShieldPassagePacket(ResourceLocation dimension, UUID shield, boolean allowed) {
    public static void encode(SupportedShieldPassagePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeUUID(packet.shield);
        buffer.writeBoolean(packet.allowed);
    }

    public static SupportedShieldPassagePacket decode(FriendlyByteBuf buffer) {
        return new SupportedShieldPassagePacket(buffer.readResourceLocation(), buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(SupportedShieldPassagePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet)));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SupportedShieldPassagePacket packet) {
            var level = Minecraft.getInstance().level;
            if (level != null && level.dimension().location().equals(packet.dimension)) {
                SupportedShieldPassage.receive(level, packet.shield, packet.allowed);
            }
        }
    }
}
