package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SyncManaForceBladeConfigPacket implements CustomPacketPayload {
    public static final Type<SyncManaForceBladeConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_force_blade_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaForceBladeConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncManaForceBladeConfigPacket::decode);

    private final float imbueDamageMultiplierScale;
    private final float attackManaCostMultiplier;
    private final float attackManaSchoolMultiplierScale;

    public SyncManaForceBladeConfigPacket(
            float imbueDamageMultiplierScale,
            float attackManaCostMultiplier,
            float attackManaSchoolMultiplierScale
    ) {
        this.imbueDamageMultiplierScale = imbueDamageMultiplierScale;
        this.attackManaCostMultiplier = attackManaCostMultiplier;
        this.attackManaSchoolMultiplierScale = attackManaSchoolMultiplierScale;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncManaForceBladeConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.imbueDamageMultiplierScale);
        buffer.writeFloat(packet.attackManaCostMultiplier);
        buffer.writeFloat(packet.attackManaSchoolMultiplierScale);
    }

    private static SyncManaForceBladeConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaForceBladeConfigPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncManaForceBladeConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncManaForceBladeConfigPacket packet) {
            ManaForceBladeConfigState.set(
                    packet.imbueDamageMultiplierScale,
                    packet.attackManaCostMultiplier,
                    packet.attackManaSchoolMultiplierScale
            );
        }
    }
}
