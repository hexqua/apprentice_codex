package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ProtectionSpellSupporterManaCostDiscountEvent {
    private ProtectionSpellSupporterManaCostDiscountEvent() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ProtectionSpellSupporter.isManaCostDiscountTargetSpell(event.getSpellId())) {
            return;
        }

        event.setManaCost(ProtectionSpellSupporter.applyManaCostDiscount(event.getManaCost(), player));
    }
}
