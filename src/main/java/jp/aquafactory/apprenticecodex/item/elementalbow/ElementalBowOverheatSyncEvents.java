package jp.aquafactory.apprenticecodex.item.elementalbow;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ElementalBowOverheatSyncEvents {
    private ElementalBowOverheatSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ElementalBowOverheatManager.cleanLegacyData(serverPlayer);
            ElementalBowOverheatSync.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // エンド帰還では Player 自体が再生成される。死亡時だけ過熱をリセットする。
        ElementalBowOverheatManager.applySyncedState(event.getEntity(), event.isWasDeath()
                ? null : ElementalBowOverheatManager.createSyncTag(event.getOriginal()));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ElementalBowOverheatSync.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ElementalBowOverheatSync.syncToClient(serverPlayer);
        }
    }
}
