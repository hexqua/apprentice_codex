package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MalumScytheThrowDamageHandler {
    private MalumScytheThrowDamageHandler() {}

    @SubscribeEvent
    public static void damage(LivingHurtEvent event) {
        var source = event.getSource();
        if (event.getAmount() <= 0 || !(source.getEntity() instanceof LivingEntity owner)) return;
        if (!source.is(DamageTypes.SPELL_REAPER_SCYTHE_THROW)
                && !source.is(DamageTypes.SPELL_REAPER_SCYTHE_THROW_CONTINUOUS)) return;
        // is_scytheへの登録はGeas/Cullingまで起動する。共通属性だけを本家と同じHurt段階で適用する。
        event.setAmount(event.getAmount() * MalumSpellReaperScytheBridge.scytheProficiency(owner));
    }
}
