package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ServantGazeEvents {
    private ServantGazeEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 保存しない召喚のため、server停止などで残ったrecastは復元せず終了させる。
            ServantGazeManager.cancel(player, RecastResult.USER_CANCEL);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ServantGazeManager.relocate(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServantGazeManager.cancel(player, RecastResult.USER_CANCEL);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServantGazeManager.cancel(player, RecastResult.DEATH);
        }
    }
}
