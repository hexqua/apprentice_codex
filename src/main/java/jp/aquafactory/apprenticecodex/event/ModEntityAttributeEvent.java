package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.entity.spelldispenser.SpellDispenserAnchorEntity;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import jp.aquafactory.apprenticecodex.spell.searchbeacon.SearchBeaconEntity;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostTotemEntity;
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
        event.put(EntityRegistry.FIELD_OVERSEER_STAFF.get(), FieldOverseerStaffEntity.createAttributes().build());
        event.put(EntityRegistry.TOTEM_OF_PERMAFROST_TOTEM.get(), TotemOfPermafrostTotemEntity.createAttributes().build());
        event.put(EntityRegistry.COMPANION_TRUNK.get(), CompanionTrunkEntity.createAttributes().build());
        event.put(EntityRegistry.HEALING_BLOOM.get(), HealingBloomEntity.createAttributes().build());
        event.put(EntityRegistry.SEARCH_BEACON.get(), SearchBeaconEntity.createAttributes().build());
        event.put(EntityRegistry.SPELL_DISPENSER_ANCHOR.get(), SpellDispenserAnchorEntity.createAttributes().build());
        event.put(EntityRegistry.REMOTE_OWNER_CAST_ANCHOR.get(), RemoteOwnerCastAnchorEntity.createAttributes().build());
    }
}
