package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharmConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SyncManaShieldCharmConfigPacket implements CustomPacketPayload {
    public static final Type<SyncManaShieldCharmConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_shield_charm_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaShieldCharmConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncManaShieldCharmConfigPacket::decode);
    private final float manaPerDamage;
    private final int recoveryThresholdMana;

    public SyncManaShieldCharmConfigPacket(float manaPerDamage, int recoveryThresholdMana) {
        this.manaPerDamage = manaPerDamage;
        this.recoveryThresholdMana = recoveryThresholdMana;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncManaShieldCharmConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.manaPerDamage);
        buffer.writeVarInt(packet.recoveryThresholdMana);
    }

    private static SyncManaShieldCharmConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaShieldCharmConfigPacket(
                buffer.readFloat(),
                buffer.readVarInt()
        );
    }

    public static void handle(SyncManaShieldCharmConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncManaShieldCharmConfigPacket packet) {
            ManaShieldCharmConfigState.set(packet.manaPerDamage, packet.recoveryThresholdMana);
        }
    }
}
