package jp.aquafactory.apprenticecodex.spell;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellMovementInputEvents {
    private SpellMovementInputEvents() {
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        clear(event);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpellMovementInput.clear(player);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SpellMovementInput.clearServer();
    }

    private static void clear(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpellMovementInput.clear(player);
        }
    }
}
