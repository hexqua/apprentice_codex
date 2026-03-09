package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import jp.aquafactory.apprenticecodex.utility.SpellGunSpellValidator;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public abstract class ArcaneAnvilMenuMixin {
    @Inject(method = "createResult", at = @At("RETURN"), remap = false)
    private void apprenticecodex$blockUnsupportedSpellImbuement(CallbackInfo ci) {
        var itemCombinerMenu = (ItemCombinerMenuAccessor) (ItemCombinerMenu) (Object) this;
        if (!SpellGunSpellValidator.isUnsupportedArcaneAnvilSpell(
                itemCombinerMenu.apprenticecodex$getInputSlots().getItem(0),
                itemCombinerMenu.apprenticecodex$getInputSlots().getItem(1)
        )) {
            return;
        }

        // ArcaneAnvil 既存UIのエラー表示を使うため、結果だけ空に戻す.
        itemCombinerMenu.apprenticecodex$getResultSlots().setItem(0, ItemStack.EMPTY);
    }
}
