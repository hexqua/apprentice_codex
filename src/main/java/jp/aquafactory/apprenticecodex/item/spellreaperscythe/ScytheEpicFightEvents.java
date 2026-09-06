package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReaperScytheCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ScytheEpicFightEvents {
    private ScytheEpicFightEvents() {}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (ModList.get().isLoaded("epicfight") && event.getEntity() instanceof ServerPlayer player) {
            EpicFightSpellReaperScytheCompat.tick(player);
        }
    }
    private static void clear(net.minecraft.world.entity.player.Player player) {
        if (ModList.get().isLoaded("epicfight") && player instanceof ServerPlayer server) {
            EpicFightSpellReaperScytheCompat.clear(server);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { clear(event.getOriginal()); }
}
