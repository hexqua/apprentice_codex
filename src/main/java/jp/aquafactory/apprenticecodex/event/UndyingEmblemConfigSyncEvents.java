package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncUndyingEmblemConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class UndyingEmblemConfigSyncEvents {
    private UndyingEmblemConfigSyncEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(UndyingEmblemConfigSyncEvents::onConfigLoading);
        modEventBus.addListener(UndyingEmblemConfigSyncEvents::onConfigReloading);
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
            syncToPlayer(player);
        }
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
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            var packet = createPacket();
            server.getPlayerList().getPlayers().forEach(player -> Networks.sendToPlayer(player, packet));
        }
    }

    private static void syncToPlayer(ServerPlayer player) {
        Networks.sendToPlayer(player, createPacket());
    }

    private static SyncUndyingEmblemConfigPacket createPacket() {
        return new SyncUndyingEmblemConfigPacket(
                ApprenticeCodexServerConfig.undyingEmblemReconstructionSpeedMultiplier());
    }
}
