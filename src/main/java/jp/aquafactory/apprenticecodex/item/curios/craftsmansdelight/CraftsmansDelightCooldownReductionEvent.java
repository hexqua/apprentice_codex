package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
        event.setEffectiveCooldown(WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                event.getSpell(),
                player,
                event.getCastSource(),
                castingItem
        ));
    }
}
