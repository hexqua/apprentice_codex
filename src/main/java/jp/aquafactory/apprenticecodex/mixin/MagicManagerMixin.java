package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerMixin {
    // FocusStaffbow の CONTINUOUS は duration cap を外すため、Iron's 標準 tick は継続中だけ横取りする。
    @Redirect(
            method = "lambda$tick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;isCasting()Z",
                    ordinal = 0
            )
    )
    private boolean apprentice_codex$skipFocusStaffbowContinuousInMagicManager(MagicData magicData) {
        return magicData.getSyncedData().isCasting() && !FocusStaffbowCastManager.shouldBypassMagicManager(magicData);
    }
}
