package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellReaperScytheCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ScytheEpicFightEvents {
    private ScytheEpicFightEvents() {}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ModList.get().isLoaded("epicfight") && event.player instanceof ServerPlayer player) {
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
