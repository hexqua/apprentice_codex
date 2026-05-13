package jp.aquafactory.apprenticecodex.item.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSmashcastScepterCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SmashcastScepterEpicFightEvents {
    private SmashcastScepterEpicFightEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ModList.get().isLoaded(EpicFightSmashcastScepterCompat.MOD_ID)) {
            return;
        }

        EpicFightSmashcastScepterCompat.install(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ModList.get().isLoaded(EpicFightSmashcastScepterCompat.MOD_ID)) {
            EpicFightSmashcastScepterCompat.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player
                && ModList.get().isLoaded(EpicFightSmashcastScepterCompat.MOD_ID)) {
            EpicFightSmashcastScepterCompat.clear(player);
        }
    }
}
