package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributeEvent {
    private ModEntityAttributeEvent() {
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.AUTO_TURRET.get(), AutoTurretEntity.createAttributes().build());
        event.put(EntityRegistry.COMPANION_TRUNK.get(), CompanionTrunkEntity.createAttributes().build());
        event.put(EntityRegistry.HEALING_BLOOM.get(), HealingBloomEntity.createAttributes().build());
    }
}
