package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.event.client.MultipurposeStaffrifleClientFireEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncMultipurposeStaffrifleFireEffectPacket(int shooterEntityId) implements CustomPacketPayload {
    public static final Type<SyncMultipurposeStaffrifleFireEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_multipurpose_staffrifle_fire_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMultipurposeStaffrifleFireEffectPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncMultipurposeStaffrifleFireEffectPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncMultipurposeStaffrifleFireEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.shooterEntityId());
    }

    public static SyncMultipurposeStaffrifleFireEffectPacket decode(FriendlyByteBuf buffer) {
        return new SyncMultipurposeStaffrifleFireEffectPacket(buffer.readVarInt());
    }

    public static void handle(SyncMultipurposeStaffrifleFireEffectPacket packet, IPayloadContext context) {
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

        private static void handle(SyncMultipurposeStaffrifleFireEffectPacket packet) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }

            var shooter = minecraft.level.getEntity(packet.shooterEntityId());
            if (shooter == minecraft.player) {
                MultipurposeStaffrifleClientFireEffectState.beginRecoil();
            }
            if (ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)
                    && shouldPlayBetterCombatShootAnimation(minecraft, shooter)) {
                BetterCombatClientCompat.playStaffrifleShootAnimation(shooter);
            }
        }

        private static boolean shouldPlayBetterCombatShootAnimation(Minecraft minecraft, Entity shooter) {
            return shooter != minecraft.player || !minecraft.options.getCameraType().isFirstPerson();
        }
    }
}
