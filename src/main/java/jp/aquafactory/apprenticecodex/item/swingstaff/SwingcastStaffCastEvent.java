package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.revolvercaststaff.RevolvercastStaff;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SwingcastStaffCastEvent {
    private SwingcastStaffCastEvent() {
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
        if (!SwingcastStaffCastContext.matches(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }

        if (castingItem.getItem() instanceof AbstractSwingcastStaffItem swingcastStaffItem) {
            event.setEffectiveCooldown(
                    swingcastStaffItem.resolveSwingcastCooldownTicks(player, castingItem, event.getSpell(), event.getEffectiveCooldown())
            );
        } else if (castingItem.getItem() instanceof RevolvercastStaff revolvercastStaff) {
            event.setEffectiveCooldown(
                    revolvercastStaff.resolveSwingcastCooldownTicks(player, castingItem, event.getSpell(), event.getEffectiveCooldown())
            );
        }
    }
}
