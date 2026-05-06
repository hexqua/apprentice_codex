package jp.aquafactory.apprenticecodex.item.offhand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.CombatTrackerAccessor;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncPhotonSiphonCombatStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PhotonSiphonCombatStateSyncEvent {
    private static final Map<UUID, Boolean> LAST_SYNCED_COMBAT_STATES = new HashMap<>();

    private PhotonSiphonCombatStateSyncEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        syncIfChanged(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer, resolveCombatState(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer, resolveCombatState(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SYNCED_COMBAT_STATES.remove(event.getEntity().getUUID());
    }

    private static void syncIfChanged(ServerPlayer player) {
        boolean inCombat = resolveCombatState(player);
        var previous = LAST_SYNCED_COMBAT_STATES.get(player.getUUID());
        if (previous != null && previous == inCombat) {
            return;
        }

        sync(player, inCombat);
    }

    private static void sync(ServerPlayer player, boolean inCombat) {
        LAST_SYNCED_COMBAT_STATES.put(player.getUUID(), inCombat);
        Networks.sendToPlayer(player, new SyncPhotonSiphonCombatStatePacket(inCombat));
    }

    private static boolean resolveCombatState(ServerPlayer player) {
        // Iron's Recall はサーバー側 CombatTracker の private 状態を見るため、描画用にも同じ値だけ同期する。
        return ((CombatTrackerAccessor) player.getCombatTracker()).apprenticecodex$isInCombat();
    }
}
