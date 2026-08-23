package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandSpellSelectionRescueCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class BetterCombatOffhandAttributeRescueEvent {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    private BetterCombatOffhandAttributeRescueEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return;
        }

        BetterCombatOffhandAttributeRescueCompat.sync(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return;
        }

        BetterCombatOffhandAttributeRescueCompat.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return;
        }

        BetterCombatOffhandAttributeRescueCompat.clear(event.getOriginal());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return;
        }

        BetterCombatOffhandSpellSelectionRescueCompat.appendSelectionIfNeeded(event);
    }
}
