package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.stln.magitech.content.network.SpellCastPayLoadHandler;
import net.stln.magitech.content.network.SpellCastPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellCastPayLoadHandler.class, remap = false)
public abstract class MagitechStaffCastSyncMixin {
    @Inject(method = "handleDataOnMainC2S", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$rejectClientStaffCast(SpellCastPayload payload, IPayloadContext context, CallbackInfo ci) {
        // この杖の発動はvanillaのuseをserverで検証する。上流の任意wand付きC2Sを別入口にしない。
        if (payload.wand().filter(MagiEngineersStaffCompat::isStaff).isPresent()) ci.cancel();
    }

    @Inject(method = "handleDataOnMainS2C", at = @At("RETURN"))
    private static void apprenticecodex$rememberCast(SpellCastPayload payload, IPayloadContext context, CallbackInfo ci) {
        var entity = context.player().level().getEntity(payload.id());
        if (entity instanceof LivingEntity caster) MagiEngineersStaffCompat.rememberClientCast(caster, payload.spell());
    }
}
