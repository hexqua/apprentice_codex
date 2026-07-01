package jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MithrilFreecastStaffCastEvent {
    private MithrilFreecastStaffCastEvent() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();

        var cooldownSource = MithrilFreecastStaffCastContext.consumeCooldownSource(
                player.getUUID(),
                castingItem,
                event.getSpell()
        );
        if (cooldownSource.isEmpty()) {
            return;
        }

        var source = cooldownSource.get();
        if (!(source.item() instanceof MithrilFreecastStaff freecastStaff)) {
            return;
        }
        event.setEffectiveCooldown(
                freecastStaff.resolveSwingTriggeredCooldownTicks(
                        player,
                        event.getSpell(),
                        source.castSource()
                )
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearResolvedCooldownSource(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
        MithrilFreecastStaffCastContext.clearResolvedCooldownSource(
                player.getUUID(),
                castingItem,
                event.getSpell()
        );
    }
}
