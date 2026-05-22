package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowConfigPacket;
import net.minecraft.core.registries.BuiltInRegistries;
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
public final class FocusStaffbowConfigSyncEvents {
    private FocusStaffbowConfigSyncEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FocusStaffbowConfigSyncEvents::onConfigLoading);
        modEventBus.addListener(FocusStaffbowConfigSyncEvents::onConfigReloading);
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

    private static SyncFocusStaffbowConfigPacket createPacket() {
        return new SyncFocusStaffbowConfigPacket(
                ApprenticeCodexServerConfig.focusStaffbowEnableContinuousFocusedCast(),
                ApprenticeCodexServerConfig.focusStaffbowEnableArrowCatalystRequirement(),
                ApprenticeCodexServerConfig.focusStaffbowArrowCatalystItemIds(),
                ApprenticeCodexServerConfig.focusStaffbowChargeSettings()
        );
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        syncIfServerConfig(event);
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        syncIfServerConfig(event);
    }

    private static void syncIfServerConfig(ModConfigEvent event) {
        if (event.getConfig().getType() != ModConfig.Type.SERVER) {
            return;
        }
        if (!ApprenticeCodex.MODID.equals(event.getConfig().getModId())) {
            return;
        }

        syncToAllPlayers();
        logArrowCatalystConfigWarnings();
    }

    private static void logArrowCatalystConfigWarnings() {
        if (!ApprenticeCodexServerConfig.focusStaffbowEnableArrowCatalystRequirement()) {
            return;
        }

        var itemIds = ApprenticeCodexServerConfig.focusStaffbowArrowCatalystItemIds();
        if (itemIds.isEmpty()) {
            ApprenticeCodex.LOGGER.warn(
                    "Focus Staffbow arrowCatalystItems is empty while arrow catalyst requirement is enabled. Non-Synthesis survival casts will be unusable."
            );
            return;
        }

        for (var itemId : itemIds) {
            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                ApprenticeCodex.LOGGER.warn("Focus Staffbow arrowCatalystItems contains an unknown item id: {}", itemId);
            }
        }
    }

    @EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            FocusStaffbowClientConfigState.reset();
        }
    }
}
