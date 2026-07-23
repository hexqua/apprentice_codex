package jp.aquafactory.apprenticecodex.spell.terraresonance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.TerraResonanceHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.TerraResonancePulsePacket;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class TerraResonanceJobManager {
    private static final int SECOND_PULSE_DELAY_TICKS = 5;
    private static final int RESULT_DELAY_TICKS = 60;
    // 最大127立方の単独探索を約52 tickで終えつつ、同時発動時もレベル全体のtick負荷を一定に保つ。
    private static final int SEARCH_BLOCK_BUDGET_PER_TICK = 40_000;
    private static final int SEARCH_JOB_SLICE = 4_000;
    private static final float PULSE_RADIUS = 1.5F;
    private static final double PULSE_TRACKING_RANGE = 64.0D;
    private static final Map<ServerLevel, List<ScheduledAction>> ACTIONS = new WeakHashMap<>();
    private static final Map<ServerLevel, Deque<SearchAction>> SEARCHES = new WeakHashMap<>();

    private TerraResonanceJobManager() {
    }

    public static void startPulsePair(ServerLevel level, BlockTargetData targetData) {
        var target = resolveEffectTarget(targetData);
        if (target == null) {
            return;
        }

        emitPulse(level, target.center(), target.face());
        schedule(level, new PulseAction(level.getGameTime() + SECOND_PULSE_DELAY_TICKS, target.center(), target.face()));
    }

    public static void startSearch(
            ServerLevel level,
            ServerPlayer player,
            BlockPos anchor,
            Direction selectedFace,
            int range
    ) {
        SEARCHES.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(new SearchAction(
                level.getGameTime() + RESULT_DELAY_TICKS,
                player.getUUID(),
                TerraResonanceSearch.start(level, anchor, selectedFace, range)
        ));
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        processSearches(serverLevel);
        processScheduledActions(serverLevel);
    }

    private static void processSearches(ServerLevel level) {
        var searches = SEARCHES.get(level);
        if (searches == null || searches.isEmpty()) {
            return;
        }

        var remainingBudget = SEARCH_BLOCK_BUDGET_PER_TICK;
        while (remainingBudget > 0 && !searches.isEmpty()) {
            var search = searches.removeFirst();
            var player = level.getServer().getPlayerList().getPlayer(search.playerId());
            if (!isSafeRecipient(level, player)) {
                continue;
            }

            var inspected = search.job().advance(level, Math.min(SEARCH_JOB_SLICE, remainingBudget));
            remainingBudget -= inspected;
            if (search.job().isComplete()) {
                var result = search.job().result();
                schedule(level, new ResultAction(
                        Math.max(search.earliestResultTime(), level.getGameTime()),
                        search.playerId(),
                        result.found(),
                        result.highlightTargets()
                ));
            } else {
                searches.addLast(search);
            }
        }
        if (searches.isEmpty()) {
            SEARCHES.remove(level);
        }
    }

    private static void processScheduledActions(ServerLevel level) {
        var actions = ACTIONS.get(level);
        if (actions == null || actions.isEmpty()) {
            return;
        }

        var now = level.getGameTime();
        var iterator = actions.iterator();
        while (iterator.hasNext()) {
            var action = iterator.next();
            if (action.executeAt() > now) {
                continue;
            }
            action.execute(level);
            iterator.remove();
        }
        if (actions.isEmpty()) {
            ACTIONS.remove(level);
        }
    }

    private static void schedule(ServerLevel level, ScheduledAction action) {
        ACTIONS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(action);
    }

    private static void emitPulse(ServerLevel level, Vec3 center, Direction selectedFace) {
        Networks.sendToPlayersNear(
                level,
                center,
                PULSE_TRACKING_RANGE,
                new TerraResonancePulsePacket(center, selectedFace, PULSE_RADIUS)
        );
        level.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }

    private static EffectTarget resolveEffectTarget(BlockTargetData targetData) {
        var pos = targetData.getHitBlockPos();
        var face = targetData.getHitFace();
        if (pos == null || face == null) {
            return null;
        }

        var normal = face.getNormal();
        return new EffectTarget(
                pos.getCenter().add(normal.getX() * 0.501D, normal.getY() * 0.501D, normal.getZ() * 0.501D),
                face
        );
    }

    private static boolean isSafeRecipient(ServerLevel level, ServerPlayer player) {
        return player != null
                && player.serverLevel() == level
                && player.isAlive()
                && !player.isRemoved();
    }

    private interface ScheduledAction {
        long executeAt();

        void execute(ServerLevel level);
    }

    private record PulseAction(long executeAt, Vec3 center, Direction selectedFace) implements ScheduledAction {
        @Override
        public void execute(ServerLevel level) {
            emitPulse(level, center, selectedFace);
        }
    }

    private record SearchAction(
            long earliestResultTime,
            UUID playerId,
            TerraResonanceSearch.SearchJob job
    ) {
    }

    private record ResultAction(
            long executeAt,
            UUID playerId,
            boolean found,
            List<BlockPos> highlightTargets
    ) implements ScheduledAction {
        private ResultAction {
            highlightTargets = List.copyOf(highlightTargets);
        }

        @Override
        public void execute(ServerLevel level) {
            var player = level.getServer().getPlayerList().getPlayer(playerId);
            if (!isSafeRecipient(level, player)) {
                return;
            }

            if (found) {
                player.displayClientMessage(
                        Component.translatable("ui.apprenticecodex.terra_resonance.found")
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
                if (!highlightTargets.isEmpty()) {
                    Networks.sendToPlayer(player, new TerraResonanceHighlightsPacket(highlightTargets));
                }
            } else {
                player.displayClientMessage(
                        Component.translatable("ui.apprenticecodex.terra_resonance.not_found")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
        }
    }

    private record EffectTarget(Vec3 center, Direction face) {
    }
}
