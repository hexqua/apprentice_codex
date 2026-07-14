package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackNameMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void apprenticecodex$appendStorageStabilizerSpellName(CallbackInfoReturnable<Component> cir) {
        var stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof StorageStabilizer)) {
            return;
        }

        // 金床名は vanilla の独自名として保持し、選択魔法だけを表示時に合成して切替へ追従させる。
        cir.setReturnValue(StorageStabilizer.createDisplayName(stack, cir.getReturnValue()));
    }
}
