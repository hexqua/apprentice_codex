package jp.aquafactory.apprenticecodex.item.magicitem;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class WoodenWandDurabilityEvent {
    private WoodenWandDurabilityEvent() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Post event) {
        if (event.getCastSource() != CastSource.SWORD
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        var castingStack = magicData.getPlayerCastingItem();
        if (!(castingStack.getItem() instanceof WoodenWand)) {
            return;
        }

        var spellData = WoodenWand.getImbuedSpell(castingStack);
        if (spellData == null || spellData.getSpell() != event.getSpell()) {
            return;
        }

        var usedHand = player.getOffhandItem() == castingStack
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        castingStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(usedHand));
    }
}
