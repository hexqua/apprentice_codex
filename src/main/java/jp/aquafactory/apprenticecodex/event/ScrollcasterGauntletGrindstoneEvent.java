package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ScrollcasterGauntletGrindstoneEvent {
    private ScrollcasterGauntletGrindstoneEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGrindstonePlaceItem(GrindstoneEvent.OnPlaceItem event) {
        if (!isScrollcasterGauntlet(event.getTopItem()) && !isScrollcasterGauntlet(event.getBottomItem())) {
            return;
        }

        event.setCanceled(true);
    }

    private static boolean isScrollcasterGauntlet(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
    }
}
