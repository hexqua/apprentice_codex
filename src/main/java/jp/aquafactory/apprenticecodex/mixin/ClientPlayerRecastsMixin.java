package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerRecasts;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientCastIntent;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientCastState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = PlayerRecasts.class, remap = false)
public abstract class ClientPlayerRecastsMixin {
    @Shadow
    @Final
    private Map<String, RecastInstance> recastLookup;

    @Inject(method = "tickRecasts", at = @At("HEAD"))
    private void apprenticecodex$preserveChargedCastRecastTicks(CallbackInfo ci) {
        if (recastLookup.isEmpty()) {
            return;
        }

        var player = Minecraft.getInstance().player;
        recastLookup.values().forEach(recastInstance -> {
            if (recastInstance.getRemainingRecasts() <= 0 || recastInstance.getTicksRemaining() <= 0) {
                return;
            }
            var chargecastCasting = player != null && ClientMagicData.isCasting()
                    && ClientMagicData.getCastDuration() > 0
                    && recastInstance.getSpellId().equals(ClientMagicData.getCastingSpellId())
                    // 現在の手持ちは持ち替え直後に変わるため、cast-start で確定した active intent を基準にする。
                    && ChargecastCatalystbookClientCastIntent.matchesActive(
                            player.getUUID(), SpellRegistry.getSpell(recastInstance.getSpellId())
                    );
            if (!FocusStaffbowClientCastState.shouldPreserveClientRecastTicks(player, recastInstance.getSpellId())
                    && !chargecastCasting) {
                return;
            }

            // クライアント表示用 Recast は毎tick減るため、独自の溜め詠唱中だけ同一表示を1tick戻して固定する。
            var preservedTicks = Math.min(Integer.MAX_VALUE, recastInstance.getTicksRemaining() + 1L);
            ((RecastInstanceAccessor) recastInstance).apprenticecodex$setRemainingTicks((int) preservedTicks);
        });
    }
}
