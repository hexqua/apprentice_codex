package jp.aquafactory.apprenticecodex.compat.epicfight;

import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.AttackPhaseEndEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EpicFightSwingMagicCompat {
    public static final String MOD_ID = "epicfight";

    private static final UUID SWING_MAGIC_EVENT_UUID = UUID.fromString("c3e82f78-5e2e-41b7-86af-f38c7adcb8bd");
    private static final Set<UUID> INSTALLED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<TriggerKey, Long> LAST_TRIGGERED_TICKS = new ConcurrentHashMap<>();

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
            INSTALLED_PLAYERS.add(player.getUUID());
        });
    }

    public static void clear(ServerPlayer player) {
        var playerId = player.getUUID();
        INSTALLED_PLAYERS.remove(playerId);
        LAST_TRIGGERED_TICKS.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private static void onAttackPhaseEnd(AttackPhaseEndEvent event) {
        if (event.getPhaseOrder() != 0) {
            return;
        }

        var playerpatch = event.getPlayerPatch();
        var player = playerpatch.getOriginal();
        var hand = resolveAttackHand(event);
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof AbstractSwingMagicItem swingMagicItem)) {
            return;
        }

        var triggerKey = new TriggerKey(player.getUUID(), hand);
        var gameTime = player.level().getGameTime();
        var lastTriggeredTick = LAST_TRIGGERED_TICKS.put(triggerKey, gameTime);
        if (lastTriggeredTick != null && lastTriggeredTick == gameTime) {
            return;
        }

        swingMagicItem.tryTriggerImbuedSpellOnSwing(player, hand, true);
    }

    private static InteractionHand resolveAttackHand(AttackPhaseEndEvent event) {
        var phase = event.getPhase();
        if (phase == null) {
            return InteractionHand.MAIN_HAND;
        }

        var hand = phase.getHand();
        return hand != null ? hand : InteractionHand.MAIN_HAND;
    }

    private record TriggerKey(UUID playerId, InteractionHand hand) {
    }
}
