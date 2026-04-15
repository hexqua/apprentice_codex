package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CraftsmansDelightManaCostDiscountEvent {
    private CraftsmansDelightManaCostDiscountEvent() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!CraftsmansDelightSpellSupport.isManaCostDiscountTarget(event.getSpellId())) {
            return;
        }

        event.setManaCost(CraftsmansDelight.applyManaCostDiscount(event.getManaCost(), player));
    }
}

