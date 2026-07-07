package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MultipurposeStaffrifleRateLimiter {
    private static final ConcurrentMap<UUID, Long> NEXT_SPECIAL_CAST_TICKS = new ConcurrentHashMap<>();

    private MultipurposeStaffrifleRateLimiter() {
    }

    public static boolean canAttemptSpecialCast(ServerPlayer player) {
        var interval = Math.max(1, ApprenticeCodexServerConfig.multipurposeStaffrifleAdsFullAutoIntervalTicks());
        var gameTime = player.level().getGameTime();
        var playerId = player.getUUID();
        var nextAllowedTick = NEXT_SPECIAL_CAST_TICKS.getOrDefault(playerId, 0L);
        if (gameTime < nextAllowedTick) {
            return false;
        }

        // クライアント入力経路や連携MODの差に関係なく、専用詠唱はADS連射設定より速く通さない。
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
