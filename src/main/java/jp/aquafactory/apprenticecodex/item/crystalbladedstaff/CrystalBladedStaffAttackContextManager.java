package jp.aquafactory.apprenticecodex.item.crystalbladedstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CrystalBladedStaffAttackContextManager {
    private static final Map<ServerLevel, Map<UUID, PendingAttackContext>> PENDING_ATTACKS = new WeakHashMap<>();

    private CrystalBladedStaffAttackContextManager() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getAmount() <= 0f) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        var attacker = resolveStaffAttacker(event);
        if (attacker == null || !CrystalBladedStaff.isFullyChargedAttack(attacker)) {
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

        var attacksByPlayer = PENDING_ATTACKS.get(serverLevel);
        if (attacksByPlayer == null || attacksByPlayer.isEmpty()) {
            return;
        }

        var gameTime = serverLevel.getGameTime();
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

    private static ServerPlayer resolveStaffAttacker(LivingDamageEvent event) {
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

        if (CrystalBladedStaff.isCrystalBladedStaff(player.getMainHandItem())) {
            return player;
        }

        return null;
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
}
