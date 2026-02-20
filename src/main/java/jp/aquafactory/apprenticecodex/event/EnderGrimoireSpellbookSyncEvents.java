package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookSync;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoire;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EnderGrimoireSpellbookSyncEvents {
    private EnderGrimoireSpellbookSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            EnderGrimoireSpellbookSync.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            EnderGrimoireSpellbookSync.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            EnderGrimoireSpellbookSync.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onCurioChanged(CurioChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!Curios.SPELLBOOK_SLOT.equals(event.getIdentifier())) {
            return;
        }

        // EnderGrimoireはISpellContainerを直接持たないため、装備変更時に明示同期して選択情報を更新する.
        if (event.getFrom().getItem() instanceof EnderGrimoire || event.getTo().getItem() instanceof EnderGrimoire) {
            EnderGrimoireSpellbookSync.syncToClient(serverPlayer);
        }
    }
}
