package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncChargecastCatalystbookConfigPacket;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ChargecastCatalystbookConfigSyncEvents {
    private ChargecastCatalystbookConfigSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Iron's 側がディメンション移動時に管理詠唱を終了しない場合に備え、保存済みの対象座標を別レベルへ持ち越さない。
            if (ChargecastCatalystbook.isManagedCast(player, null)) {
                Utils.serverSideCancelCast(player);
            }
            syncToPlayer(player);
        }
    }

    private static void syncToPlayer(ServerPlayer player) {
        Networks.sendToPlayer(player, createPacket());
    }

    private static SyncChargecastCatalystbookConfigPacket createPacket() {
        return new SyncChargecastCatalystbookConfigPacket(
                ApprenticeCodexServerConfig.chargecastCatalystbookConfig()
        );
    }

    @Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onConfigLoading(ModConfigEvent.Loading event) {
            syncIfServerConfig(event);
        }

        @SubscribeEvent
        public static void onConfigReloading(ModConfigEvent.Reloading event) {
            syncIfServerConfig(event);
        }

        private static void syncIfServerConfig(ModConfigEvent event) {
            if (event.getConfig().getType() != ModConfig.Type.SERVER
                    || !ApprenticeCodex.MODID.equals(event.getConfig().getModId())) {
                return;
            }
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            var packet = createPacket();
            for (var player : server.getPlayerList().getPlayers()) {
                Networks.sendToPlayer(player, packet);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ChargecastCatalystbookClientConfigState.reset();
            ChargecastCatalystbookClientCastIntent.clear();
        }
    }
}
