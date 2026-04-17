package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ElementalBowCastEvent {
    private ElementalBowCastEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof ElementalBow) || !ElementalBow.isElementalSpell(event.getSpell())) {
            return;
        }

        // bow の持ち替えで踏み倒せると調整意図が崩れるため、overheat は player+school 単位で持つ。
        // cooldown event では値だけ退避し、実際の overheat 更新は発射成功後に bow 本体で確定する。
        ElementalBowOverheatManager.storePendingCooldown(player, ElementalBow.getConfiguredSchoolId(castingItem), event.getEffectiveCooldown());
        event.setEffectiveCooldown(0);
    }
}
