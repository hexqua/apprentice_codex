package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.swingstaff.HighTierSwingcastStaffConfigState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncHighTierSwingcastStaffConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class HighTierSwingcastStaffConfigSyncEvents {
    private HighTierSwingcastStaffConfigSyncEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(HighTierSwingcastStaffConfigSyncEvents::onConfigLoading);
        modEventBus.addListener(HighTierSwingcastStaffConfigSyncEvents::onConfigReloading);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToPlayer(serverPlayer);
        }
    }

    private static void syncToPlayer(ServerPlayer player) {
        Networks.sendToPlayer(player, createPacket());
    }

    private static void syncToAllPlayers() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        var packet = createPacket();
        for (var player : server.getPlayerList().getPlayers()) {
            Networks.sendToPlayer(player, packet);
        }
    }

    private static SyncHighTierSwingcastStaffConfigPacket createPacket() {
        return new SyncHighTierSwingcastStaffConfigPacket(
                ApprenticeCodexServerConfig.diamondSwingcastStaffCooldownReductionTicks(),
                ApprenticeCodexServerConfig.netheriteSwingcastStaffCooldownReductionTicks()
        );
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        syncIfServerConfig(event);
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        syncIfServerConfig(event);
    }

    private static void syncIfServerConfig(ModConfigEvent event) {
        if (event.getConfig().getType() != ModConfig.Type.SERVER
                || !ApprenticeCodex.MODID.equals(event.getConfig().getModId())) {
            return;
        }
        syncToAllPlayers();
    }

    @EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            HighTierSwingcastStaffConfigState.reset();
        }
    }
}
