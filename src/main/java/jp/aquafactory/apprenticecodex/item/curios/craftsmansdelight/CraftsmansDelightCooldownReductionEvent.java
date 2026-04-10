package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class CraftsmansDelightCooldownReductionEvent {
    private CraftsmansDelightCooldownReductionEvent() {
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!CraftsmansDelight.isEquippedBy(player)) {
            return;
        }
        if (!(event.getSpell() instanceof ICraftsmansDelightAffectedSpell affectedSpell)
                || !affectedSpell.isCraftsmansDelightCooldownReductionEnabled()) {
            return;
        }

        // GUI プレビュー系は entity == null で魔法情報を読むことがあるため、実際にクールダウンを付与する経路だけで反映する。
        event.setEffectiveCooldown(CraftsmansDelight.getReducedEffectiveCooldown(event.getSpell(), player, event.getCastSource()));
    }
}
