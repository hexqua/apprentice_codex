package jp.aquafactory.apprenticecodex.item.crystalbladedstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRingAttackTrigger;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CrystalBladedStaffAttackContextManager {
    private static final Map<ServerLevel, Map<UUID, PendingAttackContext>> PENDING_ATTACKS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<HitKey, PendingMissTrigger>> PENDING_MISS_TRIGGERS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<HitKey, Long>> RECENT_HIT_TICKS = new WeakHashMap<>();
    private static final long HIT_MEMORY_TICKS = 1L;

    private CrystalBladedStaffAttackContextManager() {
    }

    public static boolean requestMissTrigger(ServerPlayer player, InteractionHand hand, boolean bypassChargeCheck) {
        return requestMissTrigger(player, hand, bypassChargeCheck, 1);
    }

    public static boolean requestMissTrigger(
            ServerPlayer player,
            InteractionHand hand,
            boolean bypassChargeCheck,
            int evaluationDelayTicks
    ) {
        return requestMissTrigger(player, hand, bypassChargeCheck, evaluationDelayTicks, List.of());
    }

    public static boolean requestMissTrigger(
            ServerPlayer player,
            InteractionHand hand,
            boolean bypassChargeCheck,
            int evaluationDelayTicks,
            List<BlockTargetData> ringTargets
    ) {
        if (player == null || player.isSpectator()) {
            return false;
        }

        var stack = player.getItemInHand(hand);
        if (!CrystalBladedStaff.isCrystalBladedStaff(stack)
                || (!bypassChargeCheck && !CrystalBladedStaff.isFullyChargedAttack(player))) {
            return false;
        }

        var level = player.serverLevel();
        var triggersByPlayerAndHand = PENDING_MISS_TRIGGERS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        triggersByPlayerAndHand.put(new HitKey(player.getUUID(), hand), new PendingMissTrigger(
                player,
                level.getGameTime(),
                hand,
                stack,
                bypassChargeCheck,
                Math.max(1, evaluationDelayTicks),
                List.copyOf(ringTargets)
        ));
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        var player = event.getEntity();
        if (player.level().isClientSide
                || event.isCanceled()
                || !(player instanceof ServerPlayer attacker)
                || !(event.getTarget() instanceof LivingEntity)) {
            return;
        }

        recordRecentCrystalBladedStaffHit(attacker, InteractionHand.MAIN_HAND);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getAmount() <= 0f) {
            return;
        }

        var attacker = resolveDirectPlayerAttack(event);
        if (attacker == null) {
            return;
        }

        recordRecentCrystalBladedStaffHit(attacker, InteractionHand.MAIN_HAND);

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!CrystalBladedStaff.isCrystalBladedStaff(attacker.getMainHandItem())
                || !CrystalBladedStaff.isFullyChargedAttack(attacker)) {
            return;
        }

        var level = attacker.serverLevel();
        var gameTime = level.getGameTime();
        var attacksByPlayer = PENDING_ATTACKS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        var attackContext = attacksByPlayer.get(attacker.getUUID());
        if (attackContext == null || attackContext.gameTime != gameTime) {
            attackContext = new PendingAttackContext(gameTime);
            attacksByPlayer.put(attacker.getUUID(), attackContext);
        }

        attackContext.recordHit(mob.getUUID(), mob.getBoundingBox().getCenter());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        var gameTime = serverLevel.getGameTime();
        var attacksByPlayer = PENDING_ATTACKS.get(serverLevel);
        if (attacksByPlayer != null && !attacksByPlayer.isEmpty()) {
            var iterator = attacksByPlayer.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                var attackContext = entry.getValue();

                if (attackContext.gameTime > gameTime) {
                    continue;
                }

                processAttack(serverLevel, entry.getKey(), attackContext);
                iterator.remove();
            }

            if (attacksByPlayer.isEmpty()) {
                PENDING_ATTACKS.remove(serverLevel);
            }
        }

        processMissTriggers(serverLevel, gameTime);
        cleanupRecentHits(serverLevel, gameTime);
    }

    private static void processAttack(ServerLevel serverLevel, UUID playerUuid, PendingAttackContext attackContext) {
        if (attackContext.targetsByUuid.isEmpty()) {
            return;
        }

        var player = serverLevel.getServer().getPlayerList().getPlayer(playerUuid);
        if (player == null || player.serverLevel() != serverLevel) {
            return;
        }

        var totalHitMobCount = attackContext.targetsByUuid.size();
        for (var impactPosition : attackContext.targetsByUuid.values()) {
            // スイープ巻き込みは Item#hurtEnemy を通らないため、攻撃 tick 終端でまとめて演出と回復を確定する。
            CrystalBladedStaff.spawnManaSiphonOrbs(player, impactPosition, totalHitMobCount);
        }
    }

    private static void processMissTriggers(ServerLevel serverLevel, long gameTime) {
        var triggersByPlayerAndHand = PENDING_MISS_TRIGGERS.get(serverLevel);
        if (triggersByPlayerAndHand == null || triggersByPlayerAndHand.isEmpty()) {
            return;
        }

        var iterator = triggersByPlayerAndHand.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var trigger = entry.getValue();

            if (gameTime < trigger.evaluationGameTime()) {
                continue;
            }

            var triggerKey = entry.getKey();
            var player = serverLevel.getServer().getPlayerList().getPlayer(triggerKey.playerUuid());
            if (player == null) {
                player = trigger.player();
            }
            if (player != null
                    && player.serverLevel() == serverLevel) {
                var hit = hasRecentHit(serverLevel, triggerKey.playerUuid(), trigger.hand(), trigger.requestGameTime());
                triggerPendingAttack(player, trigger, hit);
            }
            iterator.remove();
        }

        if (triggersByPlayerAndHand.isEmpty()) {
            PENDING_MISS_TRIGGERS.remove(serverLevel);
        }
    }

    private static void triggerPendingAttack(ServerPlayer player, PendingMissTrigger trigger, boolean hit) {
        var stack = player.getItemInHand(trigger.hand());
        // 遅延中に持ち替えた場合は不発にする。SpellDataを固定して別経路で詠唱すると契約が広がるため、
        // 1-2tickのエッジケースには、振った同一スタックが手に残っている場合だけ通常経路へ渡す。
        if (stack != trigger.stack()) {
            return;
        }

        if (!CrystalBladedStaff.isCrystalBladedStaff(stack)) {
            return;
        }

        if (!hit
                && stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem
                && swingTriggeredMagicItem.canTriggerSpellOnSwing(player, trigger.hand())) {
            if (swingTriggeredMagicItem.tryTriggerSpellOnSwing(player, trigger.hand(), trigger.bypassChargeCheck())) {
                return;
            }
        }

        // 命中時は杖魔法を抑止し、空振り時は杖魔法が開始できなかった場合だけ指輪へフォールバックする。
        AttackcastRingAttackTrigger.tryTriggerEquippedRings(player, trigger.ringTargets());
    }

    public static void recordRecentCrystalBladedStaffHit(ServerPlayer attacker, InteractionHand hand) {
        var level = attacker.serverLevel();
        var gameTime = level.getGameTime();
        var hitsByPlayerAndHand = RECENT_HIT_TICKS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        recordRecentHitIfCrystalBladedStaff(
                hitsByPlayerAndHand,
                attacker.getUUID(),
                hand,
                attacker.getItemInHand(hand),
                gameTime
        );
    }

    private static void recordRecentHitIfCrystalBladedStaff(
            Map<HitKey, Long> hitsByPlayerAndHand,
            UUID playerUuid,
            InteractionHand hand,
            ItemStack stack,
            long gameTime
    ) {
        if (CrystalBladedStaff.isCrystalBladedStaff(stack)) {
            hitsByPlayerAndHand.put(new HitKey(playerUuid, hand), gameTime);
        }
    }

    private static boolean hasRecentHit(ServerLevel serverLevel, UUID playerUuid, InteractionHand hand, long requestGameTime) {
        var hitsByPlayerAndHand = RECENT_HIT_TICKS.get(serverLevel);
        if (hitsByPlayerAndHand == null) {
            return false;
        }

        var hitGameTime = hitsByPlayerAndHand.get(new HitKey(playerUuid, hand));
        return hitGameTime != null && hitGameTime >= requestGameTime - HIT_MEMORY_TICKS;
    }

    private static void cleanupRecentHits(ServerLevel serverLevel, long gameTime) {
        var hitsByPlayerAndHand = RECENT_HIT_TICKS.get(serverLevel);
        if (hitsByPlayerAndHand == null || hitsByPlayerAndHand.isEmpty()) {
            return;
        }

        hitsByPlayerAndHand.values().removeIf(hitGameTime -> gameTime - hitGameTime > HIT_MEMORY_TICKS + 2L);
        if (hitsByPlayerAndHand.isEmpty()) {
            RECENT_HIT_TICKS.remove(serverLevel);
        }
    }

    private static ServerPlayer resolveDirectPlayerAttack(LivingDamageEvent event) {
        ServerPlayer player = null;
        if (event.getSource().getDirectEntity() instanceof ServerPlayer directPlayer) {
            player = directPlayer;
        } else if (event.getSource().getEntity() instanceof ServerPlayer sourcePlayer) {
            player = sourcePlayer;
        }

        if (player == null) {
            return null;
        }

        if (event.getSource().getDirectEntity() != player) {
            return null;
        }

        if (!(event.getSource().is(DamageTypes.PLAYER_ATTACK) || "player".equals(event.getSource().getMsgId()))) {
            return null;
        }

        return player;
    }

    private static final class PendingAttackContext {
        private final long gameTime;
        private final Map<UUID, Vec3> targetsByUuid = new LinkedHashMap<>();

        private PendingAttackContext(long gameTime) {
            this.gameTime = gameTime;
        }

        private void recordHit(UUID targetUuid, Vec3 impactPosition) {
            targetsByUuid.putIfAbsent(targetUuid, impactPosition);
        }
    }

    private record PendingMissTrigger(
            ServerPlayer player,
            long requestGameTime,
            InteractionHand hand,
            ItemStack stack,
            boolean bypassChargeCheck,
            int evaluationDelayTicks,
            List<BlockTargetData> ringTargets
    ) {
        private long evaluationGameTime() {
            return requestGameTime + evaluationDelayTicks;
        }
    }

    private record HitKey(UUID playerUuid, InteractionHand hand) {
    }
}
