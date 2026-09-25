package jp.aquafactory.apprenticecodex.spell.lockonray;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class LockOnRayEvents {
    private LockOnRayEvents() {}

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { release(event); }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) { release(event); }

    private static void release(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = MagicData.getPlayerMagicData(player);
            if (data.getAdditionalCastData() instanceof LockOnRayCastData) LockOnRay.cancel(player, data);
        }
    }
}
