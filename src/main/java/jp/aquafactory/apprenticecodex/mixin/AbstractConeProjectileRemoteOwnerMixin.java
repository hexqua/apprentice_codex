package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastAnchorEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = AbstractConeProjectile.class, remap = false)
public abstract class AbstractConeProjectileRemoteOwnerMixin {
    @Inject(method = "getSubEntityCollisions", at = @At("RETURN"))
    private void apprentice_codex$removeRemoteOwnerFromConeCollisions(CallbackInfoReturnable<Set<Entity>> cir) {
        var owner = ((Projectile) (Object) this).getOwner();
        if (owner instanceof RemoteOwnerCastAnchorEntity anchor) {
            cir.getReturnValue().removeIf(anchor::isBoundOwner);
        }
    }
}
