package jp.aquafactory.apprenticecodex.spell.automagnet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AutoMagnetFamiliarEvents {
    private AutoMagnetFamiliarEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.player instanceof ServerPlayer serverPlayer) {
            AutoMagnetFamiliarManager.ensureActive(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 通常 tick では UUID から直接解決し、ライフサイクル境界だけ全ディメンションの重複を整理する。
            AutoMagnetFamiliarManager.reconcileActive(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AutoMagnetFamiliarManager.reconcileActive(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // ログアウト中は実体だけ消して、次回ログインで復帰させる.
            AutoMagnetFamiliarManager.removeOnlyEntity(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AutoMagnetFamiliarManager.deactivate(serverPlayer);
        }
    }
}
