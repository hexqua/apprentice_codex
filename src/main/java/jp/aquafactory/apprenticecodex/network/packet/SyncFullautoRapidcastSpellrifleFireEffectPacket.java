package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.event.client.FullautoRapidcastSpellrifleClientFireEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncFullautoRapidcastSpellrifleFireEffectPacket(int shooterEntityId) {

    public static void encode(SyncFullautoRapidcastSpellrifleFireEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.shooterEntityId());
    }

    public static SyncFullautoRapidcastSpellrifleFireEffectPacket decode(FriendlyByteBuf buffer) {
        return new SyncFullautoRapidcastSpellrifleFireEffectPacket(buffer.readVarInt());
    }

    public static void handle(SyncFullautoRapidcastSpellrifleFireEffectPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncFullautoRapidcastSpellrifleFireEffectPacket packet) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }

            var shooter = minecraft.level.getEntity(packet.shooterEntityId());
            if (shooter == minecraft.player) {
                FullautoRapidcastSpellrifleClientFireEffectState.beginRecoil();
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
