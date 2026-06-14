package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.IdentifierProvider;
import yesman.epicfight.api.event.types.animation.AnimationBeginEvent;
import yesman.epicfight.api.event.types.animation.AnimationEndEvent;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.event.types.entity.DealDamageEvent;
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
    private static final int CRYSTAL_BLADED_STAFF_MISS_EVALUATION_DELAY_TICKS = 2;

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
            playerpatch.getEventListener().registerEvent(
                    EpicFightEventHooks.Entity.DELIVER_DAMAGE_PRE,
                    EpicFightSwingMagicCompat::onDealDamagePre,
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
        recordAttackPhaseHitIfCrystalBladedStaff(event, player, hand);
        triggerSwingMagicFromAttackPhase(player, hand, getAnimationId(event.getAnimation()), event.getPhaseOrder());
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

    private static void onDealDamagePre(DealDamageEvent.Pre event) {
        if (!(event.getEntityPatch() instanceof ServerPlayerPatch playerpatch)) {
            return;
        }

        var player = playerpatch.getOriginal();
        var hand = resolveDamageHand(event, playerpatch);
        if (event.getTarget() == null
                || hand == null
                || !CrystalBladedStaff.isCrystalBladedStaff(player.getItemInHand(hand))) {
            return;
        }

        CrystalBladedStaffAttackContextManager.recordRecentCrystalBladedStaffHit(player, hand);
    }

    private static void recordAttackPhaseHitIfCrystalBladedStaff(
            AttackPhaseEndEvent event,
            Player player,
            InteractionHand hand
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var triggerHand = resolveSwingMagicTriggerHand(player, hand);
        if (!CrystalBladedStaff.isCrystalBladedStaff(player.getItemInHand(triggerHand))) {
            return;
        }

        var hitEntities = event.getPlayerPatch().getCurrentlyActuallyHitEntities();
        if (hitEntities == null || hitEntities.isEmpty()) {
            return;
        }

        // Epic Fight は命中後にフェーズ終了イベントを出すため、この時点の実命中リストをmiss抑制へ反映する。
        // 1.21.1 側ではフェーズ終了時まで ACTUALLY_HIT_ENTITIES が残るかを再確認する。
        CrystalBladedStaffAttackContextManager.recordRecentCrystalBladedStaffHit(serverPlayer, triggerHand);
    }

    private static InteractionHand resolveDamageHand(DealDamageEvent.Pre event, ServerPlayerPatch playerpatch) {
        var player = playerpatch.getOriginal();
        var usedItemHand = resolveHeldHandFromUsedItem(player, event.getDamageSource().getUsedItem());
        if (usedItemHand != null) {
            return usedItemHand;
        }

        var animationAccessor = event.getDamageSource().getAnimation();
        if (animationAccessor != null && animationAccessor.get() instanceof AttackAnimation attackAnimation) {
            var animationPlayer = playerpatch.getAnimator().getPlayer(animationAccessor);
            if (animationPlayer != null && animationPlayer.isPresent()) {
                var phase = attackAnimation.getPhaseByTime(animationPlayer.get().getElapsedTime());
                if (phase != null && phase.getHand() != null) {
                    return phase.getHand();
                }
            }
        }

        return null;
    }

    private static InteractionHand resolveHeldHandFromUsedItem(Player player, ItemStack usedItem) {
        if (usedItem == null || usedItem.isEmpty()) {
            return null;
        }

        if (usedItem == player.getMainHandItem()) {
            return InteractionHand.MAIN_HAND;
        }
        if (usedItem == player.getOffhandItem()) {
            return InteractionHand.OFF_HAND;
        }

        var mainHandMatches = ItemStack.isSameItemSameTags(usedItem, player.getMainHandItem());
        var offHandMatches = ItemStack.isSameItemSameTags(usedItem, player.getOffhandItem());
        if (mainHandMatches && !offHandMatches) {
            return InteractionHand.MAIN_HAND;
        }
        if (offHandMatches && !mainHandMatches) {
            return InteractionHand.OFF_HAND;
        }

        return null;
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

    public static boolean triggerSwingMagicFromAttackPhase(
            Player player,
            InteractionHand hand,
            int animationId,
            int triggerIndex
    ) {
        return triggerSwingMagic(player, hand, TriggerSource.ATTACK_PHASE, animationId, triggerIndex);
    }

    public static InteractionHand resolveSwingMagicTriggerHand(Player player, InteractionHand hand) {
        return EpicFightScrollcasterGauntletOffhandBridge.resolveSwingMagicHand(player, hand);
    }

    private static boolean triggerSwingMagic(
            Player player,
            InteractionHand hand,
            TriggerSource source,
            int animationId,
            int triggerIndex
    ) {
        var triggerHand = resolveSwingMagicTriggerHand(player, hand);
        var stack = player.getItemInHand(triggerHand);
        if (!isSupportedAttackTriggeredItem(player, triggerHand)) {
            return false;
        }

        var triggerKey = new TriggerKey(player.getUUID(), triggerHand, source, animationId, triggerIndex);
        var gameTime = player.level().getGameTime();
        var lastTriggeredTick = LAST_TRIGGERED_TICKS.put(triggerKey, gameTime);
        if (lastTriggeredTick != null && lastTriggeredTick == gameTime) {
            return false;
        }

        if (CrystalBladedStaff.isCrystalBladedStaff(stack) && player instanceof ServerPlayer serverPlayer) {
            return CrystalBladedStaffAttackContextManager.requestMissTrigger(
                    serverPlayer,
                    triggerHand,
                    true,
                    CRYSTAL_BLADED_STAFF_MISS_EVALUATION_DELAY_TICKS
            );
        } else if (stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem) {
            return swingTriggeredMagicItem.tryTriggerSpellOnSwing(player, triggerHand, true);
        } else if (triggerHand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
                && stack.getItem() instanceof MultipurposeStaffrifle staffrifle) {
            if (staffrifle.tryTriggerSelectedSpell(serverPlayer, false)) {
                playStaffrifleShotAnimation(serverPlayer);
                return true;
            }
        }
        return false;
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

    public static InteractionHand resolveAvailableSwingMagicTriggerHand(Player player, InteractionHand preferredHand) {
        return resolveAvailableSwingMagicHand(player, preferredHand);
    }

    private static InteractionHand resolveAvailableSwingMagicHand(Player player, InteractionHand preferredHand) {
        var resolvedPreferredHand = resolveSwingMagicTriggerHand(player, preferredHand);
        if (isSupportedAttackTriggeredItem(player, resolvedPreferredHand)) {
            return resolvedPreferredHand;
        }

        var fallbackHand = preferredHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        var resolvedFallbackHand = resolveSwingMagicTriggerHand(player, fallbackHand);
        return isSupportedAttackTriggeredItem(player, resolvedFallbackHand)
                ? resolvedFallbackHand
                : preferredHand;
    }

    private static boolean isSupportedAttackTriggeredItem(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem) {
            return swingTriggeredMagicItem.canTriggerSpellOnSwing(player, hand);
        }
        return stack.getItem() instanceof MultipurposeStaffrifle;
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
