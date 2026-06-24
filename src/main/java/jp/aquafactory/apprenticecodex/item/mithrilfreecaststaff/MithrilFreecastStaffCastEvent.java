package jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MithrilFreecastStaffCastEvent {
    private MithrilFreecastStaffCastEvent() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof MithrilFreecastStaff freecastStaff)) {
            return;
        }

        if (!MithrilFreecastStaffCastContext.matches(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }

        event.setEffectiveCooldown(
                freecastStaff.resolveSwingTriggeredCooldownTicks(player, event.getSpell(), event.getEffectiveCooldown())
        );
    }
}
