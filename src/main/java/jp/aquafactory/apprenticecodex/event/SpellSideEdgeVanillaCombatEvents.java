package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeMirror;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellSideEdgeVanillaCombatEvents {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";
    private static final String EPIC_FIGHT_MOD_ID = "epicfight";
    private static final long PENDING_ATTACK_LIFETIME_TICKS = 1L;
    private static final Map<UUID, PendingAttack> PENDING_ATTACKS = new ConcurrentHashMap<>();

    private SpellSideEdgeVanillaCombatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity target)
                || !canUseVanillaIframeBypass(player)) {
            return;
        }

        PENDING_ATTACKS.put(player.getUUID(), new PendingAttack(
                target.getUUID(),
                player.serverLevel().dimension(),
                player.serverLevel().getGameTime()
        ));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }

        var player = resolveDirectPlayerAttack(event.getSource());
        if (player == null || !canUseVanillaIframeBypass(player)) {
            return;
        }

        var pending = PENDING_ATTACKS.get(player.getUUID());
        if (pending == null) {
            return;
        }
        if (pending.isExpired(player.serverLevel())) {
            PENDING_ATTACKS.remove(player.getUUID(), pending);
            return;
        }
        if (!pending.matches(event.getEntity(), player.serverLevel())) {
            return;
        }

        PENDING_ATTACKS.remove(player.getUUID(), pending);
        event.getEntity().invulnerableTime = 0;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        var dimension = serverLevel.dimension();
        var expireBefore = serverLevel.getGameTime() - PENDING_ATTACK_LIFETIME_TICKS;
        PENDING_ATTACKS.entrySet().removeIf(entry ->
                entry.getValue().dimension().equals(dimension) && entry.getValue().gameTime() < expireBefore);
    }

    private static boolean canUseVanillaIframeBypass(ServerPlayer player) {
        return !isCombatOverhaulLoaded()
                && SpellSideEdge.isSpellSideEdge(player.getMainHandItem())
                && SpellSideEdgeMirror.isSpellSideEdgeMirror(player.getOffhandItem());
    }

    private static boolean isCombatOverhaulLoaded() {
        return ModList.get().isLoaded(BETTER_COMBAT_MOD_ID) || ModList.get().isLoaded(EPIC_FIGHT_MOD_ID);
    }

    private static ServerPlayer resolveDirectPlayerAttack(DamageSource source) {
        if (!(source.getDirectEntity() instanceof ServerPlayer player)) {
            return null;
        }
        if (!(source.is(DamageTypes.PLAYER_ATTACK) || "player".equals(source.getMsgId()))) {
            return null;
        }
        return player;
    }

    private record PendingAttack(
            UUID targetUuid,
            ResourceKey<Level> dimension,
            long gameTime
    ) {
        private boolean matches(LivingEntity target, ServerLevel level) {
            return target.getUUID().equals(targetUuid)
                    && level.dimension().equals(dimension)
                    && !isExpired(level);
        }

        private boolean isExpired(ServerLevel level) {
            var tickDelta = level.getGameTime() - gameTime;
            return !level.dimension().equals(dimension)
                    || tickDelta < 0L
                    || tickDelta > PENDING_ATTACK_LIFETIME_TICKS;
        }
    }
}
