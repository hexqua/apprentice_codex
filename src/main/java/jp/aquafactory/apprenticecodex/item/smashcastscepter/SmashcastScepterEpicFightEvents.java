package jp.aquafactory.apprenticecodex.item.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSmashcastScepterCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SmashcastScepterEpicFightEvents {
    private SmashcastScepterEpicFightEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
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
