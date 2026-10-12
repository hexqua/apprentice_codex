package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.ironsspellbooks.damage.DamageSources;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleModifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class SpellboundArmsLodestoneEvents {
    private SpellboundArmsLodestoneEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !IronsArtificeCompat.defersSpellResistance(event.getSource())
                || !(event.getSource().getDirectEntity() instanceof Bullet bullet)
                || bullet.getProfile().peek(SpellboundArmsModuleModifier.MODULE_COUNT) <= 0) return;
        // LodestoneはIS_MAGICを元damageから再計算するため、この経路だけ再計算後に一度適用する。
        event.setNewDamage(event.getNewDamage() * DamageSources.getResist(
                event.getEntity(), bullet.getProfile().peek(SpellboundArmsModuleEvents.POWER).school()));
    }
}
