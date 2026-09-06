package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MalumScytheThrowDamageHandler {
    private MalumScytheThrowDamageHandler() {}

    @SubscribeEvent
    public static void damage(LivingDamageEvent.Pre event) {
        var source = event.getSource();
        if (event.getNewDamage() <= 0 || !(source.getEntity() instanceof LivingEntity owner)) return;
        if (!source.is(DamageTypes.SPELL_REAPER_SCYTHE_THROW)
                && !source.is(DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS)) return;
        // is_scytheへの登録はGeas/Cullingまで起動する。共通属性だけを本家と同じPre段階で適用する。
        event.setNewDamage(event.getNewDamage() * MalumSpellReaperScytheBridge.scytheProficiency(owner));
    }
}
