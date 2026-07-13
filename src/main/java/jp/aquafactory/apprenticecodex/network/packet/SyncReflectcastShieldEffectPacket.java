package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldClientEffectState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncReflectcastShieldEffectPacket {
    private final InteractionHand hand;
    private final String spellId;

    public SyncReflectcastShieldEffectPacket(InteractionHand hand, String spellId) {
        this.hand = hand;
        this.spellId = spellId;
    }

    public static void encode(SyncReflectcastShieldEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.spellId);
    }

    public static SyncReflectcastShieldEffectPacket decode(FriendlyByteBuf buffer) {
        return new SyncReflectcastShieldEffectPacket(buffer.readEnum(InteractionHand.class), buffer.readUtf());
    }

    public static void handle(
            SyncReflectcastShieldEffectPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
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

        private static void handle(SyncReflectcastShieldEffectPacket packet) {
            ReflectcastShieldClientEffectState.beginLocalSuccessFlash(packet.hand, packet.spellId);
        }
    }
}
