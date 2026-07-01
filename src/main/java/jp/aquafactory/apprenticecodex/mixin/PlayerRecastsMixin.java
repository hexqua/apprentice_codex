package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = PlayerRecasts.class, remap = false)
public abstract class PlayerRecastsMixin {
    @Shadow
    @Final
    private Map<String, RecastInstance> recastLookup;

    @Shadow
    @Final
    @Nullable
    private ServerPlayer serverPlayer;

    @Inject(method = "tick", at = @At("HEAD"))
    private void apprenticecodex$preserveFocusStaffbowRecastTicks(int actualTicks, CallbackInfo ci) {
        if (serverPlayer == null || actualTicks <= 0 || recastLookup.isEmpty()) {
            return;
        }
        if (serverPlayer.level().getGameTime() % actualTicks != 0) {
            return;
        }

        recastLookup.values().forEach(recastInstance -> {
            if (recastInstance.getRemainingRecasts() <= 0 || recastInstance.getTicksRemaining() <= 0) {
                return;
            }
            if (!FocusStaffbowCastManager.shouldPreserveRecastTicks(serverPlayer, recastInstance.getSpellId())) {
                return;
            }

            // Iron's 標準 tick の減算直前に同量を戻し、FocusStaffbow 詠唱中の同一 Recast だけ残り時間を固定する。
            var preservedTicks = Math.min(Integer.MAX_VALUE, (long) recastInstance.getTicksRemaining() + actualTicks);
            ((RecastInstanceAccessor) recastInstance).apprenticecodex$setRemainingTicks((int) preservedTicks);
        });
    }

}
