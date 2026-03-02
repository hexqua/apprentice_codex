package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
