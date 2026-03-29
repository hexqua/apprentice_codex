package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SpectralWingState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySpectralWingMixin {
    @Shadow
    protected int fallFlyTicks;

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$keepSpectralWingFlight(CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        SpectralWingState state = spellData.get(CodexSpellStateTypeRegister.SPECTRAL_WING_STATE);
        if (!state.active || !state.startedBySpell) {
            return;
        }

        var sharedFlagAccessor = (EntitySharedFlagAccessor) player;
        boolean flag = sharedFlagAccessor.apprenticecodex$invokeGetSharedFlag(7);
        // 1.20.1 の Elytra 維持判定は chest slot の canElytraFly を毎 tick 要求する。
        // SpectralWing 中だけは spell state を優先し、通常 Elytra の条件分岐を巻き込まない。
        if (flag && !player.onGround() && !player.isPassenger() && !player.hasEffect(MobEffects.LEVITATION)) {
            flag = true;
        } else {
            flag = false;
        }

        if (!player.level().isClientSide) {
            sharedFlagAccessor.apprenticecodex$invokeSetSharedFlag(7, flag);
        }
        ci.cancel();
    }
}
