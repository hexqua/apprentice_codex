package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.server.level.ServerPlayer;

public final class MultipurposeStaffrifleRateLimiter {
    private static final ConcurrentMap<UUID, Long> NEXT_SPECIAL_CAST_TICKS = new ConcurrentHashMap<>();

    private MultipurposeStaffrifleRateLimiter() {
    }

    public static boolean canAttemptSpecialCast(ServerPlayer player) {
        var interval = 3;
        var gameTime = player.level().getGameTime();
        var playerId = player.getUUID();
        var nextAllowedTick = NEXT_SPECIAL_CAST_TICKS.getOrDefault(playerId, 0L);
        if (gameTime < nextAllowedTick) {
            return false;
        }

        // クライアント入力経路や連携MODの差に関係なく、同一入力の重複や過剰なpacketを従来の最小間隔で抑える。
        NEXT_SPECIAL_CAST_TICKS.put(playerId, gameTime + interval);
        return true;
    }

    public static void clear(ServerPlayer player) {
        NEXT_SPECIAL_CAST_TICKS.remove(player.getUUID());
    }

    public static void clearAll() {
        NEXT_SPECIAL_CAST_TICKS.clear();
    }
}
