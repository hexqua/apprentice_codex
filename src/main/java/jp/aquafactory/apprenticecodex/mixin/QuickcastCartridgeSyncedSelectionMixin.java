package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCharge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SyncedSpellData.class, remap = false)
public abstract class QuickcastCartridgeSyncedSelectionMixin {
    @Shadow private LivingEntity livingEntity;

    @Inject(method = "setSpellSelection", at = @At("HEAD"))
    private void clearCartridgeConfirmation(SpellSelection selection, CallbackInfo ci) {
        // tick間の比較だけでは「別選択へ移動して元へ戻す」入力を見落とす。
        if (livingEntity instanceof ServerPlayer player) QuickcastCartridgeCharge.selectionChanged(player);
    }
}
