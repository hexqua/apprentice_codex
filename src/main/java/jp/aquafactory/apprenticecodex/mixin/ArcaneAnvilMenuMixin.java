package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.SpellSlotUpgradeableItem;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public abstract class ArcaneAnvilMenuMixin {
    // ArcaneAnvilMenu 自体は外部 MOD クラスなので remap しないが、
    // createResult は継承元 ItemCombinerMenu 由来で本番 jar では Minecraft 名へ難読化される.
    @Inject(method = "createResult()V", at = @At("RETURN"), remap = true, require = 1)
    private void apprenticecodex$blockUnsupportedSpellImbuement(CallbackInfo ci) {
        var itemCombinerMenu = (ItemCombinerMenuAccessor) (ItemCombinerMenu) (Object) this;
        var inputSlots = itemCombinerMenu.apprenticecodex$getInputSlots();
        var baseStack = inputSlots.getItem(0);
        var modifierStack = inputSlots.getItem(1);
        if (!SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(
                baseStack,
                modifierStack
        )) {
            apprentice_codex$normalizeRestrictedSpellImbueResult(itemCombinerMenu, baseStack, modifierStack);
            apprentice_codex$normalizeSpellSlotUpgradeResult(itemCombinerMenu, baseStack, modifierStack);
            return;
        }

        // ArcaneAnvil 既存UIのエラー表示を使うため、結果だけ空に戻す.
        itemCombinerMenu.apprenticecodex$getResultSlots().setItem(0, ItemStack.EMPTY);
    }

    @Unique
    private static void apprentice_codex$normalizeRestrictedSpellImbueResult(ItemCombinerMenuAccessor itemCombinerMenu, ItemStack baseStack, ItemStack modifierStack) {
        if (!(baseStack.getItem() instanceof RestrictedSpellImbuableItem spellImbueItem)
                || !(modifierStack.getItem() instanceof Scroll)) {
            return;
        }

        var scrollContainer = ISpellContainer.get(modifierStack);
        if (scrollContainer == null) {
            return;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        if (spellData == SpellData.EMPTY || !spellImbueItem.canImbueSpell(spellData)) {
            return;
        }

        var resultStack = itemCombinerMenu.apprenticecodex$getResultSlots().getItem(0);
        if (resultStack.isEmpty()) {
            return;
        }

        itemCombinerMenu.apprenticecodex$getResultSlots().setItem(0, spellImbueItem.createArcaneAnvilImbueResult(baseStack, spellData));
    }

    @Unique
    private static void apprentice_codex$normalizeSpellSlotUpgradeResult(ItemCombinerMenuAccessor itemCombinerMenu, ItemStack baseStack, ItemStack modifierStack) {
        if (!(baseStack.getItem() instanceof SpellSlotUpgradeableItem spellSlotUpgradeableItem)
                || !(modifierStack.getItem() instanceof SpellSlotUpgradeItem spellSlotUpgradeItem)) {
            return;
        }

        var resultStack = spellSlotUpgradeableItem.createSpellSlotUpgradeResult(baseStack, spellSlotUpgradeItem);
        if (resultStack.isEmpty()) {
            return;
        }

        itemCombinerMenu.apprenticecodex$getResultSlots().setItem(0, resultStack);
    }
}
