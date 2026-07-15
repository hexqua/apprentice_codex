package jp.aquafactory.apprenticecodex.item.offhand;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import jp.aquafactory.apprenticecodex.item.RightClickSpellItemHelper;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OffhandMagicItemCastEvent {
    private OffhandMagicItemCastEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        var player = event.getEntity();
        var mainhandStack = player.getMainHandItem();
        if (!(player.getOffhandItem().getItem() instanceof AbstractOffhandMagicItem)) {
            return;
        }

        // メインハンド側に操作優先条件がある場合はオフハンド魔法を割り込ませない。
        if (RightClickSpellItemHelper.hasMainHandRightClickBehavior(player, mainhandStack)) {
            return;
        }

        var castResult = tryCastOffhandSpell(player);
        if (castResult == CastResult.NONE) {
            return;
        }

        event.setCancellationResult(castResult == CastResult.SUCCESS ? InteractionResult.CONSUME : InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static CastResult tryCastOffhandSpell(Player player) {
        var offhandStack = player.getOffhandItem();
        if (!(offhandStack.getItem() instanceof AbstractOffhandMagicItem offhandMagicItem)) {
            return CastResult.NONE;
        }

        if (!ISpellContainer.isSpellContainer(offhandStack)) {
            offhandMagicItem.initializeSpellContainer(offhandStack);
        }

        var spellSelectionManager = new SpellSelectionManager(player);
        var selectionOption = spellSelectionManager.getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return CastResult.NONE;
        }

        var spellData = selectionOption.spellData;
        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                offhandStack,
                spellLevel,
                player.level(),
                player,
                selectionOption.getCastSource(),
                true,
                SpellSelectionManager.OFFHAND
        );

        return casted ? CastResult.SUCCESS : CastResult.FAIL;
    }
    private enum CastResult {
        NONE,
        SUCCESS,
        FAIL
    }
}

