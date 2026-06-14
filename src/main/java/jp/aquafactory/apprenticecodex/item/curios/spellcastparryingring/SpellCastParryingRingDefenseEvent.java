package jp.aquafactory.apprenticecodex.item.curios.spellcastparryingring;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.forcefield.ForceFieldDefenseEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SpellCastParryingRingDefenseEvent {
    private SpellCastParryingRingDefenseEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        var defender = event.getEntity();
        if (defender.level().isClientSide || event.isCanceled()) {
            return;
        }
        if (!isEquippedBy(defender)) {
            return;
        }

        var source = event.getSource();
        if (!SpellCastParryingRingDefenseLogic.canParry(defender, source)) {
            return;
        }

        event.setCanceled(true);
        discardDirectProjectile(source.getDirectEntity());
        ForceFieldDefenseEvent.spawnAbsorbWallEffect(defender, source);
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.SPELL_CAST_PARRYING_RING.get()))
                .orElse(false);
    }

    private static void discardDirectProjectile(@Nullable net.minecraft.world.entity.Entity directEntity) {
        if (directEntity instanceof Projectile projectile && !projectile.isRemoved()) {
            projectile.discard();
        }
    }
}
