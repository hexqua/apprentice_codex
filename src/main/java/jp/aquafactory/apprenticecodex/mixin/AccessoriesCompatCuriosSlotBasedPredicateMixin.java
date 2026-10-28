package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.accessories.AccessoriesCompatWorkaroundSupport;
import jp.aquafactory.apprenticecodex.item.broom.BroomCurioSupport;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "io.wispforest.accessories_compat.curios.wrapper.CuriosSlotBasedPredicate",
        remap = false
)
public abstract class AccessoriesCompatCuriosSlotBasedPredicateMixin {
    @Shadow
    @Final
    private ResourceLocation location;

    @Dynamic("Accessories Compatibility LayerのCuriosSlotBasedPredicate#isValid")
    @Inject(
            method = "isValid(Lnet/minecraft/world/level/Level;Lio/wispforest/accessories/api/slot/SlotType;"
                    + "ILnet/minecraft/world/item/ItemStack;)Lnet/fabricmc/fabric/api/util/TriState;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void apprenticecodex$keepUnrelatedBackItemsValid(CallbackInfoReturnable<Object> cir) {
        if (!BroomCurioSupport.CALIBRATED_BROOM_PREDICATE.equals(location)) {
            return;
        }

        var result = cir.getReturnValue();
        if (!(result instanceof Enum<?> enumResult)) {
            AccessoriesCompatWorkaroundSupport.warnDisabled("Unexpected predicate result type");
            return;
        }
        if (!"FALSE".equals(enumResult.name())) {
            return;
        }

        // Curiosのfalseは「このvalidatorでは対象外」だが、Compat Layerでは明示的な拒否に変換される。
        // 外部型を参照せずDEFAULTへ戻し、未導入環境でこのmixinクラスを安全に読み飛ばせるようにする。
        for (var constant : enumResult.getDeclaringClass().getEnumConstants()) {
            if (constant instanceof Enum<?> enumConstant && "DEFAULT".equals(enumConstant.name())) {
                cir.setReturnValue(enumConstant);
                return;
            }
        }
        AccessoriesCompatWorkaroundSupport.warnDisabled("DEFAULT predicate result is unavailable");
    }
}
