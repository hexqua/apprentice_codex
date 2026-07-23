package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class OtherworldLensSessionManager {
    private static final double KEEP_RANGE_SQ = 12.0 * 12.0;
    private static final Map<UUID, Session> BY_CASTER = new HashMap<>();
    private static final Map<LensPosition, UUID> BY_POSITION = new HashMap<>();

    private OtherworldLensSessionManager() {
    }

    static UUID start(ServerPlayer player, BlockPos lensPos, BlockPos targetPos, Block targetBlock) {
        var session = new Session(
                UUID.randomUUID(),
                player.getUUID(),
                player.getGameProfile().getName(),
                player.level().dimension(),
                lensPos.immutable(),
                targetPos.immutable(),
                BuiltInRegistries.BLOCK.getKey(targetBlock).toString(),
                player.level().getGameTime()
        );
        BY_CASTER.put(player.getUUID(), session);
        BY_POSITION.put(new LensPosition(session.dimension(), session.lensPos()), player.getUUID());
        ApprenticeCodex.LOGGER.info(
                "OtherworldLens started: player={} uuid={} dimension={} lensPos={} target={} targetBlock={}",
                session.playerName(), session.casterId(), session.dimension().location(), session.lensPos(),
                session.targetPos(), session.targetBlockId()
        );
        return session.sessionId();
    }

    @Nullable
    static EndReason validate(ServerPlayer player, @Nullable UUID expectedSessionId) {
        var session = BY_CASTER.get(player.getUUID());
        if (session == null || expectedSessionId == null || !session.sessionId().equals(expectedSessionId)) {
            return EndReason.CANCELLED;
        }
        if (!player.isAlive()) {
            return EndReason.DEATH;
        }
        if (!session.dimension().equals(player.level().dimension())) {
            return EndReason.DIMENSION_CHANGED;
        }
        if (player.position().distanceToSqr(session.lensPos().getCenter()) > KEEP_RANGE_SQ) {
            return EndReason.TOO_FAR;
        }

        var state = player.level().getBlockState(session.lensPos());
        if (state.isAir()) {
            return EndReason.LENS_MISSING;
        }
        if (!state.is(BlockRegistry.OTHERWORLD_LENS_LENS.get())) {
            return EndReason.LENS_REPLACED;
        }
        return null;
    }

    static boolean isActiveAt(ServerLevel level, BlockPos pos) {
        var casterId = BY_POSITION.get(new LensPosition(level.dimension(), pos));
        if (casterId == null) {
            return false;
        }
        var session = BY_CASTER.get(casterId);
        return session != null
                && session.dimension().equals(level.dimension())
                && session.lensPos().equals(pos)
                && level.getBlockState(pos).is(BlockRegistry.OTHERWORLD_LENS_LENS.get());
    }

    static void finish(ServerPlayer player, EndReason reason) {
        finish(player.getUUID(), player.server, reason);
    }

    static void finish(ServerPlayer player, @Nullable UUID expectedSessionId, EndReason reason) {
        var session = BY_CASTER.get(player.getUUID());
        if (session == null || expectedSessionId == null || !session.sessionId().equals(expectedSessionId)) {
            return;
        }
        finish(player.getUUID(), player.server, reason);
    }

    static void finish(UUID casterId, MinecraftServer server, EndReason reason) {
        var session = BY_CASTER.remove(casterId);
        if (session == null) {
            return;
        }
        BY_POSITION.remove(new LensPosition(session.dimension(), session.lensPos()), casterId);

        var level = server.getLevel(session.dimension());
        if (level != null && level.getBlockState(session.lensPos()).is(BlockRegistry.OTHERWORLD_LENS_LENS.get())) {
            level.removeBlock(session.lensPos(), false);
        }

        var endGameTime = level != null ? level.getGameTime() : session.startGameTime();
        ApprenticeCodex.LOGGER.info(
                "OtherworldLens ended: player={} uuid={} dimension={} lensPos={} target={} targetBlock={} reason={} durationTicks={}",
                session.playerName(), session.casterId(), session.dimension().location(), session.lensPos(),
                session.targetPos(), session.targetBlockId(), reason, Math.max(0L, endGameTime - session.startGameTime())
        );
    }

    static void finishAll(MinecraftServer server, EndReason reason) {
        for (var casterId : BY_CASTER.keySet().toArray(UUID[]::new)) {
            finish(casterId, server, reason);
        }
    }

    static void logOrphanCleanup(ServerLevel level, BlockPos pos) {
        ApprenticeCodex.LOGGER.info(
                "OtherworldLens orphan cleaned: dimension={} lensPos={} reason={}",
                level.dimension().location(), pos, EndReason.ORPHAN_CLEANUP
        );
    }

    enum EndReason {
        COMPLETED,
        CANCELLED,
        TOO_FAR,
        DIMENSION_CHANGED,
        DEATH,
        LOGOUT,
        LENS_MISSING,
        LENS_REPLACED,
        ORPHAN_CLEANUP,
        SERVER_STOPPING
    }

    private record LensPosition(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Session(UUID sessionId, UUID casterId, String playerName, ResourceKey<Level> dimension,
                           BlockPos lensPos, BlockPos targetPos, String targetBlockId, long startGameTime) {
    }
}
