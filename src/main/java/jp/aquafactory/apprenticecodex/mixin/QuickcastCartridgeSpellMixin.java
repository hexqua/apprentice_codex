package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCasting;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCharge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class QuickcastCartridgeSpellMixin {
    @Inject(method = "attemptInitiateCast", at = @At("RETURN"))
    private void clearConfirmationOnAcceptedCast(ItemStack stack, int spellLevel, Level level, Player player,
                                                CastSource source, boolean cooldown, String slot,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && player instanceof ServerPlayer serverPlayer) {
            QuickcastCartridgeCharge.clearConfirmation(serverPlayer);
        }
    }

    @Inject(method = "attemptInitiateCast", at = @At("HEAD"))
    private void checkPreviousCartridgeCast(ItemStack stack, int spellLevel, Level level, Player player,
                                            CastSource source, boolean cooldown, String slot,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) QuickcastCartridgeCasting.beforeNormalInitiation(serverPlayer);
    }

    @Inject(method = "onServerCastComplete", at = @At("RETURN"))
    private void clearCartridgeReservation(Level level, int spellLevel, LivingEntity entity, MagicData magic,
                                     boolean cancelled, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) QuickcastCartridgeCasting.validate(player);
    }
}
