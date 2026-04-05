package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
        if (!(castingItem.getItem() instanceof AbstractSwingcastStaffItem swingcastStaffItem)) {
            return;
        }

        if (!SwingcastStaffCastContext.matches(player.getUUID(), castingItem, event.getSpell())) {
            return;
        }

        event.setEffectiveCooldown(
                swingcastStaffItem.resolveSwingcastCooldownTicks(player, castingItem, event.getSpell(), event.getEffectiveCooldown())
        );
    }
}
