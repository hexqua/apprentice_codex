package jp.aquafactory.apprenticecodex.item.manaforceblade;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightManaForceBladeCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaForceBladeEpicFightEvents {
    private ManaForceBladeEpicFightEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ModList.get().isLoaded(EpicFightManaForceBladeCompat.MOD_ID)) {
            return;
        }

        EpicFightManaForceBladeCompat.install(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ModList.get().isLoaded(EpicFightManaForceBladeCompat.MOD_ID)) {
            EpicFightManaForceBladeCompat.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player
                && ModList.get().isLoaded(EpicFightManaForceBladeCompat.MOD_ID)) {
            EpicFightManaForceBladeCompat.clear(player);
        }
    }
}
