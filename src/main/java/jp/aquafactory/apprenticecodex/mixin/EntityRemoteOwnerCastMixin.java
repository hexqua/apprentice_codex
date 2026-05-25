package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRemoteOwnerCastMixin {
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastEyePosition(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        var context = apprentice_codex$remoteOwnerCastContext();
        if (context != null) {
            cir.setReturnValue(context.eyePosition());
        }
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastEyePosition(CallbackInfoReturnable<Vec3> cir) {
        var context = apprentice_codex$remoteOwnerCastContext();
        if (context != null) {
            cir.setReturnValue(context.eyePosition());
        }
    }

    @Inject(method = "getLookAngle()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastLookAngle(CallbackInfoReturnable<Vec3> cir) {
        var context = apprentice_codex$remoteOwnerCastContext();
        if (context != null) {
            cir.setReturnValue(context.forward());
        }
    }

    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastViewVector(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        var context = apprentice_codex$remoteOwnerCastContext();
        if (context != null) {
            cir.setReturnValue(context.forward());
        }
    }

    @Inject(method = "position()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastPosition(CallbackInfoReturnable<Vec3> cir) {
        var position = apprentice_codex$remoteOwnerCastFeetPosition();
        if (position != null) {
            cir.setReturnValue(position);
        }
    }

    @Inject(method = "getX()D", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastX(CallbackInfoReturnable<Double> cir) {
        var position = apprentice_codex$remoteOwnerCastFeetPosition();
        if (position != null) {
            cir.setReturnValue(position.x);
        }
    }

    @Inject(method = "getY()D", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastY(CallbackInfoReturnable<Double> cir) {
        var position = apprentice_codex$remoteOwnerCastFeetPosition();
        if (position != null) {
            cir.setReturnValue(position.y);
        }
    }

    @Inject(method = "getZ()D", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastZ(CallbackInfoReturnable<Double> cir) {
        var position = apprentice_codex$remoteOwnerCastFeetPosition();
        if (position != null) {
            cir.setReturnValue(position.z);
        }
    }

    @Inject(method = "getEyeY()D", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastEyeY(CallbackInfoReturnable<Double> cir) {
        var context = apprentice_codex$remoteOwnerCastContext();
        if (context != null) {
            cir.setReturnValue(context.eyePosition().y);
        }
    }

    @Inject(method = "blockPosition()Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
    private void apprentice_codex$remoteOwnerCastBlockPosition(CallbackInfoReturnable<BlockPos> cir) {
        var position = apprentice_codex$remoteOwnerCastFeetPosition();
        if (position != null) {
            cir.setReturnValue(BlockPos.containing(position));
        }
    }

    @Unique
    private Vec3 apprentice_codex$remoteOwnerCastFeetPosition() {
        var context = apprentice_codex$remoteOwnerCastContext();
        if (context == null) {
            return null;
        }
        var self = (Entity) (Object) this;
        return context.eyePosition().subtract(0.0D, self.getEyeHeight(), 0.0D);
    }

    @Unique
    private RemoteOwnerCastContext apprentice_codex$remoteOwnerCastContext() {
        // 1.20.1 では実座標を動かさず、spell が読む座標/視線系 API だけを差し替える。
        // 1.21.1 側では対象メソッド名や呼び出し経路が変わり得るため、Mixin 接着部として分離しておく。
        var self = (Entity) (Object) this;
        return self instanceof ServerPlayer player ? RemoteOwnerCastContext.get(player) : null;
    }
}
