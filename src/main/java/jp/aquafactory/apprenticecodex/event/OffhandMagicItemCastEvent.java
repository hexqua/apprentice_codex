package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.item.CastingImplementData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.CastingItem;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OffhandMagicItemCastEvent {
    private OffhandMagicItemCastEvent() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        var player = event.getEntity();
        var offhandStack = player.getOffhandItem();
        if (!(offhandStack.getItem() instanceof AbstractOffhandMagicItem offhandMagicItem)) {
            return;
        }

        // 右クリック系の魔法アイテムがメインハンドにある場合は通常処理を優先する.
        if (isRightClickSpellItem(player.getMainHandItem())) {
            return;
        }

        if (!ISpellContainer.isSpellContainer(offhandStack)) {
            offhandMagicItem.initializeSpellContainer(offhandStack);
        }

        var spellData = offhandMagicItem.getConfiguredSpellData(offhandStack);
        if (spellData == SpellData.EMPTY) {
            return;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                offhandStack,
                spellLevel,
                player.level(),
                player,
                CastSource.SWORD,
                true,
                SpellSelectionManager.OFFHAND
        );

        event.setCancellationResult(casted ? InteractionResult.CONSUME : InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static boolean isRightClickSpellItem(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (stack.getItem() instanceof CastingItem || stack.getItem() instanceof Scroll) {
            return true;
        }

        return CastingImplementData.has(stack) && CastingImplementData.get(stack);
    }
}
