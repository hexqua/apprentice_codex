package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.compat.malum.MalumTouchDigSpellweavingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.sammy.malum.common.item.curiosities.tools.spellweaver.SpellweavingPickaxeItem", remap = false)
public abstract class MalumSpellweavingPickaxeMixin {
    @ModifyExpressionValue(
            method = "triggerSpellweavingEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/common/item/curiosities/tools/spellweaver/SpellweavingPickaxeItem;matches(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;)Z"
            ),
            require = 0
    )
    private static boolean apprenticecodex$allowTouchDigInitialToolMismatch(boolean original) {
        // 伝播先の同種ブロック判定は変えず、Touch Dig 成功後に再実行する初回適性判定だけを緩和する。
        // ツルハシに限定しているのはNEARESTツールに絞りたいため.
        return original || MalumTouchDigSpellweavingContext.isInitialToolMatchBypassed();
    }
}
