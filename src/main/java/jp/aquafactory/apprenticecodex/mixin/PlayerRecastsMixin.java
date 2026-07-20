package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
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
    private void apprenticecodex$preserveChargedCastRecastTicks(int actualTicks, CallbackInfo ci) {
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
            var castingSpell = SpellRegistry.getSpell(recastInstance.getSpellId());
            if (!FocusStaffbowCastManager.shouldPreserveRecastTicks(serverPlayer, recastInstance.getSpellId())
                    && !ChargecastCatalystbook.isManagedCast(serverPlayer, castingSpell)) {
                return;
            }

            // Iron's 標準 tick の減算直前に同量を戻し、独自の溜め詠唱中だけ同一 Recast の残り時間を固定する。
            var preservedTicks = Math.min(Integer.MAX_VALUE, (long) recastInstance.getTicksRemaining() + actualTicks);
            ((RecastInstanceAccessor) recastInstance).apprenticecodex$setRemainingTicks((int) preservedTicks);
        });
    }

    @Inject(
            method = "removeRecast(Lio/redspace/ironsspellbooks/capabilities/magic/RecastInstance;Lio/redspace/ironsspellbooks/capabilities/magic/RecastResult;Z)V",
            at = @At("RETURN")
    )
    private void apprenticecodex$clearMithrilFreecastCooldownSource(
            RecastInstance recastInstance,
            RecastResult recastResult,
            boolean doSync,
            CallbackInfo ci
    ) {
        if (serverPlayer == null) {
            return;
        }

        MithrilFreecastStaffCastContext.clearPendingCooldownSource(
                serverPlayer.getUUID(),
                SpellRegistry.getSpell(recastInstance.getSpellId())
        );
    }

}
