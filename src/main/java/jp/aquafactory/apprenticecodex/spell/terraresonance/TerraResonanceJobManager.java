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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class TerraResonanceJobManager {
    private static final int SECOND_PULSE_DELAY_TICKS = 5;
    private static final int RESULT_DELAY_TICKS = 60;
    private static final float PULSE_RADIUS = 1.5F;
    private static final double PULSE_TRACKING_RANGE = 64.0D;
    private static final Map<ServerLevel, List<ScheduledAction>> ACTIONS = new WeakHashMap<>();

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

    public static void submitResult(ServerLevel level, ServerPlayer player, TerraResonanceSearch.SearchResult result) {
        schedule(level, new ResultAction(
                level.getGameTime() + RESULT_DELAY_TICKS,
                player.getUUID(),
                result.found(),
                result.highlightTargets()
        ));
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        var actions = ACTIONS.get(serverLevel);
        if (actions == null || actions.isEmpty()) {
            return;
        }

        var now = serverLevel.getGameTime();
        var iterator = actions.iterator();
        while (iterator.hasNext()) {
            var action = iterator.next();
            if (action.executeAt() > now) {
                continue;
            }
            action.execute(serverLevel);
            iterator.remove();
        }
        if (actions.isEmpty()) {
            ACTIONS.remove(serverLevel);
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

        private static boolean isSafeRecipient(ServerLevel level, ServerPlayer player) {
            return player != null
                    && player.serverLevel() == level
                    && player.isAlive()
                    && !player.isRemoved();
        }
    }

    private record EffectTarget(Vec3 center, Direction face) {
    }
}
