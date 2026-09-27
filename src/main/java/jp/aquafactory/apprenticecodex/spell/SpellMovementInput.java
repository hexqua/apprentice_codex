package jp.aquafactory.apprenticecodex.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpellMovementInput {
    // 発動時に有効とみなす期間を Quick Blink とそろえる。
    private static final long MAX_AGE_TICKS = 10;
    private static final Map<UUID, State> SERVER = new HashMap<>();
    private static final Input ZERO = new Input(0.0F, 0.0F);

    private SpellMovementInput() {
    }

    public static void update(ServerPlayer player, float forward, float strafe) {
        if (!Float.isFinite(forward) || !Float.isFinite(strafe)
                || Math.abs(forward) > 1.0F || Math.abs(strafe) > 1.0F) {
            return;
        }
        SERVER.put(player.getUUID(), new State(new Input(forward, strafe), player.level().getGameTime()));
    }

    public static Input recent(ServerPlayer player) {
        var state = SERVER.get(player.getUUID());
        long time = player.level().getGameTime();
        return state != null && state.gameTime <= time && time - state.gameTime <= MAX_AGE_TICKS
                ? state.input : ZERO;
    }

    public static void clear(ServerPlayer player) {
        SERVER.remove(player.getUUID());
    }

    public static void clearServer() {
        SERVER.clear();
    }

    public record Input(float forward, float strafe) {
        public Input normalizedOrBackward() {
            var length = Mth.sqrt(forward * forward + strafe * strafe);
            return length <= 1.0E-3F ? new Input(-1.0F, 0.0F) : new Input(forward / length, strafe / length);
        }
    }

    private record State(Input input, long gameTime) {
    }
}
