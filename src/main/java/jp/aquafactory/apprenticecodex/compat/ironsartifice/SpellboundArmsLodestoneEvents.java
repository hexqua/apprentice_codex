package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.ironsspellbooks.damage.DamageSources;
import jp.aquafactory.apprenticecodex.item.modifier.spellboundarmsmodule.SpellboundArmsModuleModifier;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SpellboundArmsLodestoneEvents {
    private SpellboundArmsLodestoneEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !IronsArtificeCompat.defersSpellResistance(event.getSource())
                || !(event.getSource().getDirectEntity() instanceof Bullet bullet)
                || bullet.getProfile().peek(SpellboundArmsModuleModifier.MODULE_COUNT) <= 0) return;
        // LodestoneはIS_MAGICを元damageから再計算するため、この経路だけ再計算後に一度適用する。
        event.setAmount(event.getAmount() * DamageSources.getResist(
                event.getEntity(), bullet.getProfile().peek(SpellboundArmsModuleEvents.POWER).school()));
    }
}
