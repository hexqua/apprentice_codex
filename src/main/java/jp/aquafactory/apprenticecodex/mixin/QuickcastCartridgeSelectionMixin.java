package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellSelectionManager.SelectionOption.class, remap = false)
public abstract class QuickcastCartridgeSelectionMixin {
    @Shadow public String slot;

    @Inject(method = "getCastSource", at = @At("HEAD"), cancellable = true)
    private void cartridgeSource(CallbackInfoReturnable<CastSource> cir) {
        if (QuickcastCartridgeCasting.SLOT.equals(slot)) cir.setReturnValue(CastSource.SPELLBOOK);
    }
}
