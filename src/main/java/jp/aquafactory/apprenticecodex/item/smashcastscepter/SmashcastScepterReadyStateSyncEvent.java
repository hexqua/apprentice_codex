package jp.aquafactory.apprenticecodex.item.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncSmashcastScepterReadyStatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SmashcastScepterReadyStateSyncEvent {
    private static final Map<UUID, Boolean> LAST_SYNCED_READY_STATES = new HashMap<>();

    private SmashcastScepterReadyStateSyncEvent() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (var player : serverLevel.players()) {
            syncIfChanged(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer, resolveReadyState(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer, resolveReadyState(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer, resolveReadyState(serverPlayer));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SYNCED_READY_STATES.remove(event.getEntity().getUUID());
    }

    public static boolean resolveReadyState(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof SmashcastScepter
                && SmashcastScepter.isSmashAttack(player);
    }

    private static void syncIfChanged(ServerPlayer player) {
        boolean ready = resolveReadyState(player);
        var previous = LAST_SYNCED_READY_STATES.get(player.getUUID());
        if (previous != null && previous == ready) {
            return;
        }

        sync(player, ready);
    }

    private static void sync(ServerPlayer player, boolean ready) {
        LAST_SYNCED_READY_STATES.put(player.getUUID(), ready);
        Networks.sendToPlayer(player, new SyncSmashcastScepterReadyStatePacket(ready));
    }
}
