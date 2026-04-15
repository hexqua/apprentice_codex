package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilImbueBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ArcaneAnvilImbueBlockTooltipEvent {
    private static final String CAN_BE_IMBUED_FRAME_KEY = "tooltip.irons_spellbooks.can_be_imbued_frame";

    private ArcaneAnvilImbueBlockTooltipEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ArcaneAnvilImbueBlockItem)) {
            return;
        }

        removeTooltipLine(event.getToolTip(), CAN_BE_IMBUED_FRAME_KEY);
    }

    private static void removeTooltipLine(java.util.List<Component> tooltip, String translationKey) {
        for (var i = 0; i < tooltip.size(); i++) {
            if (!(tooltip.get(i).getContents() instanceof TranslatableContents translatableContents)) {
                continue;
            }

            if (!translationKey.equals(translatableContents.getKey())) {
                continue;
            }

            tooltip.remove(i);
            return;
        }
    }
}
