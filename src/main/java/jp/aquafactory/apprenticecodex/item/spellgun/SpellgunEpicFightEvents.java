package jp.aquafactory.apprenticecodex.item.spellgun;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellgunCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellgunEpicFightEvents {
    private SpellgunEpicFightEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!ModList.get().isLoaded(EpicFightSpellgunCompat.MOD_ID)) {
            return;
        }

        EpicFightSpellgunCompat.install(player);
        EpicFightSpellgunCompat.tick(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ModList.get().isLoaded(EpicFightSpellgunCompat.MOD_ID)) {
            EpicFightSpellgunCompat.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player
                && ModList.get().isLoaded(EpicFightSpellgunCompat.MOD_ID)) {
            EpicFightSpellgunCompat.clear(player);
        }
    }
}
