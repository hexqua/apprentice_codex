package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaffConfigState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncSoulstainedSteelSwingcastStaffConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SoulstainedSteelSwingcastStaffConfigSyncEvents {
    private SoulstainedSteelSwingcastStaffConfigSyncEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SoulstainedSteelSwingcastStaffConfigSyncEvents::onConfigLoading);
        modEventBus.addListener(SoulstainedSteelSwingcastStaffConfigSyncEvents::onConfigReloading);
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

    private static SyncSoulstainedSteelSwingcastStaffConfigPacket createPacket() {
        return new SyncSoulstainedSteelSwingcastStaffConfigPacket(
                ApprenticeCodexServerConfig.soulstainedSteelSwingcastStaffManaCostPerBlade()
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

    @Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            SoulstainedSteelSwingcastStaffConfigState.reset();
        }
    }
}
