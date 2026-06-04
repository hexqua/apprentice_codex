package jp.aquafactory.apprenticecodex.remoteownercast;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class RemoteOwnerContinuousCastManager {
    private static final Map<ServerLevel, List<RemoteOwnerContinuousRuntime>> ACTIVE_RUNTIMES = new WeakHashMap<>();

    private RemoteOwnerContinuousCastManager() {
    }

    public static void register(ServerLevel level, RemoteOwnerContinuousRuntime runtime) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(runtime, "runtime");
        ACTIVE_RUNTIMES.computeIfAbsent(level, ignored -> new ArrayList<>()).add(runtime);
    }

    public static boolean hasActive(ServerLevel level, UUID ownerId, String runtimeKey) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(runtimeKey, "runtimeKey");

        var runtimes = ACTIVE_RUNTIMES.get(level);
        if (runtimes == null) {
            return false;
        }
        return runtimes.stream()
                .anyMatch(runtime -> runtime.ownerId().equals(ownerId)
                        && runtime.runtimeKey().equals(runtimeKey)
                        && !runtime.session().isFinished());
    }

    public static void clearOwner(ServerPlayer owner, boolean cancelled) {
        clearOwner(owner, cancelled, owner.serverLevel());
    }

    public static void clearOwner(ServerPlayer owner, boolean cancelled, @Nullable ServerLevel ownerLevel) {
        Objects.requireNonNull(owner, "owner");
        RemoteOwnerCooldownManager.clearPending(owner);

        var levelIterator = ACTIVE_RUNTIMES.entrySet().iterator();
        while (levelIterator.hasNext()) {
            var levelEntry = levelIterator.next();
            var level = levelEntry.getKey();
            var runtimes = levelEntry.getValue();
            var runtimeIterator = runtimes.iterator();
            while (runtimeIterator.hasNext()) {
                var runtime = runtimeIterator.next();
                if (!runtime.ownerId().equals(owner.getUUID())) {
                    continue;
                }
                finishRuntime(level, runtime, level == ownerLevel ? owner : null, cancelled);
                runtimeIterator.remove();
            }
            if (runtimes.isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        var runtimes = ACTIVE_RUNTIMES.get(level);
        if (runtimes == null || runtimes.isEmpty()) {
            return;
        }

        var iterator = runtimes.iterator();
        while (iterator.hasNext()) {
            var runtime = iterator.next();
            var owner = level.getPlayerByUUID(runtime.ownerId());
            if (!(owner instanceof ServerPlayer serverPlayer) || serverPlayer.isDeadOrDying() || serverPlayer.isSpectator()) {
                finishRuntime(level, runtime, null, true);
                iterator.remove();
                continue;
            }

            if (!runtime.tickPreparation().prepare(level, serverPlayer, runtime.session())) {
                finishRuntime(level, runtime, serverPlayer, true);
                iterator.remove();
                continue;
            }

            if (level.getGameTime() >= runtime.finishAtGameTime() && !runtime.session().isFinished()) {
                RemoteOwnerCastRunner.finishContinuousCast(level, serverPlayer, runtime.session(), false);
            } else if (RemoteOwnerCastRunner.tickContinuousCast(level, serverPlayer, runtime.session())) {
                continue;
            }

            finishRuntime(level, runtime, serverPlayer, false);
            iterator.remove();
        }

        if (runtimes.isEmpty()) {
            ACTIVE_RUNTIMES.remove(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOwner(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOwner(player, true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOwner(player, true);
        }
    }

    private static void finishRuntime(
            ServerLevel level,
            RemoteOwnerContinuousRuntime runtime,
            @Nullable ServerPlayer owner,
            boolean cancelled
    ) {
        var session = runtime.session();
        if (!session.isFinished()) {
            if (owner != null) {
                RemoteOwnerCastRunner.finishContinuousCast(level, owner, session, cancelled);
            } else {
                RemoteOwnerCastRunner.cancelContinuousCastWithoutOwner(session);
            }
        }

        if (owner == null) {
            return;
        }

        if (session.consumeFinishedCooldownTicks() > 0) {
            var spellData = session.spellData();
            var castSource = session.castSource();
            if (spellData != null && castSource != null) {
                RemoteOwnerCooldownManager.addCooldown(
                        owner,
                        spellData,
                        castSource,
                        runtime.castingStack(),
                        runtime.cooldownPolicy()
                );
            }
        }
        runtime.finishCallback().onFinished(level, owner, cancelled);
    }
}
