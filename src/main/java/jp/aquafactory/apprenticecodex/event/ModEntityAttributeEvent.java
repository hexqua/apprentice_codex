package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
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
        event.put(EntityRegistry.COMPANION_TRUNK.get(), CompanionTrunkEntity.createAttributes().build());
        event.put(EntityRegistry.HEALING_BLOOM.get(), HealingBloomEntity.createAttributes().build());
        event.put(EntityRegistry.SEARCH_BEACON.get(), SearchBeaconEntity.createAttributes().build());
        event.put(EntityRegistry.SPELL_DISPENSER_ANCHOR.get(), SpellDispenserAnchorEntity.createAttributes().build());
    }
}
