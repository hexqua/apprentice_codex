package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.animation.AnimationBeginEvent;
import yesman.epicfight.api.event.types.animation.AnimationEndEvent;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.common.AbstractAnimatorControl;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EpicFightSwingMagicCompat {
    public static final String MOD_ID = "epicfight";

    private static final String SWING_MAGIC_EVENT_ID = "apprenticecodex:swing_magic";
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
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Animation.ATTACK_PHASE_END,
                    EpicFightSwingMagicCompat::onAttackPhaseEnd,
                    IdentifierProvider.constant(SWING_MAGIC_EVENT_ID)
            );
            playerpatch.getEventListener().registerContextAwareEvent(
                    EpicFightEventHooks.Player.COMBO_ATTACK,
                    (event, context) -> onBasicAttack(event),
                    IdentifierProvider.constant(SWING_MAGIC_EVENT_ID)
            );
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Animation.BEGIN,
                    EpicFightSwingMagicCompat::onAnimationBegin,
                    IdentifierProvider.constant(SWING_MAGIC_EVENT_ID)
            );
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Animation.END,
                    EpicFightSwingMagicCompat::onAnimationEnd,
                    IdentifierProvider.constant(SWING_MAGIC_EVENT_ID)
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
        if (!(event.getEntityPatch().getOriginal() instanceof Player player)) {
            return;
        }
        var animationAccessor = event.getAnimation();
        if (animationAccessor == null || !animationAccessor.isPresent()) {
            return;
        }
        var animation = animationAccessor.get();

        // datapack 側は Epic Fight の登録IDで書く。location はアニメーションファイル位置なので互換用の予備に留める。
        var schedule = EpicFightSwingMagicScheduleManager.getSchedule(animationAccessor.registryName());
        if (schedule == null) {
            schedule = EpicFightSwingMagicScheduleManager.getSchedule(animation.getLocation());
        }
        if (schedule == null) {
            return;
        }

        scheduleTimedTriggers(player, animation, event.getEntityPatch(), schedule);
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
        if (!(event.getEntityPatch().getOriginal() instanceof Player player)) {
            return;
        }
        var playerId = player.getUUID();
        var animation = event.getAnimation();
        var activeScheduledAnimationId = ACTIVE_SCHEDULED_ANIMATION_IDS.get(playerId);
        if (animation != null && animation.isPresent()
                && activeScheduledAnimationId != null
                && activeScheduledAnimationId == animation.get().getId()) {
            TIMED_TRIGGERS.remove(playerId);
            ACTIVE_SCHEDULED_ANIMATION_IDS.remove(playerId);
            return;
        }

        var triggers = TIMED_TRIGGERS.get(player.getUUID());
        if (triggers == null || triggers.isEmpty()) {
            return;
        }

        if (animation != null && animation.isPresent() && triggers.get(0).animationId() == animation.get().getId()) {
            TIMED_TRIGGERS.remove(player.getUUID());
        }
    }

    private static void onAttackPhaseEnd(AttackPhaseEndEvent event) {
        if (!(event.getEntityPatch().getOriginal() instanceof Player player)) {
            return;
        }
        var activeScheduledAnimationId = ACTIVE_SCHEDULED_ANIMATION_IDS.get(player.getUUID());
        if (activeScheduledAnimationId != null && activeScheduledAnimationId == getAnimationId(event.getAnimation())) {
            return;
        }

        var hand = resolveAttackHand(event);
        triggerSwingMagic(player, hand, TriggerSource.ATTACK_PHASE, getAnimationId(event.getAnimation()), event.getPhaseOrder());
    }

    private static void onBasicAttack(ComboAttackEvent event) {
        var playerpatch = event.getPlayerPatch();
        var player = playerpatch.getOriginal();
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MultipurposeStaffrifle staffrifle)) {
            return;
        }

        // Staffrifle は近接武器ではないため、Epic Fight の基本攻撃を射撃詠唱へ差し替える。
        // 1.21.1 側では ComboAttackEvent の発火順が変わる可能性があるため、この接続点は再確認する。
        event.cancel();
        if (staffrifle.tryTriggerSelectedSpell(player, false)) {
            playStaffrifleShotAnimation(playerpatch);
        }
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
        if (!(stack.getItem() instanceof AbstractSwingMagicItem)
                && !(stack.getItem() instanceof MultipurposeStaffrifle)) {
            return;
        }

        var triggerKey = new TriggerKey(player.getUUID(), hand, source, animationId, triggerIndex);
        var gameTime = player.level().getGameTime();
        var lastTriggeredTick = LAST_TRIGGERED_TICKS.put(triggerKey, gameTime);
        if (lastTriggeredTick != null && lastTriggeredTick == gameTime) {
            return;
        }

        if (stack.getItem() instanceof AbstractSwingMagicItem swingMagicItem) {
            swingMagicItem.tryTriggerImbuedSpellOnSwing(player, hand, true);
        } else if (hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && stack.getItem() instanceof MultipurposeStaffrifle staffrifle) {
            if (staffrifle.tryTriggerSelectedSpell(serverPlayer, false)) {
                playStaffrifleShotAnimation(serverPlayer);
            }
        }
    }

    public static void playStaffrifleShotAnimation(ServerPlayer player) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class)
                .filter(ServerPlayerPatch::isEpicFightMode)
                .ifPresent(EpicFightSwingMagicCompat::playStaffrifleShotAnimation);
    }

    private static void playStaffrifleShotAnimation(ServerPlayerPatch playerpatch) {
        var player = playerpatch.getOriginal();
        if (!(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            return;
        }

        playerpatch.getAnimator().playAnimation(Animations.BIPED_CROSSBOW_SHOT, 0.0F);
        var packet = new SPAnimatorControl(
                AbstractAnimatorControl.Action.PLAY_CLIENT,
                Animations.BIPED_CROSSBOW_SHOT,
                playerpatch,
                0.0F,
                AbstractAnimatorControl.Layer.COMPOSITE_LAYER,
                AbstractAnimatorControl.Priority.HIGHEST
        );
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(packet, player);
    }

    private static InteractionHand resolveAvailableSwingMagicHand(Player player, InteractionHand preferredHand) {
        if (isSupportedAttackTriggeredItem(player.getItemInHand(preferredHand))) {
            return preferredHand;
        }

        var fallbackHand = preferredHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        return isSupportedAttackTriggeredItem(player.getItemInHand(fallbackHand))
                ? fallbackHand
                : preferredHand;
    }

    private static boolean isSupportedAttackTriggeredItem(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof AbstractSwingMagicItem
                || stack.getItem() instanceof MultipurposeStaffrifle;
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
