package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CraftsmansDelightCooldownReductionEvent {
    private CraftsmansDelightCooldownReductionEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!CraftsmansDelight.isEquippedBy(player)) {
            return;
        }
        if (!CraftsmansDelightSpellSupport.isCooldownReductionTarget(event.getSpell())) {
            return;
        }

        // GUI プレビュー系は entity == null で魔法情報を読むことがあるため、実際にクールダウンを付与する経路だけで反映する。
        var magicData = MagicData.getPlayerMagicData(player);
        var castingItem = magicData == null ? ItemStack.EMPTY : magicData.getPlayerCastingItem();
        var cooldownSource = MithrilFreecastStaffCastContext.resolveCooldownSource(
                player.getUUID(),
                castingItem,
                event.getSpell()
        );
        var castSource = cooldownSource
                .map(MithrilFreecastStaffCastContext.CooldownSource::castSource)
                .orElse(event.getCastSource());
        var cooldownStack = cooldownSource
                .map(MithrilFreecastStaffCastContext.CooldownSource::stack)
                .orElse(castingItem);
        event.setEffectiveCooldown(WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                castSource,
                cooldownStack
        ));
    }
}
