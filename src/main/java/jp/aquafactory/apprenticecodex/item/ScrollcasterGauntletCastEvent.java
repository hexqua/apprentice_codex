package jp.aquafactory.apprenticecodex.item;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletFreecastContext;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ScrollcasterGauntletCastEvent {
    private ScrollcasterGauntletCastEvent() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (event.getCastSource() != CastSource.SWORD || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var castingItem = magicData.getPlayerCastingItem();
        if (!(castingItem.getItem() instanceof ScrollcasterGauntlet gauntlet)) {
            return;
        }

        var effectiveCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource(),
                castingItem
        );
        if (ScrollcasterGauntletFreecastContext.matches(player.getUUID(), castingItem, event.getSpell())) {
            effectiveCooldown = gauntlet.resolveFreecastSwingCooldownTicks(
                    player,
                    castingItem,
                    event.getSpell(),
                    effectiveCooldown
            );
        }
        event.setEffectiveCooldown(effectiveCooldown);
    }
}
