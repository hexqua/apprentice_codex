package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CircuitHeatStaffCastEvent {
    private static final Map<UUID, PendingOverheatCast> PENDING_OVERHEAT_CASTS = new ConcurrentHashMap<>();

    private CircuitHeatStaffCastEvent() {
    }

    public static void reserveOverheatCast(ServerPlayer player, String spellId, int manaCost, float originalMana, int overheatTicks) {
        PENDING_OVERHEAT_CASTS.put(player.getUUID(), new PendingOverheatCast(
                spellId,
                Math.max(0, manaCost),
                Math.max(0.0F, originalMana),
                Math.max(0, overheatTicks)
        ));
    }

    public static void clearReservedOverheatCast(ServerPlayer player) {
        PENDING_OVERHEAT_CASTS.remove(player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pendingCast = PENDING_OVERHEAT_CASTS.remove(player.getUUID());
        if (pendingCast == null || !pendingCast.spellId().equals(event.getSpellId())) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !(magicData.getPlayerCastingItem().getItem() instanceof CircuitHeatStaff)) {
            return;
        }

        event.setManaCost(pendingCast.manaCost());
        if (pendingCast.shouldOverheat()) {
            CircuitHeatStaff.startStaffOverheat(magicData.getPlayerCastingItem(), player.level(), pendingCast.overheatTicks());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pendingCast = PENDING_OVERHEAT_CASTS.get(player.getUUID());
        if (pendingCast == null) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData != null && magicData.isCasting()) {
            return;
        }

        PENDING_OVERHEAT_CASTS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_OVERHEAT_CASTS.remove(player.getUUID());
        }
    }

    private record PendingOverheatCast(String spellId, int manaCost, float originalMana, int overheatTicks) {
        private boolean shouldOverheat() {
            return manaCost > 0 && originalMana - manaCost <= 0.0F && overheatTicks > 0;
        }
    }
}
