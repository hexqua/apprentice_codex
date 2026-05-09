package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.AnimationBeginEvent;
import yesman.epicfight.world.entity.eventlistener.AnimationEndEvent;
import yesman.epicfight.world.entity.eventlistener.AttackPhaseEndEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EpicFightSwingMagicCompat {
    public static final String MOD_ID = "epicfight";

    private static final UUID SWING_MAGIC_EVENT_UUID = UUID.fromString("c3e82f78-5e2e-41b7-86af-f38c7adcb8bd");
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<TriggerKey, Long> LAST_TRIGGERED_TICKS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, List<TimedTrigger>> TIMED_TRIGGERS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Integer> ACTIVE_SCHEDULED_ANIMATION_IDS = new ConcurrentHashMap<>();

    private EpicFightSwingMagicCompat() {
    }

    public static void install(ServerPlayer player) {
        if (!player.isAlive() || INSTALLED_PLAYERS.contains(player.getUUID())) {
            return;
        }

        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(playerpatch -> {
            // Epic Fight は vanilla の attack input を経由しないため、攻撃フェーズ終端を swing 発動点として使う。
            playerpatch.getEventListener().addEventListener(
                    EventType.ATTACK_PHASE_END_EVENT,
                    SWING_MAGIC_EVENT_UUID,
                    EpicFightSwingMagicCompat::onAttackPhaseEnd
            );
            playerpatch.getEventListener().addEventListener(
                    EventType.ANIMATION_BEGIN_EVENT,
                    SWING_MAGIC_EVENT_UUID,
                    EpicFightSwingMagicCompat::onAnimationBegin
            );
            playerpatch.getEventListener().addEventListener(
                    EventType.ANIMATION_END_EVENT,
                    SWING_MAGIC_EVENT_UUID,
                    EpicFightSwingMagicCompat::onAnimationEnd
            );
            INSTALLED_PLAYERS.add(player.getUUID());
        });
    }

    public static void tick(ServerPlayer player) {
        processTimedTriggers(player);
    }

    public static void clear(ServerPlayer player) {
        var playerId = player.getUUID();
        INSTALLED_PLAYERS.remove(playerId);
        LAST_TRIGGERED_TICKS.keySet().removeIf(key -> key.playerId().equals(playerId));
        TIMED_TRIGGERS.remove(playerId);
        ACTIVE_SCHEDULED_ANIMATION_IDS.remove(playerId);
    }

    private static void onAnimationBegin(AnimationBeginEvent event) {
        var player = event.getPlayerPatch().getOriginal();

        var animation = event.getAnimation();
        if (animation == null) {
            return;
        }

        // datapack 側は Epic Fight の登録IDで書く。location はアニメーションファイル位置なので互換用の予備に留める。
        var schedule = EpicFightSwingMagicScheduleManager.getSchedule(animation.getRegistryName());
        if (schedule == null) {
            schedule = EpicFightSwingMagicScheduleManager.getSchedule(animation.getLocation());
        }
        if (schedule == null) {
            return;
        }

        scheduleTimedTriggers(player, animation, event.getPlayerPatch(), schedule);
    }

    private static void scheduleTimedTriggers(
            Player player,
            StaticAnimation animation,
            LivingEntityPatch<?> playerpatch,
            EpicFightSwingMagicScheduleManager.Schedule schedule
    ) {
        var gameTime = player.level().getGameTime();
        var animationId = animation.getId();
        var playSpeed = resolvePlaySpeed(animation, playerpatch);
        var scheduleTriggers = schedule.triggers();
        var triggers = new ArrayList<TimedTrigger>(scheduleTriggers.size());
        for (int i = 0; i < scheduleTriggers.size(); i++) {
            var trigger = scheduleTriggers.get(i);
            triggers.add(new TimedTrigger(
                    gameTime + secondsToTicks(trigger.time(), playSpeed),
                    animationId,
                    i,
                    trigger.hand()
            ));
        }

        TIMED_TRIGGERS.put(player.getUUID(), triggers);
        ACTIVE_SCHEDULED_ANIMATION_IDS.put(player.getUUID(), animationId);
    }

    private static void onAnimationEnd(AnimationEndEvent event) {
        var player = event.getPlayerPatch().getOriginal();
        var playerId = player.getUUID();
        var animation = event.getAnimation();
        var activeScheduledAnimationId = ACTIVE_SCHEDULED_ANIMATION_IDS.get(playerId);
        if (animation != null && activeScheduledAnimationId != null && activeScheduledAnimationId == animation.getId()) {
            TIMED_TRIGGERS.remove(playerId);
            ACTIVE_SCHEDULED_ANIMATION_IDS.remove(playerId);
            return;
        }

        var triggers = TIMED_TRIGGERS.get(player.getUUID());
        if (triggers == null || triggers.isEmpty()) {
            return;
        }

        if (animation != null && triggers.get(0).animationId() == animation.getId()) {
            TIMED_TRIGGERS.remove(player.getUUID());
        }
    }

    private static void onAttackPhaseEnd(AttackPhaseEndEvent event) {
        var player = event.getPlayerPatch().getOriginal();
        var activeScheduledAnimationId = ACTIVE_SCHEDULED_ANIMATION_IDS.get(player.getUUID());
        if (activeScheduledAnimationId != null && activeScheduledAnimationId == getAnimationId(event.getAnimation())) {
            return;
        }

        var hand = resolveSwingMagicHand(player, event);
        triggerSwingMagic(player, hand, TriggerSource.ATTACK_PHASE, getAnimationId(event.getAnimation()), event.getPhaseOrder());
    }

    private static void processTimedTriggers(ServerPlayer player) {
        var playerId = player.getUUID();
        var triggers = TIMED_TRIGGERS.get(playerId);
        if (triggers == null || triggers.isEmpty()) {
            return;
        }

        var gameTime = player.level().getGameTime();
        triggers.removeIf(trigger -> {
            if (trigger.triggerTick() > gameTime) {
                return false;
            }

            var hand = resolveAvailableSwingMagicHand(player, trigger.preferredHand());
            triggerSwingMagic(player, hand, TriggerSource.TIMED_ANIMATION, trigger.animationId(), trigger.index());
            return true;
        });

        if (triggers.isEmpty()) {
            TIMED_TRIGGERS.remove(playerId);
        }
    }

    private static void triggerSwingMagic(
            Player player,
            InteractionHand hand,
            TriggerSource source,
            int animationId,
            int triggerIndex
    ) {
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof AbstractSwingMagicItem swingMagicItem)) {
            return;
        }

        var triggerKey = new TriggerKey(player.getUUID(), hand, source, animationId, triggerIndex);
        var gameTime = player.level().getGameTime();
        var lastTriggeredTick = LAST_TRIGGERED_TICKS.put(triggerKey, gameTime);
        if (lastTriggeredTick != null && lastTriggeredTick == gameTime) {
            return;
        }

        swingMagicItem.tryTriggerImbuedSpellOnSwing(player, hand, true);
    }

    private static InteractionHand resolveSwingMagicHand(Player player, AttackPhaseEndEvent event) {
        return resolveAvailableSwingMagicHand(player, resolveAttackHand(event));
    }

    private static InteractionHand resolveAvailableSwingMagicHand(Player player, InteractionHand preferredHand) {
        if (player.getItemInHand(preferredHand).getItem() instanceof AbstractSwingMagicItem) {
            return preferredHand;
        }

        var fallbackHand = preferredHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        return player.getItemInHand(fallbackHand).getItem() instanceof AbstractSwingMagicItem
                ? fallbackHand
                : preferredHand;
    }

    private static InteractionHand resolveAttackHand(AttackPhaseEndEvent event) {
        var phase = event.getPhase();
        if (phase == null) {
            return InteractionHand.MAIN_HAND;
        }

        var hand = phase.getHand();
        return hand != null ? hand : InteractionHand.MAIN_HAND;
    }

    private static int getAnimationId(AnimationManager.AnimationAccessor<? extends StaticAnimation> animation) {
        return animation != null ? animation.id() : -1;
    }

    private static long secondsToTicks(float seconds, float playSpeed) {
        return Math.max(1L, Math.round(seconds * 20.0F / playSpeed));
    }

    private static float resolvePlaySpeed(StaticAnimation animation, LivingEntityPatch<?> playerpatch) {
        if (animation instanceof AttackAnimation attackAnimation) {
            var playSpeed = attackAnimation.getPlaySpeed(playerpatch, attackAnimation);
            return playSpeed > 0.0F ? playSpeed : 1.0F;
        }

        return 1.0F;
    }

    private enum TriggerSource {
        ATTACK_PHASE,
        TIMED_ANIMATION
    }

    private record TriggerKey(UUID playerId, InteractionHand hand, TriggerSource source, int animationId, int triggerIndex) {
    }

    private record TimedTrigger(long triggerTick, int animationId, int index, InteractionHand preferredHand) {
    }
}
