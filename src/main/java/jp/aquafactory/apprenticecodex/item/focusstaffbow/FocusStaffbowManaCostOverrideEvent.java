package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FocusStaffbowManaCostOverrideEvent {
    private static final Map<UUID, Integer> RESERVED_MANA_COSTS = new ConcurrentHashMap<>();

    private FocusStaffbowManaCostOverrideEvent() {
    }

    public static void reserveManaCostOverride(ServerPlayer player, int manaCost) {
        RESERVED_MANA_COSTS.put(player.getUUID(), Math.max(0, manaCost));
    }

    public static void clearManaCostOverride(ServerPlayer player) {
        RESERVED_MANA_COSTS.remove(player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getAbilities().instabuild) {
            RESERVED_MANA_COSTS.remove(player.getUUID());
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !(magicData.getPlayerCastingItem().getItem() instanceof FocusStaffbow)) {
            RESERVED_MANA_COSTS.remove(player.getUUID());
            return;
        }

        var reservedManaCost = RESERVED_MANA_COSTS.remove(player.getUUID());
        if (reservedManaCost != null) {
            event.setManaCost(reservedManaCost);
        }
    }
}
