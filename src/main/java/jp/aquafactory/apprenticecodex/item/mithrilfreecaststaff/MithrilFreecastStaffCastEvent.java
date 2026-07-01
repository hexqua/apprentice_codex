package jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.RecastCooldownPolicyContext;
import net.minecraft.server.level.ServerPlayer;
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
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof MithrilFreecastStaff freecastStaff)) {
            return;
        }

        if (RecastCooldownPolicyContext.isCompletingRecast(player, event.getSpell())) {
            // Iron's の Recast は発動元 ItemStack を保持しないため、Mithril の選択元 policy も保持しない。
            // 一部ケースだけ有利にならないよう、Recast 完了 cooldown では意図的に policy を読まない。
            return;
        }

        var cooldownSource = MithrilFreecastStaffCastContext.consumeCooldownSource(player.getUUID(), castingItem, event.getSpell());
        if (cooldownSource.isEmpty()) {
            return;
        }

        var source = cooldownSource.get();
        event.setEffectiveCooldown(
                freecastStaff.resolveSwingTriggeredCooldownTicks(
                        player,
                        event.getSpell(),
                        source.castSource(),
                        source.stack()
                )
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearResolvedCooldownSource(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (castingItem.getItem() instanceof MithrilFreecastStaff) {
            MithrilFreecastStaffCastContext.clearResolvedCooldownSource(
                    player.getUUID(),
                    castingItem,
                    event.getSpell()
            );
        }
    }
}
