package jp.aquafactory.apprenticecodex.item.elementalbow;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ElementalBowCastEvents {
    private ElementalBowCastEvents() {}

    // 通常詠唱の同期より先に弓の状態を送り、途中から追跡した観測者にもアニメーション抑止を適用する。
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer player) {
            ElementalBowPendingCast.syncToObserver(player, observer);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof ServerPlayer player) ElementalBowPendingCast.tick(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ElementalBowPendingCast.cancel(player);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ElementalBowPendingCast.cancel(player);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ElementalBowPendingCast.cancel(player);
    }
}
