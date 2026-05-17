package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
