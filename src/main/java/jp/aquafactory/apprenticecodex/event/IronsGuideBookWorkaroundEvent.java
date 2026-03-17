package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.patchouli.IronsGuideBookWorkaround;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class IronsGuideBookWorkaroundEvent {
    private IronsGuideBookWorkaroundEvent() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        var craftedStack = event.getCrafting();
        if (!IronsGuideBookWorkaround.isUnboundPatchouliGuideBook(craftedStack)) {
            return;
        }

        if (!IronsGuideBookWorkaround.matchesOriginalIronsGuideBookRecipe(event.getInventory())) {
            return;
        }

        IronsGuideBookWorkaround.bindToIronsGuideBook(craftedStack);
    }
}
