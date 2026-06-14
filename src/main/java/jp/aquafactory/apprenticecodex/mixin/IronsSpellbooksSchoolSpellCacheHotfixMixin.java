package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.network.SyncJsonConfigPacket;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexCommonConfig;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellConfigManager.class, remap = false)
public abstract class IronsSpellbooksSchoolSpellCacheHotfixMixin {
    @Inject(method = "handleClientSync", at = @At("RETURN"))
    private void apprentice_codex$clearSchoolSpellCacheAfterClientSync(SyncJsonConfigPacket packet, CallbackInfo ci) {
        apprentice_codex$clearSchoolSpellCacheIfEnabled();
    }

    @Inject(method = "onDatapackSync", at = @At("RETURN"))
    private static void apprentice_codex$clearSchoolSpellCacheAfterDatapackSync(OnDatapackSyncEvent event, CallbackInfo ci) {
        apprentice_codex$clearSchoolSpellCacheIfEnabled();
    }

    @Unique
    private static void apprentice_codex$clearSchoolSpellCacheIfEnabled() {
        if (!ApprenticeCodexCommonConfig.enableIronsSpellbooksSchoolSpellCacheHotfix()) {
            return;
        }
        SpellRegistry.onConfigReload();
    }
}
