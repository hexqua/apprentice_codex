package jp.aquafactory.apprenticecodex.item.spellchargedgreatsword;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSpellchargedGreatswordCompat;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellchargedGreatswordEpicFightEvents {
    private SpellchargedGreatswordEpicFightEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ModList.get().isLoaded(EpicFightSpellchargedGreatswordCompat.MOD_ID)) {
            return;
        }

        EpicFightSpellchargedGreatswordCompat.tick(player);
    }
}
