package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public class SyncElementalBowOverheatPacket implements CustomPacketPayload {
    public static final Type<SyncElementalBowOverheatPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_elemental_bow_overheat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncElementalBowOverheatPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncElementalBowOverheatPacket::decode);

    private final CompoundTag data;

    public SyncElementalBowOverheatPacket(@Nullable CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncElementalBowOverheatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncElementalBowOverheatPacket decode(FriendlyByteBuf buffer) {
        return new SyncElementalBowOverheatPacket(buffer.readNbt());
    }

    public static void handle(SyncElementalBowOverheatPacket packet, IPayloadContext context) {
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

        private static void handle(SyncElementalBowOverheatPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            ElementalBowOverheatManager.applySyncedState(player, packet.data);
        }
    }
}
