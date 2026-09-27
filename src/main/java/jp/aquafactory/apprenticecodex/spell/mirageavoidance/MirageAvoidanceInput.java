package jp.aquafactory.apprenticecodex.spell.mirageavoidance;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MirageAvoidanceInput {
    private static final double INPUT_EPSILON_SQ = 1.0E-6D;
    private static final long PENDING_INPUT_TTL_TICKS = 200L;
    private static final Map<UUID, PendingInput> PENDING_INPUTS = new ConcurrentHashMap<>();

    private MirageAvoidanceInput() {
    }

    public static void setPending(Player player, float forward, float strafe) {
        PENDING_INPUTS.put(
                player.getUUID(),
                new PendingInput(sanitize(forward, strafe), player.level().getGameTime() + PENDING_INPUT_TTL_TICKS)
        );
    }

    public static void clearPending(Player player) {
        PENDING_INPUTS.remove(player.getUUID());
    }

    public static DirectionInput consumePending(Player player) {
        var pendingInput = PENDING_INPUTS.remove(player.getUUID());
        if (pendingInput != null && pendingInput.expiresAtGameTime >= player.level().getGameTime()) {
            return pendingInput.input;
        }

        return sanitize(player.zza, player.xxa);
    }

    public static DirectionInput sanitize(float forward, float strafe) {
        var resolvedForward = Float.isFinite(forward) ? Mth.clamp(forward, -1.0F, 1.0F) : 0.0F;
        var resolvedStrafe = Float.isFinite(strafe) ? Mth.clamp(strafe, -1.0F, 1.0F) : 0.0F;
        var input = new Vec3(resolvedStrafe, 0.0D, resolvedForward);
        if (input.lengthSqr() <= INPUT_EPSILON_SQ) {
            return DirectionInput.defaultBackward();
        }

        var normalized = input.normalize();
        return new DirectionInput((float) normalized.z, (float) normalized.x);
    }

    public record DirectionInput(float forward, float strafe) {
        private static DirectionInput defaultBackward() {
            return new DirectionInput(-1.0F, 0.0F);
        }
    }

    private record PendingInput(DirectionInput input, long expiresAtGameTime) {
    }
}
