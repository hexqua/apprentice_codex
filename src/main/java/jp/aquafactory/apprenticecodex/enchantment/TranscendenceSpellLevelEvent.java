package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class TranscendenceSpellLevelEvent {
    private TranscendenceSpellLevelEvent() {
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        var caster = event.getEntity();
        if (caster == null) {
            return;
        }

        var addedLevels = getApplicableTranscendenceLevels(caster.getMainHandItem(), event.getSpell(), false)
                + getApplicableTranscendenceLevels(caster.getOffhandItem(), event.getSpell(), true);
        if (addedLevels <= 0) {
            return;
        }

        event.addLevels(addedLevels);
    }

    private static int getApplicableTranscendenceLevels(ItemStack stack, AbstractSpell spell, boolean isOffhandSlot) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var item = stack.getItem();
        // 魔法補助具は従来通りオフハンド限定だが、spell gun は両手持ちでもレベル加算対象にする。
        var isSupportedSlot =
                (isOffhandSlot && item instanceof AbstractOffhandMagicItem)
                        || item instanceof AbstractSpellGunItem;
        if (!isSupportedSlot) {
            return 0;
        }

        var transcendenceLevel = Enchantments.getLevel(stack, Enchantments.TRANSCENDENCE);
        if (transcendenceLevel <= 0 || !ISpellContainer.isSpellContainer(stack)) {
            return 0;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return 0;
        }

        var imbuedSpell = spellContainer.getSpellAtIndex(0);
        if (imbuedSpell == SpellData.EMPTY || !imbuedSpell.getSpell().equals(spell)) {
            return 0;
        }

        return transcendenceLevel;
    }
}
