package jp.aquafactory.apprenticecodex.item.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SmashcastScepter;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SmashcastScepterFallProtectionEvent {
    private static final Map<ServerLevel, Map<UUID, PendingFallProtection>> PENDING_PROTECTIONS = new WeakHashMap<>();
    private static final int FALL_PROTECTION_TICKS = 40;

    private SmashcastScepterFallProtectionEvent() {
    }

    public static void register(ServerPlayer player) {
        var movement = player.getDeltaMovement();
        player.setDeltaMovement(new Vec3(movement.x, SmashcastScepter.WIND_BURST_MOTION_EPSILON, movement.z));
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        PENDING_PROTECTIONS.computeIfAbsent(player.serverLevel(), ignored -> new HashMap<>())
                .put(player.getUUID(), new PendingFallProtection(player.serverLevel().getGameTime()));
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getDistance() <= 0.0F) {
            return;
        }

        var protections = PENDING_PROTECTIONS.get(player.serverLevel());
        if (protections == null) {
            return;
        }

        var protection = protections.get(player.getUUID());
        if (protection == null) {
            return;
        }

        if (!protection.isActive(player.serverLevel().getGameTime())) {
            protections.remove(player.getUUID());
            removeLevelIfEmpty(player.serverLevel(), protections);
            return;
        }

        // 1.21.1申し送り事項:
        // 1.20.1 にはバニラメイスの impulse fall damage context がないため、
        // Forge の LivingFallEvent で次回着地だけを代替キャンセルする。
        // 1.21.1 へ port する際はこの予約キャンセルではなく、
        // MaceItem と同じ currentImpulseImpactPos / setIgnoreFallDamageFromCurrentImpulse(true) へ置き換える。
        event.setCanceled(true);
        protections.remove(player.getUUID());
        removeLevelIfEmpty(player.serverLevel(), protections);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        var protections = PENDING_PROTECTIONS.get(serverLevel);
        if (protections == null || protections.isEmpty()) {
            return;
        }

        var currentGameTime = serverLevel.getGameTime();
        protections.entrySet().removeIf(entry -> !entry.getValue().isActive(currentGameTime));
        removeLevelIfEmpty(serverLevel, protections);
    }

    private static void removeLevelIfEmpty(ServerLevel level, Map<UUID, PendingFallProtection> protections) {
        if (protections.isEmpty()) {
            PENDING_PROTECTIONS.remove(level);
        }
    }

    private record PendingFallProtection(long gameTime) {
        private boolean isActive(long currentGameTime) {
            return currentGameTime - gameTime <= FALL_PROTECTION_TICKS;
        }
    }
}
