package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshieldRuntime;
import jp.aquafactory.apprenticecodex.item.shield.ReflectcastShieldRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerMixin {
    // 独自 CONTINUOUS は構えや duration を独自管理するため、Iron's 標準 tick と二重実行させない。
    @Redirect(
            method = "lambda$tick$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;isCasting()Z",
                    ordinal = 0
            )
    )
    private boolean apprentice_codex$skipManagedContinuousInMagicManager(MagicData magicData) {
        return magicData.getSyncedData().isCasting()
                && !FocusStaffbowCastManager.shouldBypassMagicManager(magicData)
                && !BulwarkGreatshieldRuntime.shouldBypassMagicManager(magicData)
                && !ReflectcastShieldRuntime.shouldBypassMagicManager(magicData);
    }
}
