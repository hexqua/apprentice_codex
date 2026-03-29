package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class ModEntityAttributeEvent {
    private ModEntityAttributeEvent() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModEntityAttributeEvent::onEntityAttributeCreation);
    }

    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.AUTO_TURRET.get(), AutoTurretEntity.createAttributes().build());
    }
}
