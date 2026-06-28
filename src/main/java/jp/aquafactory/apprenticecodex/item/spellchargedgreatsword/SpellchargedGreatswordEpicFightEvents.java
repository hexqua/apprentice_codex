package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellchargedGreatswordEpicFightEvents {
    private SpellchargedGreatswordEpicFightEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!ModList.get().isLoaded(EpicFightSpellchargedGreatswordCompat.MOD_ID)) {
            return;
        }

        EpicFightSpellchargedGreatswordCompat.tick(player);
    }
}
