package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandSpellSelectionRescueCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class BetterCombatOffhandAttributeRescueEvent {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    private BetterCombatOffhandAttributeRescueEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return;
        }

        BetterCombatOffhandAttributeRescueCompat.sync(serverPlayer);
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
    public static void onSpellSelection(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.SpellSelectionEvent event) {
        if (!ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return;
        }

        BetterCombatOffhandSpellSelectionRescueCompat.appendSelectionIfNeeded(event);
    }
}
