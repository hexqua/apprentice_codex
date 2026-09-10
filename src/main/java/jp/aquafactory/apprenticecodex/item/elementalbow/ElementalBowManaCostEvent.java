package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ElementalBowManaCostEvent {
    private ElementalBowManaCostEvent() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        var spell = SpellRegistry.getSpell(event.getSpellId());
        if (!ElementalBowCasting.isActive(event.getEntity(), spell)) return;
        // getManaCost 自体は書き換えず、他のイベントによる割引を残して発射時だけ倍率を適用する。
        event.setManaCost(ElementalBowRunes.scaleMana(event.getManaCost(),
                ElementalBowCasting.manaMultiplier(event.getEntity(), spell)));
    }
}
