package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastType;
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
        reserveOverheatCast(player, spellId, manaCost, originalMana, overheatTicks, false);
    }

    public static void reserveOverheatCast(ServerPlayer player, String spellId, int manaCost, float originalMana,
                                           int overheatTicks, boolean continuous) {
        PENDING_OVERHEAT_CASTS.put(player.getUUID(), new PendingOverheatCast(
                spellId,
                Math.max(0, manaCost),
                Math.max(0, overheatTicks),
                continuous,
                false
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

        var pendingCast = PENDING_OVERHEAT_CASTS.get(player.getUUID());
        if (pendingCast == null || !pendingCast.spellId().equals(event.getSpellId())) {
            PENDING_OVERHEAT_CASTS.remove(player.getUUID());
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !(magicData.getPlayerCastingItem().getItem() instanceof CircuitHeatStaff)) {
            PENDING_OVERHEAT_CASTS.remove(player.getUUID());
            return;
        }

        event.setManaCost(pendingCast.manaCost());
        if (pendingCast.shouldOverheat(magicData.getMana())) {
            CircuitHeatStaff.startStaffOverheat(magicData.getPlayerCastingItem(), player.level(), pendingCast.overheatTicks());
            pendingCast.markOverheated();
        }

        if (!pendingCast.continuous() || magicData.getCastType() != CastType.CONTINUOUS) {
            PENDING_OVERHEAT_CASTS.remove(player.getUUID());
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

    private static final class PendingOverheatCast {
        private final String spellId;
        private final int manaCost;
        private final int overheatTicks;
        private final boolean continuous;
        private boolean overheated;

        private PendingOverheatCast(String spellId, int manaCost, int overheatTicks, boolean continuous, boolean overheated) {
            this.spellId = spellId;
            this.manaCost = manaCost;
            this.overheatTicks = overheatTicks;
            this.continuous = continuous;
            this.overheated = overheated;
        }

        private String spellId() {
            return spellId;
        }

        private int manaCost() {
            return manaCost;
        }

        private int overheatTicks() {
            return overheatTicks;
        }

        private boolean continuous() {
            return continuous;
        }

        private boolean shouldOverheat(float currentMana) {
            return !overheated && manaCost > 0 && Math.max(0.0F, currentMana) - manaCost <= 0.0F && overheatTicks > 0;
        }

        private void markOverheated() {
            overheated = true;
        }
    }
}
