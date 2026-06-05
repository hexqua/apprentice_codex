package jp.aquafactory.apprenticecodex.remoteownercast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.UUID;

public record RemoteOwnerContinuousRuntime(
        UUID ownerId,
        String runtimeKey,
        ItemStack castingStack,
        RemoteOwnerCastRunner.ContinuousCastSession session,
        long finishAtGameTime,
        RemoteOwnerCooldownPolicy cooldownPolicy,
        TickPreparation tickPreparation,
        FinishCallback finishCallback
) {
    public RemoteOwnerContinuousRuntime {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(runtimeKey, "runtimeKey");
        castingStack = Objects.requireNonNull(castingStack, "castingStack").copy();
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(cooldownPolicy, "cooldownPolicy");
        Objects.requireNonNull(tickPreparation, "tickPreparation");
        Objects.requireNonNull(finishCallback, "finishCallback");
    }

    @FunctionalInterface
    public interface TickPreparation {
        boolean prepare(ServerLevel level, ServerPlayer owner, RemoteOwnerCastRunner.ContinuousCastSession session);
    }

    @FunctionalInterface
    public interface FinishCallback {
        void onFinished(ServerLevel level, ServerPlayer owner, boolean cancelled);
    }
}
