package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.EquipmentSpellTimingConfigState;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunAmmoConsumption;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncEquipmentSpellTimingConfigPacket;
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
public final class EquipmentSpellTimingConfigSyncEvents {
    private EquipmentSpellTimingConfigSyncEvents() {
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

    private static SyncEquipmentSpellTimingConfigPacket createPacket() {
        return new SyncEquipmentSpellTimingConfigPacket(
                ApprenticeCodexServerConfig.craftsmansDelightCooldownMultiplier(),
                ApprenticeCodexServerConfig.magiAgentSuitBootsCooldownMultiplier(),
                ApprenticeCodexServerConfig.magiAgentSuitBootsCastTimeMultiplier(),
                ApprenticeCodexServerConfig.magiAgentSuitAmmoNoConsumeChance()
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

            // 装備を付け直さずに設定変更を反映し、接続中clientへ同じ値を同期する。
            EquipmentSpellTimingConfigState.setAmmoNoConsumeChance(ApprenticeCodexServerConfig.magiAgentSuitAmmoNoConsumeChance());
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(() -> {
                for (var player : server.getPlayerList().getPlayers()) SpellgunAmmoConsumption.refreshHoodModifier(player);
                syncToAllPlayers();
            });
        }
    }

    @Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            EquipmentSpellTimingConfigState.reset();
        }
    }
}
