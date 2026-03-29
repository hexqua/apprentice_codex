package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributeEvent {
    private ModEntityAttributeEvent() {
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.AUTO_TURRET.get(), AutoTurretEntity.createAttributes().build());
    }
}
