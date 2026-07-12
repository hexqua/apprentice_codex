package jp.aquafactory.apprenticecodex.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber
public final class KnockbackControlEvent {

    private KnockbackControlEvent() {}

    private static final Map<LivingEntity, Integer> IMMUNE = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> IMMUNE_THIS_TICK = new WeakHashMap<>();

    public static void markIgnoreNextKnockback(LivingEntity target) {
        if (target == null) return;
        //noinspection resource
        if (target.level().isClientSide) return;
        IMMUNE.put(target, target.tickCount);
    }

    public static void markIgnoreKnockbackThisTick(LivingEntity target) {
        if (target == null || target.level().isClientSide) {
            return;
        }
        IMMUNE_THIS_TICK.put(target, target.tickCount);
    }

    public static boolean shouldIgnorePushThisTick(LivingEntity target) {
        var markedTick = IMMUNE_THIS_TICK.get(target);
        if (markedTick == null) {
            return false;
        }
        if (markedTick == target.tickCount) {
            return true;
        }
        IMMUNE_THIS_TICK.remove(target);
        return false;
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        var entity = event.getEntity();
        if (shouldIgnorePushThisTick(entity)) {
            event.setCanceled(true);
            return;
        }

        var markedTick = IMMUNE.get(entity);
        if (markedTick == null) return;

        if (entity.tickCount - markedTick <= 10) {
            event.setCanceled(true);
        }

        IMMUNE.remove(entity);
    }
}
