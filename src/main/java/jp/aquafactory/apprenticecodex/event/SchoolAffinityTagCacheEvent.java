package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SchoolAffinityTagCacheEvent {
    private SchoolAffinityTagCacheEvent() {
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) {
            return;
        }

        SchoolAffinityRegistry.invalidateBindings();
    }
}
