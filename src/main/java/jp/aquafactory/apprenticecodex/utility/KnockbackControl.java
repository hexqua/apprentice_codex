package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber
public final class KnockbackControl {

    private KnockbackControl() {}

    private static final Map<LivingEntity, Integer> IMMUNE = new WeakHashMap<>();

    public static void markIgnoreNextKnockback(LivingEntity target) {
        if (target == null) return;
        //noinspection resource
        if (target.level().isClientSide) return;
        IMMUNE.put(target, target.tickCount);
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        var entity = event.getEntity();
        var markedTick = IMMUNE.get(entity);
        if (markedTick == null) return;

        if (entity.tickCount - markedTick <= 10) {
            event.setCanceled(true);
        }

        IMMUNE.remove(entity);
    }
}
